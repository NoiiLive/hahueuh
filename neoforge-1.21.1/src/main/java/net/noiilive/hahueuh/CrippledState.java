package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.GateStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Set;

public final class CrippledState {
    private static final ResourceLocation MAX_HEALTH_LOCK_ID =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "od_depleted_max_health");
    private static final ResourceLocation JUMP_LOCK_ID =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "od_depleted_jump");
    private static final ResourceLocation STEP_HEIGHT_ID =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "od_depleted_step_height");
    private static final float CRIPPLED_MAX_HEALTH = 1.0f;
    private static final double CRIPPLED_STEP_HEIGHT = 1.0;
    private static final int REAPPLY_INTERVAL = 20;
    private static final int EFFECT_DURATION = 60;
    private static final int CRIPPLED_WEAKNESS_AMPLIFIER = 2;
    private static final int CRIPPLED_SLOWNESS_AMPLIFIER = 2;

    private static boolean isDepleted(ServerPlayer player) {
        return player.getData(ModAttachments.PLAYER_OD_DEPLETED.get());
    }

    private static int currentOd(ServerPlayer player) {
        return player.getData(ModAttachments.PLAYER_OD_CURRENT.get());
    }

    /**
     * Un-cripples a player whose Od has been restored above zero by some other means
     * (commands, lifespan reroll, etc.) without going through a normal death respawn.
     */
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

        if (!ConfigMagic.CRIPPLED_ENABLED.get()) {
            player.setData(ModAttachments.PLAYER_OD_CURRENT.get(), BookOfLifeStats.maxOd(player));
            int lockoutSeconds = ConfigMagic.CRIPPLED_MAGIC_LOCKOUT_MINUTES.getAsInt() * 60;
            HahUeuh.SPELL_CASTING.lockOutAllSpells(player, lockoutSeconds);
            player.displayClientMessage(Component.translatable("hahueuh.message.od_depleted_soft",
                    ConfigMagic.CRIPPLED_MAGIC_LOCKOUT_MINUTES.getAsInt()).withStyle(ChatFormatting.DARK_GRAY), false);
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 0.8f);
            return;
        }

        player.setData(ModAttachments.PLAYER_OD_DEPLETED.get(), true);
        player.setData(ModAttachments.PLAYER_GATE_STATUS.get(), GateStatus.DESTROYED);
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
            maxHp.addOrUpdateTransientModifier(
                    new AttributeModifier(MAX_HEALTH_LOCK_ID, lock, AttributeModifier.Operation.ADD_VALUE));
        }
        AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null) {
            jump.addOrUpdateTransientModifier(
                    new AttributeModifier(JUMP_LOCK_ID, -jump.getBaseValue(), AttributeModifier.Operation.ADD_VALUE));
        }
        AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
        if (step != null) {
            step.addOrUpdateTransientModifier(new AttributeModifier(STEP_HEIGHT_ID,
                    CRIPPLED_STEP_HEIGHT - step.getBaseValue(), AttributeModifier.Operation.ADD_VALUE));
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_DURATION, CRIPPLED_WEAKNESS_AMPLIFIER, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION, CRIPPLED_SLOWNESS_AMPLIFIER, false, false, true));
    }

    private void clear(ServerPlayer player) {
        player.setData(ModAttachments.PLAYER_OD_DEPLETED.get(), false);
        removeModifier(player, Attributes.MAX_HEALTH, MAX_HEALTH_LOCK_ID);
        removeModifier(player, Attributes.JUMP_STRENGTH, JUMP_LOCK_ID);
        removeModifier(player, Attributes.STEP_HEIGHT, STEP_HEIGHT_ID);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.setData(ModAttachments.PLAYER_GATE_STATUS.get(), GateStatus.OPEN);
        GateStrain.setStrain(player, 0);
    }

    private static void removeModifier(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, ResourceLocation id) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst != null) inst.removeModifier(id);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % REAPPLY_INTERVAL != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!isDepleted(player)) continue;
            checkRecovery(player);
            if (isDepleted(player)) reapply(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isDepleted(player)) return;
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
        if (server != null && level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_SHOWDEATHMESSAGES)) {
            server.getPlayerList().broadcastSystemMessage(Component.translatable("hahueuh.message.od_death",
                    player.getDisplayName()), false);
        }

        player.getInventory().clearContent();
        clear(player);
        player.setData(ModAttachments.PLAYER_OD_CURRENT.get(), BookOfLifeStats.maxOd(player));
        player.setRemainingFireTicks(0);
        player.clearFire();
        respawnAtSpawn(player);
        player.setHealth(player.getMaxHealth());
    }

    private static void respawnAtSpawn(ServerPlayer player) {
        DimensionTransition dt = player.findRespawnPositionAndUseSpawnBlock(false, DimensionTransition.DO_NOTHING);
        Vec3 dest = dt.pos();
        player.teleportTo(dt.newLevel(), dest.x, dest.y, dest.z, Set.of(), dt.yRot(), dt.xRot());
    }
}
