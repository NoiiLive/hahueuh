package net.noiilive.hahueuh;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.noiilive.hahueuh.network.GreedVariant;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GreedCompatibility {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static final String FILE_NAME = "hahueuh_greed_compat.json";

    private static final class PersistedState {
        Map<String, Integer> score = new HashMap<>();
        Set<String> awardedContainers = new java.util.HashSet<>();
    }

    private static final Type PERSISTED_TYPE = new TypeToken<PersistedState>() {}.getType();

    private static final java.util.Random RANDOM = new java.util.Random();

    private final Map<UUID, Integer> score = new ConcurrentHashMap<>();
    private final Set<String> awardedContainers = ConcurrentHashMap.newKeySet();
    private MinecraftServer server;
    private Path filePath;

    private static boolean enabled() { return ConfigMain.COMPATIBILITY_ENABLED.get(); }
    private static int threshold() { return ConfigGreed.GREED_COMPAT_THRESHOLD.getAsInt(); }

    public boolean isCompatible(UUID uuid) {
        return !enabled() || score.getOrDefault(uuid, 0) >= threshold();
    }

    public int scaleCooldownTicks(UUID uuid, int baseTicks) {
        if (!enabled()) return baseTicks;
        int t = threshold();
        if (t <= 0) return baseTicks;
        double pct = Math.min(1.0, (double) score.getOrDefault(uuid, 0) / t);
        return (int) Math.round(baseTicks * (2.0 - pct));
    }

    public int getScore(UUID uuid) {
        return score.getOrDefault(uuid, 0);
    }

    public void setScore(UUID uuid, int value) {
        score.put(uuid, Math.max(0, value));
        save();
    }

    public void ensureStartingScore(UUID uuid) {
        if (score.containsKey(uuid)) return;
        int min = ConfigMain.STARTING_COMPATIBILITY_MIN.get();
        int max = ConfigMain.STARTING_COMPATIBILITY_MAX.get();
        int lo = Math.min(min, max), hi = Math.max(min, max);
        int roll = lo + RANDOM.nextInt(hi - lo + 1);
        score.put(uuid, roll);
        save();
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ensureStartingScore(player.getUUID());
        }
    }

    private void addScore(ServerPlayer player, int points, String reason) {
        if (!enabled() || points <= 0) return;
        UUID id = player.getUUID();
        int old = score.getOrDefault(id, 0);
        if (old >= threshold()) return;
        int now = Math.min(threshold(), old + points);
        score.put(id, now);
        save();
        player.displayClientMessage(Component.translatable("hahueuh.message.greed_attunement",
                now - old, now, Component.translatable(reason)).withStyle(ChatFormatting.DARK_RED), true);
        if (now >= threshold()) {
            player.sendSystemMessage(Component.translatable("hahueuh.message.greed_compatible")
                    .withStyle(ChatFormatting.RED));
        }
    }

    public void applyDrawbacks(ServerPlayer player) {
        if (!enabled() || server == null) return;
        int threshold = threshold();
        int current = score.getOrDefault(player.getUUID(), 0);
        if (threshold <= 0 || current >= threshold) return;
        double pct = (double) current / threshold;

        boolean blindness, nausea, hunger;
        int damageIntervalTicks;

        if (pct < 0.25) {
            blindness = true;  nausea = true;  hunger = true; damageIntervalTicks = 20;
        } else if (pct < 0.50) {
            blindness = false; nausea = true;  hunger = true; damageIntervalTicks = 20;
        } else if (pct < 0.75) {
            blindness = false; nausea = false; hunger = true; damageIntervalTicks = 60;
        } else if (pct < 0.90) {
            blindness = false; nausea = false; hunger = true; damageIntervalTicks = 100;
        } else {
            blindness = false; nausea = false; hunger = true; damageIntervalTicks = 0;
        }

        if (blindness) player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, true));
        if (hunger)    player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 1, false, false, true));
        if (nausea)    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, false, true));
        if (damageIntervalTicks > 0 && server.getTickCount() % damageIntervalTicks == 0) {
            player.hurt(player.damageSources().magic(), 1.0f);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!enabled() || server == null) return;
        var authority = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            UUID uuid = player.getUUID();
            if (authority.canUseGreed(uuid) && !isCompatible(uuid)
                    && isChannelingGreed(uuid, authority.getGreedVariant(uuid))) {
                applyDrawbacks(player);
            }
        }
    }

    private static boolean isChannelingGreed(UUID uuid, GreedVariant variant) {
        return switch (variant) {
            case LIONSHEART -> HahUeuh.LIONS_HEART.isActive(uuid);
            case CORLEONIS -> HahUeuh.BASE_SHIFT.isActive(uuid) || HahUeuh.SECOND_SHIFT.isActive(uuid);
            case ECHIDNA -> HahUeuh.BOOK_OF_WISDOM.isSummoned(uuid);
        };
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof ChestBlockEntity chest
                && chest.getLootTable() != null) {
            awardChestLoot(player, "block|" + event.getLevel().dimension().location() + "|" + event.getPos().asLong());
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getTarget() instanceof MinecartChest cart && cart.getLootTable() != null) {
            awardChestLoot(player, "cart|" + cart.getUUID());
        }
    }

    private void awardChestLoot(ServerPlayer player, String containerKey) {
        String key = player.getUUID() + "|" + containerKey;
        if (!awardedContainers.add(key)) return;
        addScore(player, ConfigGreed.GREED_POINTS_CHEST_LOOT.getAsInt(), "hahueuh.reason.chest_loot");
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        var state = event.getState();
        if (state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.EMERALD_ORES) || state.is(Blocks.ANCIENT_DEBRIS)) {
            addScore(player, ConfigGreed.GREED_POINTS_ORE_MINE.getAsInt(), "hahueuh.reason.ore_mining");
        }
    }

    @SubscribeEvent
    public void onVillagerTrade(TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MerchantOffer offer = event.getMerchantOffer();
        if (offer.getResult().is(Items.EMERALD)) {
            addScore(player, ConfigGreed.GREED_POINTS_VILLAGER_TRADE.getAsInt(), "hahueuh.reason.villager_trade");
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.server = event.getServer();
        this.filePath = event.getServer().getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        reload();
    }

    public void reload() {
        if (filePath == null) return;
        score.clear();
        awardedContainers.clear();
        if (Files.exists(filePath)) {
            try {
                PersistedState state = GSON.fromJson(Files.readString(filePath, StandardCharsets.UTF_8), PERSISTED_TYPE);
                if (state != null) {
                    if (state.score != null) state.score.forEach((k, v) -> {
                        try { score.put(UUID.fromString(k), v); } catch (IllegalArgumentException ignored) {}
                    });
                    if (state.awardedContainers != null) awardedContainers.addAll(state.awardedContainers);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load Greed compatibility data", e);
            }
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        save();
        filePath = null;
        server = null;
    }

    private void save() {
        if (filePath == null) return;
        try {
            PersistedState state = new PersistedState();
            score.forEach((uuid, v) -> state.score.put(uuid.toString(), v));
            state.awardedContainers.addAll(awardedContainers);
            Files.writeString(filePath, GSON.toJson(state, PERSISTED_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to save Greed compatibility data", e);
        }
    }
}
