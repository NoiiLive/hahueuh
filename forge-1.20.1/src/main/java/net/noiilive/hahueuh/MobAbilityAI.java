package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.network.HandMode;
import net.noiilive.hahueuh.network.SlothVariant;
import net.noiilive.hahueuh.network.UnseenHandGrabSyncPacket;
import net.noiilive.hahueuh.network.UnseenHandSyncPacket;
import net.noiilive.hahueuh.network.WitchFactorAuthority;
import net.noiilive.hahueuh.snapshot.PlayerAuthorityManager;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.noiilive.hahueuh.network.ModNetworking;
import net.noiilive.hahueuh.capability.MobWitchFactorData;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MobAbilityAI {
    private static final int ATTEMPT_INTERVAL_TICKS = 20;
    private static final int HAND_HOLD_TICKS = 16;
    private static final int HAND_APPLY_AT_REMAINING = 6;
    private static final int SIN_PURSUIT_PRIORITY = 0;
    private static final double SIN_PURSUIT_SPEED = 1.0;
    private static final float SIN_PURSUIT_FALLBACK_DAMAGE = 1.0f;

    private record ActiveHand(HandMode mode, float distance, int ticksRemaining, boolean applied, int grabbedTargetId) {}

    private final Map<UUID, ActiveHand> activeHands = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> slothQuickCooldownUntilTick = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> retaliationUntilTick = new ConcurrentHashMap<>();

    private static final class SinPursuitGoal extends MeleeAttackGoal {
        SinPursuitGoal(PathfinderMob mob) {
            super(mob, SIN_PURSUIT_SPEED, false);
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target, double distanceSqr) {
            if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                super.checkAndPerformAttack(target, distanceSqr);
                return;
            }
            if (distanceSqr <= getAttackReachSqr(target) && isTimeToAttack()) {
                resetAttackCooldown();
                mob.swing(InteractionHand.MAIN_HAND);
                target.hurt(mob.damageSources().mobAttack(mob), SIN_PURSUIT_FALLBACK_DAMAGE);
            }
        }
    }

    @SubscribeEvent
    public void onEntityTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        WitchFactorAuthority sin = MobWitchFactorData.get(mob).getAuthority();
        boolean fingerMob = sin == WitchFactorAuthority.NONE
                && MobWitchFactorData.get(mob).getFingerHands() > 0;
        if (sin == WitchFactorAuthority.NONE && !fingerMob) return;

        if (mob.tickCount % 20 == 0 && Miasma.hasActiveSinToggle(mob)) {
            Miasma.addToggleSecond(mob);
        }

        expireRetaliation(level, mob);

        ActiveHand hand = activeHands.get(mob.getUUID());
        if (hand != null) {
            tickActiveHand(level, mob, hand);
            return;
        }

        if (mob.tickCount % ATTEMPT_INTERVAL_TICKS != 0) return;
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        if (fingerMob) {
            tryStartSlothQuickAction(level, mob, target);
            return;
        }
        switch (sin) {
            case SLOTH -> tryStartSlothQuickAction(level, mob, target);
            case GREED -> tryGreedAbility(mob, target);
            case NONE -> {}
        }
    }

    @SubscribeEvent
    public void onSinMobHurt(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        if (!isSinOrFingerMob(mob)) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == mob) return;
        if (hasNativeMeleeGoal(mob)) return;
        if (isProtectedFrom(mob, attacker)) return;

        int seconds = ConfigMain.MOB_SIN_RETALIATION_SECONDS.get();
        if (seconds <= 0) return;

        ensureSinPursuitGoal(mob);
        retaliationUntilTick.put(mob.getUUID(), level.getServer().getTickCount() + seconds * 20);
        LivingEntity current = mob.getTarget();
        if (current == null || !current.isAlive()) {
            mob.setTarget(attacker);
        }
    }

    private void expireRetaliation(ServerLevel level, Mob mob) {
        Integer until = retaliationUntilTick.get(mob.getUUID());
        if (until == null || level.getServer().getTickCount() < until) return;
        retaliationUntilTick.remove(mob.getUUID());
        if (mob.getTarget() != null) mob.setTarget(null);
    }

    private static boolean isSinOrFingerMob(Mob mob) {
        return MobWitchFactorData.get(mob).getAuthority() != WitchFactorAuthority.NONE
                || MobWitchFactorData.get(mob).getFingerHands() > 0;
    }

    private static boolean hasNativeMeleeGoal(Mob mob) {
        for (WrappedGoal wrapped : mob.goalSelector.getAvailableGoals()) {
            Goal goal = wrapped.getGoal();
            if (goal instanceof MeleeAttackGoal && !(goal instanceof SinPursuitGoal)) return true;
        }
        return false;
    }

    private static boolean hasSinPursuitGoal(Mob mob) {
        for (WrappedGoal wrapped : mob.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof SinPursuitGoal) return true;
        }
        return false;
    }

    private static void ensureSinPursuitGoal(Mob mob) {
        if (!(mob instanceof PathfinderMob pathfinder) || hasSinPursuitGoal(mob)) return;
        mob.goalSelector.addGoal(SIN_PURSUIT_PRIORITY, new SinPursuitGoal(pathfinder));
    }

    private static boolean isProtectedFrom(Mob mob, LivingEntity attacker) {
        UUID attackerId = attacker.getUUID();
        if (mob instanceof OwnableEntity ownable && attackerId.equals(ownable.getOwnerUUID())) return true;
        return attackerId.equals(HahUeuh.FINGER_GRANT.ownerOf(mob.getUUID()));
    }

    private void tryStartSlothQuickAction(ServerLevel level, Mob mob, LivingEntity target) {
        UUID uuid = mob.getUUID();
        Integer until = slothQuickCooldownUntilTick.get(uuid);
        if (until != null && level.getServer().getTickCount() < until) return;
        if (mob.getRandom().nextDouble() * 100.0 >= ConfigSloth.MOB_QUICK_ACTION_CHANCE.get()) return;

        float maxRange = ConfigSloth.SLOTH_MAX_DISTANCE.get();
        float reach = mob.distanceTo(target);
        if (reach > maxRange) return;

        HandMode mode = mob.getRandom().nextBoolean() ? HandMode.ATTACK : HandMode.GRAB;
        float distance = Math.max(1.0f, Math.min(reach, maxRange));
        activeHands.put(uuid, new ActiveHand(mode, distance, HAND_HOLD_TICKS, false, -1));
        broadcastHand(level, mob, true, mode, distance);
        Miasma.addSingleUse(mob);

        int cooldownSeconds = ConfigSloth.QUICK_ACTION_COOLDOWN_SECONDS.get();
        if (cooldownSeconds > 0) {
            slothQuickCooldownUntilTick.put(uuid, level.getServer().getTickCount() + cooldownSeconds * 20);
        }
    }

    private void tickActiveHand(ServerLevel level, Mob mob, ActiveHand hand) {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            endHand(level, mob);
            return;
        }

        int remaining = hand.ticksRemaining() - 1;
        boolean reached = remaining <= HAND_APPLY_AT_REMAINING;
        int grabbedId = hand.grabbedTargetId();

        if (reached && !hand.applied()) {
            AttributeInstance attackDamage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            float base = attackDamage != null ? (float) attackDamage.getValue() : 2.0f;
            target.hurt(mob.damageSources().mobAttack(mob), base * (hand.mode() == HandMode.GRAB ? 0.35f : 0.5f));
            if (hand.mode() == HandMode.GRAB) {
                grabbedId = target.getId();
                broadcastGrab(level, mob, List.of(target.getId()));
            } else {
                if (SlothVariant.byId(MobWitchFactorData.get(mob).getVariant()) == SlothVariant.SEKHMET) {
                    HahUeuh.SNAPSHOT_MANAGER.mobSekhmetBreakBlocks(level, target.position(),
                            SlothVariant.sekhmetSize(mob.getUUID()));
                }
            }
        }

        if (reached && hand.mode() == HandMode.GRAB) {
            dragTowardMob(mob, target);
        }

        ActiveHand updated = new ActiveHand(hand.mode(), hand.distance(), remaining,
                hand.applied() || reached, grabbedId);

        if (remaining <= 0) {
            endHand(level, mob);
        } else {
            activeHands.put(mob.getUUID(), updated);
        }
    }

    private void dragTowardMob(Mob mob, LivingEntity target) {
        double dx = mob.getX() - target.getX();
        double dz = mob.getZ() - target.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        if (horiz < 0.8) return;
        double step = Math.min(horiz - 0.6, 0.9);
        target.setDeltaMovement(dx / horiz * step, 0.1, dz / horiz * step);
        target.hasImpulse = true;
        target.hurtMarked = true;
        if (target instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
        }
    }

    private void endHand(ServerLevel level, Mob mob) {
        if (activeHands.remove(mob.getUUID()) == null) return;
        broadcastHand(level, mob, false, HandMode.NONE, 0f);
        broadcastGrab(level, mob, List.of());
    }

    private void broadcastHand(ServerLevel level, Mob mob, boolean active, HandMode mode, float distance) {
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
        SlothVariant variant;
        int count;
        if (MobWitchFactorData.get(mob).getAuthority() == WitchFactorAuthority.SLOTH) {
            variant = SlothVariant.byId(MobWitchFactorData.get(mob).getVariant());
            count = variant == SlothVariant.UNSEEN_HANDS ? SlothVariant.unseenHandCount(mob.getUUID()) : 0;
        } else {
            variant = SlothVariant.UNSEEN_HANDS;
            count = MobWitchFactorData.get(mob).getFingerHands();
        }
        UnseenHandSyncPacket payload = new UnseenHandSyncPacket(mob.getUUID(), mob.getId(), active, distance,
                mode.ordinal(), variant.ordinal(), false, count);
        ResourceKey<Level> dim = mob.level().dimension();
        for (ServerPlayer viewer : level.getServer().getPlayerList().getPlayers()) {
            if (!canSeeUnseenHands(am, viewer)) continue;
            if (!viewer.level().dimension().equals(dim)) continue;
            ModNetworking.sendToPlayer(viewer, payload);
        }
    }

    private static boolean canSeeUnseenHands(PlayerAuthorityManager am, ServerPlayer viewer) {
        return am.canUseSloth(viewer.getUUID()) || HahUeuh.FINGER_GRANT.receivedHands(viewer.getUUID()) > 0;
    }

    private void broadcastGrab(ServerLevel level, Mob mob, List<Integer> grabbedIds) {
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
        UnseenHandGrabSyncPacket payload = new UnseenHandGrabSyncPacket(mob.getUUID(), grabbedIds);
        ResourceKey<Level> dim = mob.level().dimension();
        for (ServerPlayer viewer : level.getServer().getPlayerList().getPlayers()) {
            if (!canSeeUnseenHands(am, viewer)) continue;
            if (!viewer.level().dimension().equals(dim)) continue;
            ModNetworking.sendToPlayer(viewer, payload);
        }
    }

    private void tryGreedAbility(Mob mob, LivingEntity target) {
        String variantId = MobWitchFactorData.get(mob).getVariant();
        if (GreedVariant.byId(variantId) != GreedVariant.LIONSHEART) return;

        UUID uuid = mob.getUUID();
        if (HahUeuh.LIONS_HEART.isActive(uuid)) {
            if (mob.getRandom().nextDouble() * 100.0 < ConfigGreed.MOB_OBJECT_FREEZE_CHANCE.get()) {
                HahUeuh.OBJECT_FREEZE.activateMob(mob);
                Miasma.addSingleUse(mob);
            }
        } else if (!HahUeuh.LIONS_HEART.isOnCooldown(uuid)
                && mob.getRandom().nextDouble() * 100.0 < ConfigGreed.MOB_LIONS_HEART_ACTIVATE_CHANCE.get()) {
            HahUeuh.LIONS_HEART.activateMob(mob);
        }
    }
}
