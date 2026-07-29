package net.noiilive.hahueuh;

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.noiilive.hahueuh.network.MurakStatePayload;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Murak {
    private static final ResourceLocation GRAVITY_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "murak_gravity");

    private final Set<UUID> flying = ConcurrentHashMap.newKeySet();
    private MinecraftServer server;

    public void tryCast(ServerPlayer caster) {
        if (caster.isShiftKeyDown() && hasReducedGravity(caster)) {
            clear(caster);
            actionBar(caster, "hahueuh.message.murak_self_ended", ChatFormatting.GRAY);
            return;
        }
        net.noiilive.hahueuh.magic.SpellRegistry.get(net.noiilive.hahueuh.magic.Spells.MURAK)
                .ifPresent(spell -> HahUeuh.SPELL_CASTING.tryStart(caster, spell));
    }

    public void cast(ServerPlayer caster) {
        if (caster.isShiftKeyDown()) {
            if (hasReducedGravity(caster)) {
                clear(caster);
                actionBar(caster, "hahueuh.message.murak_self_ended", ChatFormatting.GRAY);
            } else {
                apply(caster, MobEffectInstance.INFINITE_DURATION);
                actionBar(caster, "hahueuh.message.murak_self_started", ChatFormatting.AQUA);
            }
            return;
        }

        LivingEntity target = resolveTarget(caster);
        if (target == null) {
            actionBar(caster, "hahueuh.message.murak_no_target", ChatFormatting.RED);
            return;
        }

        apply(target, ConfigMagicYin.MURAK_TARGET_DURATION_SECONDS.get() * 20);
        actionBar(caster, "hahueuh.message.murak_target_started", ChatFormatting.AQUA);
    }

    private LivingEntity resolveTarget(ServerPlayer caster) {
        double range = ConfigMagicYin.MURAK_RANGE.get();
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(caster,
                e -> e != caster && e.isAlive() && !e.isSpectator() && e instanceof LivingEntity, range);
        return hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le ? le : null;
    }

    public boolean hasReducedGravity(LivingEntity entity) {
        return entity.hasEffect(ModEffects.REDUCED_GRAVITY);
    }

    private static boolean isSelfSustained(LivingEntity entity) {
        MobEffectInstance instance = entity.getEffect(ModEffects.REDUCED_GRAVITY);
        return instance != null && instance.isInfiniteDuration();
    }

    private void apply(LivingEntity target, int durationTicks) {
        target.addEffect(new MobEffectInstance(ModEffects.REDUCED_GRAVITY,
                durationTicks, 0, false, true, true));
        applyModifiers(target);

        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    24, 0.35, target.getBbHeight() * 0.4, 0.35, 0.02);
            level.playSound(null, target.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL,
                    SoundSource.PLAYERS, 0.9f, 1.6f);
        }
        syncTo(target);
    }

    public void clear(LivingEntity target) {
        target.removeEffect(ModEffects.REDUCED_GRAVITY);
        removeModifiers(target);
        endFlight(target);

        if (target.level() instanceof ServerLevel level) {
            level.playSound(null, target.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL,
                    SoundSource.PLAYERS, 0.7f, 0.7f);
        }
        syncTo(target);
    }

    private void applyModifiers(LivingEntity target) {
        if (!hasReducedGravity(target)) {
            removeModifiers(target);
            return;
        }
        AttributeInstance gravity = target.getAttribute(Attributes.GRAVITY);
        if (gravity != null) {
            double effective = flying.contains(target.getUUID()) ? 0.0 : ConfigMagicYin.MURAK_GRAVITY.get();
            double delta = effective - Attributes.GRAVITY.value().getDefaultValue();
            gravity.addOrUpdateTransientModifier(new AttributeModifier(
                    GRAVITY_MODIFIER_ID, delta, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private void removeModifiers(LivingEntity target) {
        AttributeInstance gravity = target.getAttribute(Attributes.GRAVITY);
        if (gravity != null) gravity.removeModifier(GRAVITY_MODIFIER_ID);
    }

    public boolean isFlying(UUID uuid) {
        return flying.contains(uuid);
    }

    public void setFlying(ServerPlayer player, boolean wantsFlight) {
        if (wantsFlight) {
            if (!hasReducedGravity(player) || player.isSpectator()) return;
            if (!flying.add(player.getUUID())) return;
            player.level().playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP,
                    SoundSource.PLAYERS, 0.8f, 1.4f);
        } else if (!flying.remove(player.getUUID())) {
            return;
        }
        applyModifiers(player);
        player.fallDistance = 0;
        syncTo(player);
    }

    private void endFlight(LivingEntity target) {
        if (flying.remove(target.getUUID())) {
            target.fallDistance = 0;
        }
    }

    private void syncTo(LivingEntity target) {
        if (target instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player,
                    new MurakStatePayload(hasReducedGravity(player), flying.contains(player.getUUID())));
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer srv = server;
        if (srv == null) return;

        if (event.getServer().getTickCount() % SpellUpkeep.TICKS_PER_SECOND == 0) {
            for (ServerPlayer player : srv.getPlayerList().getPlayers()) {
                if (!isSelfSustained(player)) continue;
                if (!SpellUpkeep.drain(player, ConfigMagicYin.MURAK_SELF_UPKEEP_PER_SECOND.get())) {
                    clear(player);
                }
            }
        }

        if (flying.isEmpty()) return;
        for (UUID uuid : new ArrayList<>(flying)) {
            ServerPlayer player = srv.getPlayerList().getPlayer(uuid);
            if (player == null) {
                flying.remove(uuid);
                continue;
            }
            if (!hasReducedGravity(player) || player.isSpectator()) {
                setFlying(player, false);
                continue;
            }
            player.fallDistance = 0;
        }
    }

    @SubscribeEvent
    public void onFall(net.neoforged.neoforge.event.entity.living.LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !hasReducedGravity(entity)) return;
        event.setCanceled(true);
        entity.fallDistance = 0f;
    }

    @SubscribeEvent
    public void onEffectRemoved(MobEffectEvent.Remove event) {
        if (!event.getEffect().is(ModEffects.REDUCED_GRAVITY)) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        removeModifiers(entity);
        endFlight(entity);
        syncTo(entity);
    }

    @SubscribeEvent
    public void onEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || !instance.is(ModEffects.REDUCED_GRAVITY)) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        removeModifiers(entity);
        endFlight(entity);
        syncTo(entity);
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        removeModifiers(entity);
        endFlight(entity);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (hasReducedGravity(player)) applyModifiers(player);
            syncTo(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        flying.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.server = event.getServer();
        flying.clear();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        flying.clear();
        this.server = null;
    }

    private static void actionBar(ServerPlayer player, String key, ChatFormatting style) {
        player.displayClientMessage(Component.translatable(key).withStyle(style), true);
    }
}
