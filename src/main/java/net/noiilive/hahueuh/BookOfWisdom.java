package net.noiilive.hahueuh;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.noiilive.hahueuh.network.AbilityCooldownPayload;
import net.noiilive.hahueuh.network.BoundVisionAbility;
import net.noiilive.hahueuh.network.GreedVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BookOfWisdom {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PERSIST_FILE_NAME = "hahueuh_book_of_wisdom.json";
    private static final Type PERSIST_TYPE = new TypeToken<Set<String>>() {}.getType();

    private static final int SWEEP_INTERVAL_TICKS = 10;
    private static final int VOID_RESCUE_MARGIN = 24;

    private final Set<UUID> summoned = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> cooldownUntilTick = new ConcurrentHashMap<>();
    private MinecraftServer server;
    private Path persistFilePath;

    public boolean isSummoned(UUID uuid) {
        return summoned.contains(uuid);
    }

    public void toggle(ServerPlayer player) {
        if (server == null) return;
        UUID uuid = player.getUUID();

        if (!HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().canUseGreed(uuid)
                || HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().getGreedVariant(uuid) != GreedVariant.ECHIDNA) {
            player.displayClientMessage(Component.translatable("hahueuh.message.no_greed_authority")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        int remainingCooldown = player.isCreative() ? 0 : cooldownRemainingTicks(uuid);
        if (remainingCooldown > 0) {
            int seconds = (int) Math.ceil(remainingCooldown / 20.0);
            player.displayClientMessage(Component.translatable("hahueuh.message.book_of_wisdom_cooldown", seconds)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (summoned.contains(uuid)) {
            boolean removed = removeFromOwnInventory(player);
            if (!removed && !player.isCreative()) {
                player.displayClientMessage(Component.translatable("hahueuh.message.book_of_wisdom_out_of_reach")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
            summoned.remove(uuid);
            savePersisted();
            startCooldown(player);
            player.displayClientMessage(Component.translatable("hahueuh.message.book_of_wisdom_unsummoned")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        } else {
            giveBook(player);
            summoned.add(uuid);
            savePersisted();
            startCooldown(player);
            player.displayClientMessage(Component.translatable("hahueuh.message.book_of_wisdom_summoned")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    private void giveBook(ServerPlayer player) {
        ItemStack book = new ItemStack(ModItems.MEMORIES_OF_THE_WORLD.get());
        book.set(ModDataComponents.BOOK_OWNER.get(), player.getUUID());
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
    }

    private static boolean isOwnedBook(ItemStack stack, UUID owner) {
        return stack.is(ModItems.MEMORIES_OF_THE_WORLD.get()) && owner.equals(stack.get(ModDataComponents.BOOK_OWNER.get()));
    }

    public boolean isHoldingOwnBook(ServerPlayer player) {
        UUID uuid = player.getUUID();
        return isOwnedBook(player.getMainHandItem(), uuid) || isOwnedBook(player.getOffhandItem(), uuid);
    }

    public boolean isHoldingCredentialFor(ServerPlayer player, BoundVisionAbility ability) {
        return isHoldingOwnBook(player) || HahUeuh.BOOK_OF_WISDOM_COPY.isHoldingBoundCopy(player, ability);
    }

    private boolean removeFromOwnInventory(ServerPlayer player) {
        UUID uuid = player.getUUID();
        var items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            if (isOwnedBook(items.get(i), uuid)) {
                items.set(i, ItemStack.EMPTY);
                return true;
            }
        }
        var offhand = player.getInventory().offhand;
        for (int i = 0; i < offhand.size(); i++) {
            if (isOwnedBook(offhand.get(i), uuid)) {
                offhand.set(i, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (server == null || summoned.isEmpty()) return;
        if (server.getTickCount() % SWEEP_INTERVAL_TICKS != 0) return;

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity e : level.getEntities().getAll()) {
                if (e instanceof ItemEntity ie) {
                    ItemStack stack = ie.getItem();
                    if (!stack.is(ModItems.MEMORIES_OF_THE_WORLD.get())) continue;

                    ie.setUnlimitedLifetime();

                    if (ie.getY() < level.getMinBuildHeight() + VOID_RESCUE_MARGIN) {
                        UUID owner = stack.get(ModDataComponents.BOOK_OWNER.get());
                        rescueFallingBook(ie, owner);
                    }
                } else if (e instanceof LivingEntity living) {
                    checkHeldBookCurse(living);
                }
            }
        }
    }

    private void checkHeldBookCurse(LivingEntity living) {
        boolean holdingBook = living.getMainHandItem().is(ModItems.MEMORIES_OF_THE_WORLD.get())
                || living.getOffhandItem().is(ModItems.MEMORIES_OF_THE_WORLD.get());
        if (!holdingBook || MentalOverload.isExemptEchidna(living)) return;
        MentalOverload.applyInsanity(living);
    }

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        event.getAffectedEntities().removeIf(e ->
                e instanceof ItemEntity ie && ie.getItem().is(ModItems.MEMORIES_OF_THE_WORLD.get()));
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (server == null || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (!removeAllOwnedFromInventory(player)) return;
        if (summoned.remove(player.getUUID())) {
            savePersisted();
        }
    }

    private boolean removeAllOwnedFromInventory(ServerPlayer player) {
        UUID uuid = player.getUUID();
        boolean removed = false;
        var items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            if (isOwnedBook(items.get(i), uuid)) {
                items.set(i, ItemStack.EMPTY);
                removed = true;
            }
        }
        var offhand = player.getInventory().offhand;
        for (int i = 0; i < offhand.size(); i++) {
            if (isOwnedBook(offhand.get(i), uuid)) {
                offhand.set(i, ItemStack.EMPTY);
                removed = true;
            }
        }
        return removed;
    }

    private void rescueFallingBook(ItemEntity ie, UUID owner) {
        ServerPlayer player = owner == null ? null : server.getPlayerList().getPlayer(owner);
        if (player != null) {
            ItemStack stack = ie.getItem();
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            ie.discard();
        } else {
            ie.teleportTo(ie.getX(), ie.level().getMinBuildHeight() + VOID_RESCUE_MARGIN, ie.getZ());
            ie.setDeltaMovement(ie.getDeltaMovement().x, 0.0, ie.getDeltaMovement().z);
        }
    }

    private void startCooldown(ServerPlayer player) {
        if (server == null || player.isCreative()) return;
        int cooldownSeconds = ConfigGreed.BOOK_OF_WISDOM_COOLDOWN_SECONDS.getAsInt();
        if (cooldownSeconds <= 0) return;
        cooldownUntilTick.put(player.getUUID(), server.getTickCount() + HahUeuh.GREED_COMPAT.scaleCooldownTicks(player.getUUID(), cooldownSeconds * 20));
        PacketDistributor.sendToPlayer(player,
                new AbilityCooldownPayload(HahUeuhAbilities.BOOK_OF_WISDOM_ABILITY, HahUeuh.GREED_COMPAT.scaleCooldownTicks(player.getUUID(), cooldownSeconds * 20)));
    }

    private int cooldownRemainingTicks(UUID uuid) {
        Integer until = cooldownUntilTick.get(uuid);
        if (until == null || server == null) return 0;
        return Math.max(0, until - server.getTickCount());
    }

    public Map<UUID, Integer> captureCooldownRemaining() {
        Map<UUID, Integer> result = new HashMap<>();
        if (server == null) return result;
        int tick = server.getTickCount();
        cooldownUntilTick.forEach((uuid, until) -> {
            int remaining = until - tick;
            if (remaining > 0) result.put(uuid, remaining);
        });
        return result;
    }

    public void restoreCooldownRemaining(Map<UUID, Integer> remainingByUuid) {
        if (server == null) return;
        cooldownUntilTick.clear();
        int tick = server.getTickCount();
        remainingByUuid.forEach((uuid, remaining) -> cooldownUntilTick.put(uuid, tick + remaining));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int remaining = remainingByUuid.getOrDefault(player.getUUID(), 0);
            PacketDistributor.sendToPlayer(player, new AbilityCooldownPayload(HahUeuhAbilities.BOOK_OF_WISDOM_ABILITY, remaining));
        }
    }

    public Set<UUID> captureSummonedState() {
        return new HashSet<>(summoned);
    }

    public void restoreSummonedState(Set<UUID> restored) {
        summoned.clear();
        summoned.addAll(restored);
        savePersisted();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.server = event.getServer();
        this.persistFilePath = server.getWorldPath(LevelResource.ROOT).resolve(PERSIST_FILE_NAME);
        loadPersisted();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        savePersisted();
        summoned.clear();
        cooldownUntilTick.clear();
        this.server = null;
    }

    public void reloadPersisted() {
        loadPersisted();
    }

    private void loadPersisted() {
        summoned.clear();
        if (persistFilePath == null || !Files.exists(persistFilePath)) return;
        try {
            Set<String> raw = GSON.fromJson(Files.readString(persistFilePath, StandardCharsets.UTF_8), PERSIST_TYPE);
            if (raw == null) return;
            for (String key : raw) {
                try {
                    summoned.add(UUID.fromString(key));
                } catch (IllegalArgumentException e) {
                    HahUeuh.LOGGER.warn("Ignoring malformed Book of Wisdom persisted UUID '{}'", key);
                }
            }
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to load persisted Book of Wisdom state from {}", persistFilePath, e);
        }
    }

    private void savePersisted() {
        if (persistFilePath == null) return;
        try {
            Set<String> raw = new HashSet<>();
            summoned.forEach(uuid -> raw.add(uuid.toString()));
            Files.createDirectories(persistFilePath.getParent());
            Files.writeString(persistFilePath, GSON.toJson(raw, PERSIST_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to save persisted Book of Wisdom state to {}", persistFilePath, e);
        }
    }
}
