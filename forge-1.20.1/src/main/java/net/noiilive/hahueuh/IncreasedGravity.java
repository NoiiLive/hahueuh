package net.noiilive.hahueuh;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;

public final class IncreasedGravity {
    public static final int EL_VITA_AMPLIFIER = 1;

    private static final UUID GRAVITY_MODIFIER_ID = UUID.fromString("7c4a1d3e-9b52-4a6f-8f1c-3d5e7a9b0c11");

    public boolean isActive(LivingEntity entity) {
        return entity.hasEffect(ModEffects.INCREASED_GRAVITY.get());
    }

    public int amplifier(LivingEntity entity) {
        MobEffectInstance instance = entity.getEffect(ModEffects.INCREASED_GRAVITY.get());
        return instance == null ? -1 : instance.getAmplifier();
    }

    public boolean isElVitaTier(LivingEntity entity) {
        return amplifier(entity) >= EL_VITA_AMPLIFIER;
    }

    public void refreshModifier(LivingEntity target) {
        MobEffectInstance instance = target.getEffect(ModEffects.INCREASED_GRAVITY.get());
        if (instance == null) {
            removeModifier(target);
            return;
        }
        AttributeInstance gravity = target.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
        if (gravity == null) return;
        double baseline = ForgeMod.ENTITY_GRAVITY.get().getDefaultValue();
        double multiplier = instance.getAmplifier() >= EL_VITA_AMPLIFIER
                ? ConfigMagicYin.EL_VITA_GRAVITY_MULTIPLIER.get()
                : ConfigMagicYin.VITA_GRAVITY_MULTIPLIER.get();
        gravity.removeModifier(GRAVITY_MODIFIER_ID);
        gravity.addTransientModifier(new AttributeModifier(GRAVITY_MODIFIER_ID, "hahueuh:vita_gravity",
                baseline * (multiplier - 1.0), AttributeModifier.Operation.ADDITION));
    }

    public void removeModifier(LivingEntity target) {
        AttributeInstance gravity = target.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
        if (gravity != null) gravity.removeModifier(GRAVITY_MODIFIER_ID);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getTickCount() % SpellUpkeep.TICKS_PER_SECOND != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            MobEffectInstance instance = player.getEffect(ModEffects.INCREASED_GRAVITY.get());
            if (!SpellUpkeep.isUntimed(instance)) continue;

            int perSecond = instance.getAmplifier() >= EL_VITA_AMPLIFIER
                    ? ConfigMagicYin.EL_VITA_SELF_UPKEEP_PER_SECOND.get()
                    : ConfigMagicYin.VITA_SELF_UPKEEP_PER_SECOND.get();
            if (!SpellUpkeep.drain(player, perSecond)) {
                player.removeEffect(ModEffects.INCREASED_GRAVITY.get());
                removeModifier(player);
            }
        }
    }

    @SubscribeEvent
    public void onFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !isActive(entity)) return;
        double multiplier = isElVitaTier(entity)
                ? ConfigMagicYin.EL_VITA_FALL_DAMAGE_MULTIPLIER.get()
                : ConfigMagicYin.VITA_FALL_DAMAGE_MULTIPLIER.get();
        event.setDamageMultiplier((float) (event.getDamageMultiplier() * multiplier));
    }

    @SubscribeEvent
    public void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEffect() != ModEffects.INCREASED_GRAVITY.get()) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        removeModifier(entity);
    }

    @SubscribeEvent
    public void onEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || instance.getEffect() != ModEffects.INCREASED_GRAVITY.get()) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        removeModifier(entity);
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        removeModifier(entity);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isActive(player)) {
            refreshModifier(player);
        }
    }
}
