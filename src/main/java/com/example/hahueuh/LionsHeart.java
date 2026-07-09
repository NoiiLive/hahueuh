package com.example.hahueuh;

import com.example.hahueuh.network.AbilityCooldownPayload;
import com.example.hahueuh.network.LionsHeartStatePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LionsHeart {
    private static final int WARNING_TICKS = 4 * 20;
    private static final int GRACE_TICKS = 5 * 20;
    private static final int BURNOUT_TICKS = 5 * 20;
    private static final int BURNOUT_DAMAGE_INTERVAL_TICKS = 5;
    private static final float BURNOUT_DAMAGE_MIN = 4f;
    private static final float BURNOUT_DAMAGE_MAX = 20f;

    private final Map<UUID, Integer> activatedAtTick = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> cooldownUntilTick = new ConcurrentHashMap<>();
    private final Set<UUID> burnoutInProgress = ConcurrentHashMap.newKeySet();
    private MinecraftServer server;

    public boolean isActive(UUID uuid) {
        return activatedAtTick.containsKey(uuid);
    }

    public void forceResetOnRollback(ServerPlayer player) {
        UUID uuid = player.getUUID();
        burnoutInProgress.remove(uuid);
        if (activatedAtTick.remove(uuid) != null) {
            PacketDistributor.sendToPlayer(player, new LionsHeartStatePayload(false));
        }
    }

    public void toggle(ServerPlayer player) {
        if (server == null) return;
        UUID uuid = player.getUUID();

        if (isActive(uuid)) {
            deactivate(player, "hahueuh.message.lions_heart_deactivated");
            return;
        }

        if (!HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().canUseGreed(uuid)) {
            player.displayClientMessage(Component.translatable("hahueuh.message.no_greed_authority")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        int remainingCooldown = player.isCreative() ? 0 : cooldownRemainingTicks(uuid);
        if (remainingCooldown > 0) {
            int seconds = (int) Math.ceil(remainingCooldown / 20.0);
            player.displayClientMessage(Component.translatable("hahueuh.message.lions_heart_cooldown", seconds)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        activate(player);
    }

    private void activate(ServerPlayer player) {
        activatedAtTick.put(player.getUUID(), server.getTickCount());
        PacketDistributor.sendToPlayer(player, new LionsHeartStatePayload(true));
        player.level().playSound(null, player, ModSounds.LIONSHEART_ACTIVATE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        player.displayClientMessage(Component.translatable("hahueuh.message.lions_heart_activated")
                .withStyle(ChatFormatting.GOLD), true);
    }

    private void deactivate(ServerPlayer player, String messageKey) {
        UUID uuid = player.getUUID();
        activatedAtTick.remove(uuid);
        PacketDistributor.sendToPlayer(player, new LionsHeartStatePayload(false));
        player.level().playSound(null, player, ModSounds.LIONSHEART_DEACTIVATE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

        if (!player.isCreative()) {
            int cooldownSeconds = Config.LIONS_HEART_COOLDOWN_SECONDS.getAsInt();
            if (cooldownSeconds > 0) {
                cooldownUntilTick.put(uuid, server.getTickCount() + cooldownSeconds * 20);
                PacketDistributor.sendToPlayer(player,
                        new AbilityCooldownPayload(HahUeuhAbilities.LIONS_HEART_ABILITY, cooldownSeconds * 20));
            }
        }

        player.displayClientMessage(Component.translatable(messageKey).withStyle(ChatFormatting.GOLD), true);
    }

    private int cooldownRemainingTicks(UUID uuid) {
        Integer until = cooldownUntilTick.get(uuid);
        if (until == null || server == null) return 0;
        return Math.max(0, until - server.getTickCount());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.server = event.getServer();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        activatedAtTick.clear();
        cooldownUntilTick.clear();
        burnoutInProgress.clear();
        this.server = null;
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isActive(player.getUUID())) {
            deactivate(player, "hahueuh.message.lions_heart_deactivated");
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (server == null || activatedAtTick.isEmpty()) return;
        for (UUID uuid : new ArrayList<>(activatedAtTick.keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                activatedAtTick.remove(uuid);
                continue;
            }
            tickActive(player, activatedAtTick.get(uuid));
        }
    }

    private void tickActive(ServerPlayer player, int activatedAt) {
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20f);
        player.setAirSupply(player.getMaxAirSupply());
        player.clearFire();
        player.setTicksFrozen(0);

        int elapsed = server.getTickCount() - activatedAt;
        if (elapsed == WARNING_TICKS) {
            player.displayClientMessage(Component.translatable("hahueuh.message.lions_heart_strain")
                    .withStyle(ChatFormatting.YELLOW), true);
        }
        if (elapsed < GRACE_TICKS) return;

        int burnoutElapsed = elapsed - GRACE_TICKS;
        if (burnoutElapsed % BURNOUT_DAMAGE_INTERVAL_TICKS == 0) {
            UUID uuid = player.getUUID();
            float progress = Math.min(1f, (float) burnoutElapsed / BURNOUT_TICKS);
            float damage = BURNOUT_DAMAGE_MIN + progress * (BURNOUT_DAMAGE_MAX - BURNOUT_DAMAGE_MIN);
            burnoutInProgress.add(uuid);
            player.hurt(player.damageSources().indirectMagic(player, player), damage);
            burnoutInProgress.remove(uuid);
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isActive(player.getUUID())) return;
        UUID uuid = player.getUUID();
        activatedAtTick.remove(uuid);
        burnoutInProgress.remove(uuid);
        PacketDistributor.sendToPlayer(player, new LionsHeartStatePayload(false));
        player.level().playSound(null, player, ModSounds.LIONSHEART_DEACTIVATE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        if (server != null && !player.isCreative()) {
            int cooldownSeconds = Config.LIONS_HEART_COOLDOWN_SECONDS.getAsInt();
            if (cooldownSeconds > 0) {
                cooldownUntilTick.put(uuid, server.getTickCount() + cooldownSeconds * 20);
                PacketDistributor.sendToPlayer(player,
                        new AbilityCooldownPayload(HahUeuhAbilities.LIONS_HEART_ABILITY, cooldownSeconds * 20));
            }
        }
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer target && isActive(target.getUUID())
                && !burnoutInProgress.contains(target.getUUID())) {
            event.setCanceled(true);
            return;
        }

        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof ServerPlayer attackingPlayer && isActive(attackingPlayer.getUUID())) {
            event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> 0f);
            event.addReductionModifier(DamageContainer.Reduction.ENCHANTMENTS, (container, reduction) -> 0f);
        }
    }

    @SubscribeEvent
    public void onShieldBlock(LivingShieldBlockEvent event) {
        Entity attacker = event.getDamageSource().getEntity();
        if (attacker instanceof ServerPlayer attackingPlayer && isActive(attackingPlayer.getUUID())) {
            event.setBlocked(false);
        }
    }
}
