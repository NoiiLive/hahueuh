package net.noiilive.hahueuh;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerAllies {
    private static final String FILE_NAME = "hahueuh_player_allies.json";
    private static final int REQUEST_EXPIRY_TICKS = 30 * 20;
    private static final int SWEEP_INTERVAL_TICKS = 20;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type STORE_TYPE = new TypeToken<StoredData>() {}.getType();

    private final Map<UUID, Set<UUID>> allies = new ConcurrentHashMap<>();
    private final Map<UUID, String> nameCache = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, PendingRequest>> pendingRequests = new ConcurrentHashMap<>();
    private MinecraftServer server;
    private Path filePath;

    private static final class PendingRequest {
        final String requesterName;
        final int expiryTick;

        PendingRequest(String requesterName, int expiryTick) {
            this.requesterName = requesterName;
            this.expiryTick = expiryTick;
        }
    }

    private static final class StoredData {
        Map<String, List<String>> allies = new HashMap<>();
        Map<String, String> names = new HashMap<>();
    }

    public boolean areAllies(UUID a, UUID b) {
        Set<UUID> set = allies.get(a);
        return set != null && set.contains(b);
    }

    public Set<UUID> getAllies(UUID player) {
        return allies.getOrDefault(player, Set.of());
    }

    public void requestAlly(ServerPlayer requester, GameProfile targetProfile) {
        if (server == null) return;
        UUID requesterUuid = requester.getUUID();
        UUID targetUuid = targetProfile.getId();
        cacheName(requesterUuid, requester.getName().getString());
        cacheName(targetUuid, targetProfile.getName());

        if (requesterUuid.equals(targetUuid)) {
            requester.displayClientMessage(Component.translatable("hahueuh.message.ally_cannot_self")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        if (areAllies(requesterUuid, targetUuid)) {
            requester.displayClientMessage(Component.translatable("hahueuh.message.ally_already",
                    targetProfile.getName()).withStyle(ChatFormatting.RED), true);
            return;
        }

        if (consumePending(requesterUuid, targetUuid)) {
            finishAccept(requester, targetUuid, targetProfile.getName());
            return;
        }

        if (hasPendingRequest(targetUuid, requesterUuid)) {
            requester.displayClientMessage(Component.translatable("hahueuh.message.ally_request_pending",
                    targetProfile.getName()).withStyle(ChatFormatting.RED), true);
            return;
        }

        pendingRequests.computeIfAbsent(targetUuid, k -> new ConcurrentHashMap<>())
                .put(requesterUuid, new PendingRequest(requester.getName().getString(), server.getTickCount() + REQUEST_EXPIRY_TICKS));

        requester.displayClientMessage(Component.translatable("hahueuh.message.ally_request_sent",
                targetProfile.getName()).withStyle(ChatFormatting.GOLD), true);

        ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);
        if (target != null) sendRequestPrompt(target, requesterUuid, requester.getName().getString());
    }

    private void sendRequestPrompt(ServerPlayer target, UUID requesterUuid, String requesterName) {
        Component accept = Component.translatable("hahueuh.message.ally_accept_button")
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/rezero ally accept " + requesterName))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("hahueuh.message.ally_accept_button"))));
        Component decline = Component.translatable("hahueuh.message.ally_decline_button")
                .withStyle(style -> style
                        .withColor(ChatFormatting.RED)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/rezero ally decline " + requesterName))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("hahueuh.message.ally_decline_button"))));

        Component prompt = Component.translatable("hahueuh.message.ally_request_prompt", requesterName)
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" "))
                .append(accept)
                .append(Component.literal(" "))
                .append(decline);
        target.sendSystemMessage(prompt);
    }

    public void acceptRequest(ServerPlayer target, GameProfile requesterProfile) {
        if (server == null) return;
        UUID targetUuid = target.getUUID();
        UUID requesterUuid = requesterProfile.getId();
        cacheName(requesterUuid, requesterProfile.getName());

        if (!consumePending(targetUuid, requesterUuid)) {
            target.displayClientMessage(Component.translatable("hahueuh.message.ally_no_request")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        finishAccept(target, requesterUuid, requesterProfile.getName());
    }

    private void finishAccept(ServerPlayer target, UUID requesterUuid, String requesterName) {
        UUID targetUuid = target.getUUID();
        allies.computeIfAbsent(targetUuid, k -> ConcurrentHashMap.newKeySet()).add(requesterUuid);
        allies.computeIfAbsent(requesterUuid, k -> ConcurrentHashMap.newKeySet()).add(targetUuid);
        save();

        target.displayClientMessage(Component.translatable("hahueuh.message.ally_you_accepted", requesterName)
                .withStyle(ChatFormatting.GREEN), false);
        ServerPlayer requester = server.getPlayerList().getPlayer(requesterUuid);
        if (requester != null) {
            requester.displayClientMessage(Component.translatable("hahueuh.message.ally_request_accepted",
                    target.getName()).withStyle(ChatFormatting.GREEN), false);
        }
    }

    public void declineRequest(ServerPlayer target, GameProfile requesterProfile) {
        if (server == null) return;
        UUID targetUuid = target.getUUID();
        UUID requesterUuid = requesterProfile.getId();

        if (!consumePending(targetUuid, requesterUuid)) {
            target.displayClientMessage(Component.translatable("hahueuh.message.ally_no_request")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        ServerPlayer requester = server.getPlayerList().getPlayer(requesterUuid);
        if (requester != null) {
            requester.displayClientMessage(Component.translatable("hahueuh.message.ally_request_declined",
                    target.getName()).withStyle(ChatFormatting.RED), false);
        }
    }

    public void removeAlly(ServerPlayer player, GameProfile targetProfile) {
        UUID playerUuid = player.getUUID();
        UUID targetUuid = targetProfile.getId();

        Set<UUID> mine = allies.get(playerUuid);
        if (mine == null || !mine.remove(targetUuid)) {
            player.displayClientMessage(Component.translatable("hahueuh.message.ally_remove_not_allied",
                    targetProfile.getName()).withStyle(ChatFormatting.RED), true);
            return;
        }
        Set<UUID> theirs = allies.get(targetUuid);
        if (theirs != null) theirs.remove(playerUuid);
        save();

        player.displayClientMessage(Component.translatable("hahueuh.message.ally_removed",
                targetProfile.getName()).withStyle(ChatFormatting.GOLD), true);
        ServerPlayer target = server == null ? null : server.getPlayerList().getPlayer(targetUuid);
        if (target != null) {
            target.displayClientMessage(Component.translatable("hahueuh.message.ally_removed_by",
                    player.getName()).withStyle(ChatFormatting.RED), false);
        }
    }

    private boolean hasPendingRequest(UUID target, UUID requester) {
        Map<UUID, PendingRequest> byRequester = pendingRequests.get(target);
        if (byRequester == null) return false;
        PendingRequest pending = byRequester.get(requester);
        return pending != null && server != null && pending.expiryTick > server.getTickCount();
    }

    private boolean consumePending(UUID target, UUID requester) {
        Map<UUID, PendingRequest> byRequester = pendingRequests.get(target);
        if (byRequester == null) return false;
        PendingRequest pending = byRequester.remove(requester);
        if (byRequester.isEmpty()) pendingRequests.remove(target);
        return pending != null && server != null && pending.expiryTick > server.getTickCount();
    }

    private void cacheName(UUID uuid, String name) {
        if (name != null && !name.isEmpty()) nameCache.put(uuid, name);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer target)) return;
        cacheName(target.getUUID(), target.getName().getString());
        Map<UUID, PendingRequest> byRequester = pendingRequests.get(target.getUUID());
        if (byRequester == null || byRequester.isEmpty()) return;
        int now = server != null ? server.getTickCount() : 0;
        for (Map.Entry<UUID, PendingRequest> entry : byRequester.entrySet()) {
            if (entry.getValue().expiryTick > now) {
                sendRequestPrompt(target, entry.getKey(), entry.getValue().requesterName);
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (server == null || pendingRequests.isEmpty()) return;
        if (server.getTickCount() % SWEEP_INTERVAL_TICKS != 0) return;
        int now = server.getTickCount();
        pendingRequests.values().forEach(byRequester -> byRequester.values().removeIf(p -> p.expiryTick <= now));
        pendingRequests.values().removeIf(Map::isEmpty);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.server = event.getServer();
        this.filePath = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        load();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        save();
        allies.clear();
        nameCache.clear();
        pendingRequests.clear();
        this.server = null;
        this.filePath = null;
    }

    private void load() {
        allies.clear();
        nameCache.clear();
        if (filePath == null || !Files.exists(filePath)) return;
        try {
            StoredData raw = GSON.fromJson(Files.readString(filePath, StandardCharsets.UTF_8), STORE_TYPE);
            if (raw == null) return;
            if (raw.allies != null) {
                raw.allies.forEach((uuidStr, list) -> {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        Set<UUID> set = ConcurrentHashMap.newKeySet();
                        for (String s : list) {
                            try {
                                set.add(UUID.fromString(s));
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                        allies.put(uuid, set);
                    } catch (IllegalArgumentException e) {
                        HahUeuh.LOGGER.warn("Ignoring malformed Player Allies UUID '{}'", uuidStr);
                    }
                });
            }
            if (raw.names != null) {
                raw.names.forEach((uuidStr, name) -> {
                    try {
                        nameCache.put(UUID.fromString(uuidStr), name);
                    } catch (IllegalArgumentException ignored) {
                    }
                });
            }
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to load Player Allies data from {}", filePath, e);
        }
    }

    private void save() {
        if (filePath == null) return;
        try {
            StoredData raw = new StoredData();
            allies.forEach((uuid, set) -> {
                List<String> list = new ArrayList<>();
                for (UUID u : set) list.add(u.toString());
                raw.allies.put(uuid.toString(), list);
            });
            nameCache.forEach((uuid, name) -> raw.names.put(uuid.toString(), name));
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, GSON.toJson(raw, STORE_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to save Player Allies data to {}", filePath, e);
        }
    }
}
