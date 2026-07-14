package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.AbilityCooldownPayload;
import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.network.MaterialPhaseStatePayload;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MaterialPhase {
    private static final double CORE_INSET = 0.5;

    private static final ResourceLocation DAMAGE_BUFF_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "material_phase_damage");
    private static final double DAMAGE_BUFF_AMOUNT = 19.0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PERSIST_FILE_NAME = "hahueuh_material_phase.json";
    private static final Type PERSIST_TYPE = new TypeToken<Set<String>>() {}.getType();

    private final Set<UUID> active = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> cooldownUntilTick = new ConcurrentHashMap<>();
    private final Set<UUID> persistedActive = ConcurrentHashMap.newKeySet();
    private MinecraftServer server;
    private Path persistFilePath;

    public boolean isActive(UUID uuid) {
        return active.contains(uuid);
    }

    public void toggle(ServerPlayer player) {
        if (server == null) return;
        UUID uuid = player.getUUID();

        if (active.contains(uuid)) {
            deactivate(player, true);
            return;
        }

        if (!HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().canUseGreed(uuid)
                || HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().getGreedVariant(uuid) != GreedVariant.LIONSHEART) {
            player.displayClientMessage(Component.translatable("hahueuh.message.no_greed_authority")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (!HahUeuh.LIONS_HEART.isActive(uuid)) {
            player.displayClientMessage(Component.translatable("hahueuh.message.material_phase_needs_lions_heart")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        int remainingCooldown = player.isCreative() ? 0 : cooldownRemainingTicks(uuid);
        if (remainingCooldown > 0) {
            int seconds = (int) Math.ceil(remainingCooldown / 20.0);
            player.displayClientMessage(Component.translatable("hahueuh.message.material_phase_cooldown", seconds)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        active.add(uuid);
        applyDamageBuff(player);
        PacketDistributor.sendToPlayer(player, new MaterialPhaseStatePayload(true));
        player.displayClientMessage(Component.translatable("hahueuh.message.material_phase_activated")
                .withStyle(ChatFormatting.GOLD), true);
    }

    private void deactivate(ServerPlayer player, boolean startCooldown) {
        UUID uuid = player.getUUID();
        if (!active.remove(uuid)) return;
        removeDamageBuff(player);
        PacketDistributor.sendToPlayer(player, new MaterialPhaseStatePayload(false));
        if (startCooldown && !player.isCreative()) {
            int cooldownSeconds = ConfigGreed.MATERIAL_PHASE_COOLDOWN_SECONDS.getAsInt();
            if (cooldownSeconds > 0) {
                cooldownUntilTick.put(uuid, server.getTickCount() + HahUeuh.GREED_COMPAT.scaleCooldownTicks(uuid, cooldownSeconds * 20));
                PacketDistributor.sendToPlayer(player,
                        new AbilityCooldownPayload(HahUeuhAbilities.MATERIAL_PHASE_ABILITY, HahUeuh.GREED_COMPAT.scaleCooldownTicks(uuid, cooldownSeconds * 20)));
            }
        }
        player.displayClientMessage(Component.translatable("hahueuh.message.material_phase_deactivated")
                .withStyle(ChatFormatting.GOLD), true);
    }

    public void forceResetOnRollback(ServerPlayer player) {
        if (active.remove(player.getUUID())) {
            removeDamageBuff(player);
            PacketDistributor.sendToPlayer(player, new MaterialPhaseStatePayload(false));
        }
    }

    public Set<UUID> captureActive() {
        return new HashSet<>(active);
    }

    public void restoreActiveOnRollback(Set<UUID> activeAtSnapshot) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            boolean want = activeAtSnapshot.contains(uuid);
            if (want == active.contains(uuid)) continue;
            if (want) {
                active.add(uuid);
                applyDamageBuff(player);
                PacketDistributor.sendToPlayer(player, new MaterialPhaseStatePayload(true));
            } else {
                active.remove(uuid);
                removeDamageBuff(player);
                PacketDistributor.sendToPlayer(player, new MaterialPhaseStatePayload(false));
            }
        }
        reconcileOfflinePersisted(activeAtSnapshot);
    }

    private void reconcileOfflinePersisted(Set<UUID> activeAtSnapshot) {
        boolean changed = false;
        for (UUID uuid : activeAtSnapshot) {
            if (server.getPlayerList().getPlayer(uuid) == null) changed |= persistedActive.add(uuid);
        }
        if (changed) savePersisted();
    }

    private void applyDamageBuff(ServerPlayer player) {
        AttributeInstance inst = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (inst != null) {
            inst.addOrUpdateTransientModifier(
                    new AttributeModifier(DAMAGE_BUFF_MODIFIER_ID, DAMAGE_BUFF_AMOUNT, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private void removeDamageBuff(ServerPlayer player) {
        AttributeInstance inst = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (inst != null) inst.removeModifier(DAMAGE_BUFF_MODIFIER_ID);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.server = event.getServer();
        this.persistFilePath = server.getWorldPath(LevelResource.ROOT).resolve(PERSIST_FILE_NAME);
        loadPersisted();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        active.clear();
        cooldownUntilTick.clear();
        this.server = null;
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        if (active.remove(uuid)) {
            persistedActive.add(uuid);
            savePersisted();
        }
    }

    public void restoreOnLogin(ServerPlayer player) {
        if (server == null) return;
        UUID uuid = player.getUUID();
        if (!persistedActive.remove(uuid)) return;
        savePersisted();

        if (!HahUeuh.LIONS_HEART.isActive(uuid)) return;
        active.add(uuid);
        applyDamageBuff(player);
        PacketDistributor.sendToPlayer(player, new MaterialPhaseStatePayload(true));
    }

    public void reloadPersisted() {
        loadPersisted();
    }

    private void loadPersisted() {
        persistedActive.clear();
        if (persistFilePath == null || !Files.exists(persistFilePath)) return;
        try {
            Set<String> raw = GSON.fromJson(Files.readString(persistFilePath, StandardCharsets.UTF_8), PERSIST_TYPE);
            if (raw == null) return;
            for (String key : raw) {
                try {
                    persistedActive.add(UUID.fromString(key));
                } catch (IllegalArgumentException e) {
                    HahUeuh.LOGGER.warn("Ignoring malformed Material Phase persisted UUID '{}'", key);
                }
            }
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to load persisted Material Phase state from {}", persistFilePath, e);
        }
    }

    private void savePersisted() {
        if (persistFilePath == null) return;
        try {
            Set<String> raw = new HashSet<>();
            persistedActive.forEach(uuid -> raw.add(uuid.toString()));
            Files.createDirectories(persistFilePath.getParent());
            Files.writeString(persistFilePath, GSON.toJson(raw, PERSIST_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to save persisted Material Phase state to {}", persistFilePath, e);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (server == null || active.isEmpty()) return;
        for (UUID uuid : new ArrayList<>(active)) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                active.remove(uuid);
                continue;
            }
            if (!HahUeuh.LIONS_HEART.isActive(uuid)) {
                deactivate(player, false);
                continue;
            }
            destroyPhasedBlocks(player);
        }
    }

    private void destroyPhasedBlocks(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!level.getGameRules().getBoolean(ModGameRules.REZERO_BLOCK_DESTRUCTION)) return;

        AABB core = player.getBoundingBox().deflate(CORE_INSET, 0.0, CORE_INSET);
        BlockPos min = BlockPos.containing(core.minX, core.minY, core.minZ);
        BlockPos max = BlockPos.containing(core.maxX, core.maxY, core.maxZ);
        boolean anyDestroyed = false;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getBlock() instanceof LiquidBlock) continue;
            if (state.getDestroySpeed(level, pos) < 0) continue; // unbreakable (bedrock, barrier, ...)

            level.sendParticles(ParticleTypes.EXPLOSION,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
            level.destroyBlock(pos.immutable(), ModGameRules.rollDrops(level));
            anyDestroyed = true;
        }
        if (anyDestroyed) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.PLAYERS, 0.4f, 1.4f + level.random.nextFloat() * 0.2f);
        }
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
            PacketDistributor.sendToPlayer(player, new AbilityCooldownPayload(HahUeuhAbilities.MATERIAL_PHASE_ABILITY, remaining));
        }
    }
}
