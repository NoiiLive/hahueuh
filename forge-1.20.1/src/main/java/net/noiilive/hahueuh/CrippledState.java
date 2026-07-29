package net.noiilive.hahueuh;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.noiilive.hahueuh.capability.PlayerData;
import net.noiilive.hahueuh.capability.PlayerDataEvents;
import net.noiilive.hahueuh.network.GateStatus;

import java.util.UUID;

public final class CrippledState {
    private static final UUID MAX_HEALTH_LOCK_ID = UUID.fromString("8f1c2d3e-4a5b-4c6d-8e9f-0a1b2c3d4e5f");
    private static final UUID STEP_HEIGHT_ID = UUID.fromString("9a2d3e4f-5b6c-4d7e-9f0a-1b2c3d4e5f60");
    private static final float CRIPPLED_MAX_HEALTH = 1.0f;
    private static final double CRIPPLED_STEP_HEIGHT_ADDITION = 0.5;
    private static final int REAPPLY_INTERVAL = 20;
    private static final int EFFECT_DURATION = 60;
    private static final int CRIPPLED_WEAKNESS_AMPLIFIER = 2;
    private static final int CRIPPLED_SLOWNESS_AMPLIFIER = 2;

    private static boolean isDepleted(ServerPlayer player) {
        return PlayerData.get(player).isOdDepleted();
    }

    private static int currentOd(ServerPlayer player) {
        return PlayerData.get(player).getOdCurrent();
    }

    public void checkRecovery(ServerPlayer player) {
        if (!isDepleted(player) || currentOd(player) <= 0) return;
        clear(player);
        player.displayClientMessage(Component.translatable("hahueuh.message.od_recovered")
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    public void afflict(ServerPlayer player) {
        if (isDepleted(player)) return;
        PlayerData data = PlayerData.get(player);

        if (!ConfigMagic.CRIPPLED_ENABLED.get()) {
            data.setOdCurrent(BookOfLifeStats.maxOd(data));
            PlayerDataEvents.sync(player);
            int lockoutSeconds = ConfigMagic.CRIPPLED_MAGIC_LOCKOUT_MINUTES.get() * 60;
            HahUeuh.SPELL_CASTING.lockOutAllSpells(player, lockoutSeconds);
            player.displayClientMessage(Component.translatable("hahueuh.message.od_depleted_soft",
                    ConfigMagic.CRIPPLED_MAGIC_LOCKOUT_MINUTES.get()).withStyle(ChatFormatting.DARK_GRAY), false);
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 0.8f);
            return;
        }

        data.setOdDepleted(true);
        data.setGateStatus(GateStatus.DESTROYED);
        PlayerDataEvents.sync(player);
        reapply(player);
        player.displayClientMessage(Component.translatable("hahueuh.message.od_depleted")
                .withStyle(ChatFormatting.DARK_GRAY), false);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 0.6f);
    }

    private void reapply(ServerPlayer player) {
        AttributeInstance maxHp = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) {
            double lock = CRIPPLED_MAX_HEALTH - maxHp.getBaseValue();
            maxHp.removeModifier(MAX_HEALTH_LOCK_ID);
            maxHp.addTransientModifier(new AttributeModifier(
                    MAX_HEALTH_LOCK_ID, "hahueuh:od_depleted_max_health", lock,
                    AttributeModifier.Operation.ADDITION));
        }
        AttributeInstance step = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (step != null) {
            step.removeModifier(STEP_HEIGHT_ID);
            step.addTransientModifier(new AttributeModifier(
                    STEP_HEIGHT_ID, "hahueuh:od_depleted_step_height", CRIPPLED_STEP_HEIGHT_ADDITION,
                    AttributeModifier.Operation.ADDITION));
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_DURATION,
                CRIPPLED_WEAKNESS_AMPLIFIER, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION,
                CRIPPLED_SLOWNESS_AMPLIFIER, false, false, true));
    }

    private void clear(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        data.setOdDepleted(false);
        removeModifier(player, Attributes.MAX_HEALTH, MAX_HEALTH_LOCK_ID);
        removeModifier(player, ForgeMod.STEP_HEIGHT_ADDITION.get(), STEP_HEIGHT_ID);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        data.setGateStatus(GateStatus.OPEN);
        PlayerDataEvents.sync(player);
        GateStrain.setStrain(player, 0);
    }

    private static void removeModifier(ServerPlayer player, Attribute attribute, UUID id) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst != null) inst.removeModifier(id);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getTickCount() % REAPPLY_INTERVAL != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!isDepleted(player)) continue;
            checkRecovery(player);
            if (isDepleted(player)) reapply(player);
        }
    }

    @SubscribeEvent
    public void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isDepleted(player)) return;
        Vec3 motion = player.getDeltaMovement();
        if (motion.y > 0.0) {
            player.setDeltaMovement(motion.x, 0.0, motion.z);
            player.hasImpulse = true;
            player.hurtMarked = true;
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        checkRecovery(player);
        if (isDepleted(player)) reapply(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isDepleted(player)) return;
        event.setCanceled(true);

        ServerLevel level = player.serverLevel();
        double x = player.getX(), y = player.getY() + 1.0, z = player.getZ();
        level.sendParticles(ParticleTypes.ASH, x, y, z, 80, 0.4, 0.9, 0.4, 0.03);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 24, 0.3, 0.6, 0.3, 0.01);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.9f, 0.7f);

        MinecraftServer server = player.getServer();
        if (server != null && level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES)) {
            server.getPlayerList().broadcastSystemMessage(Component.translatable("hahueuh.message.od_death",
                    player.getDisplayName()), false);
        }

        player.getInventory().clearContent();
        clear(player);
        PlayerData data = PlayerData.get(player);
        data.setOdCurrent(BookOfLifeStats.maxOd(data));
        PlayerDataEvents.sync(player);
        player.setRemainingFireTicks(0);
        player.clearFire();
        respawnAtSpawn(player);
        player.setHealth(player.getMaxHealth());
    }

    private static void respawnAtSpawn(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        ServerLevel target = player.getRespawnDimension() != null
                ? server.getLevel(player.getRespawnDimension()) : null;
        if (target == null) target = server.overworld();

        BlockPos respawnPos = player.getRespawnPosition();
        if (respawnPos == null) respawnPos = target.getSharedSpawnPos();

        player.teleportTo(target, respawnPos.getX() + 0.5, respawnPos.getY(), respawnPos.getZ() + 0.5,
                player.getYRot(), player.getXRot());
    }
}
