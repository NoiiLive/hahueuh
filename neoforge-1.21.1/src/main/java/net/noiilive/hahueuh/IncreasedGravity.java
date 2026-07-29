package net.noiilive.hahueuh;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class IncreasedGravity {
    public static final int EL_VITA_AMPLIFIER = 1;

    private static final ResourceLocation GRAVITY_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "vita_gravity");

    public boolean isActive(LivingEntity entity) {
        return entity.hasEffect(ModEffects.INCREASED_GRAVITY);
    }

    public int amplifier(LivingEntity entity) {
        MobEffectInstance instance = entity.getEffect(ModEffects.INCREASED_GRAVITY);
        return instance == null ? -1 : instance.getAmplifier();
    }

    public boolean isElVitaTier(LivingEntity entity) {
        return amplifier(entity) >= EL_VITA_AMPLIFIER;
    }

    public void refreshModifier(LivingEntity target) {
        MobEffectInstance instance = target.getEffect(ModEffects.INCREASED_GRAVITY);
        if (instance == null) {
            removeModifier(target);
            return;
        }
        AttributeInstance gravity = target.getAttribute(Attributes.GRAVITY);
        if (gravity == null) return;
        double baseline = Attributes.GRAVITY.value().getDefaultValue();
        double multiplier = instance.getAmplifier() >= EL_VITA_AMPLIFIER
                ? ConfigMagicYin.EL_VITA_GRAVITY_MULTIPLIER.get()
                : ConfigMagicYin.VITA_GRAVITY_MULTIPLIER.get();
        gravity.addOrUpdateTransientModifier(new AttributeModifier(
                GRAVITY_MODIFIER_ID, baseline * (multiplier - 1.0), AttributeModifier.Operation.ADD_VALUE));
    }

    public void removeModifier(LivingEntity target) {
        AttributeInstance gravity = target.getAttribute(Attributes.GRAVITY);
        if (gravity != null) gravity.removeModifier(GRAVITY_MODIFIER_ID);
    }

    @SubscribeEvent
    public void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % SpellUpkeep.TICKS_PER_SECOND != 0) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            MobEffectInstance instance = player.getEffect(ModEffects.INCREASED_GRAVITY);
            if (instance == null || !instance.isInfiniteDuration()) continue;

            int perSecond = instance.getAmplifier() >= EL_VITA_AMPLIFIER
                    ? ConfigMagicYin.EL_VITA_SELF_UPKEEP_PER_SECOND.get()
                    : ConfigMagicYin.VITA_SELF_UPKEEP_PER_SECOND.get();
            if (!SpellUpkeep.drain(player, perSecond)) {
                player.removeEffect(ModEffects.INCREASED_GRAVITY);
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
        if (!event.getEffect().is(ModEffects.INCREASED_GRAVITY)) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        removeModifier(entity);
    }

    @SubscribeEvent
    public void onEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || !instance.is(ModEffects.INCREASED_GRAVITY)) return;
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
