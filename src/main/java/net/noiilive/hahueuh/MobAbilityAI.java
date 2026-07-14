package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.network.HandMode;
import net.noiilive.hahueuh.network.SlothVariant;
import net.noiilive.hahueuh.network.UnseenHandGrabSyncPayload;
import net.noiilive.hahueuh.network.UnseenHandSyncPayload;
import net.noiilive.hahueuh.network.WitchFactorAuthority;
import net.noiilive.hahueuh.snapshot.PlayerAuthorityManager;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MobAbilityAI {
    private static final int ATTEMPT_INTERVAL_TICKS = 20;
    private static final int HAND_HOLD_TICKS = 16;
    private static final int HAND_APPLY_AT_REMAINING = 6;

    private record ActiveHand(HandMode mode, float distance, int ticksRemaining, boolean applied, int grabbedTargetId) {}

    private final Map<UUID, ActiveHand> activeHands = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> slothQuickCooldownUntilTick = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        WitchFactorAuthority sin = mob.getData(ModAttachments.MOB_WITCH_FACTOR.get());
        if (sin == WitchFactorAuthority.NONE) return;

        ActiveHand hand = activeHands.get(mob.getUUID());
        if (hand != null) {
            tickActiveHand(level, mob, hand);
            return;
        }

        if (mob.tickCount % ATTEMPT_INTERVAL_TICKS != 0) return;
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        switch (sin) {
            case SLOTH -> tryStartSlothQuickAction(level, mob, target);
            case GREED -> tryGreedAbility(mob, target);
            case NONE -> {}
        }
    }

    private void tryStartSlothQuickAction(ServerLevel level, Mob mob, LivingEntity target) {
        UUID uuid = mob.getUUID();
        Integer until = slothQuickCooldownUntilTick.get(uuid);
        if (until != null && level.getServer().getTickCount() < until) return;
        if (mob.getRandom().nextDouble() * 100.0 >= ConfigSloth.MOB_QUICK_ACTION_CHANCE.get()) return;

        float maxRange = ConfigSloth.SLOTH_MAX_DISTANCE.getAsInt();
        float reach = mob.distanceTo(target);
        if (reach > maxRange) return;

        HandMode mode = mob.getRandom().nextBoolean() ? HandMode.ATTACK : HandMode.GRAB;
        float distance = Math.max(1.0f, Math.min(reach, maxRange));
        activeHands.put(uuid, new ActiveHand(mode, distance, HAND_HOLD_TICKS, false, -1));
        broadcastHand(level, mob, true, mode, distance);

        int cooldownSeconds = ConfigSloth.QUICK_ACTION_COOLDOWN_SECONDS.getAsInt();
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
                if (SlothVariant.byId(mob.getData(ModAttachments.MOB_WITCH_FACTOR_VARIANT.get())) == SlothVariant.SEKHMET) {
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
        if (horiz < 0.8) return; // already in melee range — don't jitter them into/through the mob
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
        int variantOrdinal = SlothVariant.byId(mob.getData(ModAttachments.MOB_WITCH_FACTOR_VARIANT.get())).ordinal();
        UnseenHandSyncPayload payload = new UnseenHandSyncPayload(mob.getUUID(), mob.getId(), active, distance,
                mode.ordinal(), variantOrdinal, false);
        ResourceKey<Level> dim = mob.level().dimension();
        for (ServerPlayer viewer : level.getServer().getPlayerList().getPlayers()) {
            if (!am.canUseSloth(viewer.getUUID())) continue;
            if (!viewer.level().dimension().equals(dim)) continue;
            PacketDistributor.sendToPlayer(viewer, payload);
        }
    }

    private void broadcastGrab(ServerLevel level, Mob mob, List<Integer> grabbedIds) {
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
        UnseenHandGrabSyncPayload payload = new UnseenHandGrabSyncPayload(mob.getUUID(), grabbedIds);
        ResourceKey<Level> dim = mob.level().dimension();
        for (ServerPlayer viewer : level.getServer().getPlayerList().getPlayers()) {
            if (!am.canUseSloth(viewer.getUUID())) continue;
            if (!viewer.level().dimension().equals(dim)) continue;
            PacketDistributor.sendToPlayer(viewer, payload);
        }
    }

    private void tryGreedAbility(Mob mob, LivingEntity target) {
        String variantId = mob.getData(ModAttachments.MOB_WITCH_FACTOR_VARIANT.get());
        if (GreedVariant.byId(variantId) != GreedVariant.LIONSHEART) return;

        UUID uuid = mob.getUUID();
        if (HahUeuh.LIONS_HEART.isActive(uuid)) {
            if (mob.getRandom().nextDouble() * 100.0 < ConfigGreed.MOB_OBJECT_FREEZE_CHANCE.get()) {
                HahUeuh.OBJECT_FREEZE.activateMob(mob);
            }
        } else if (!HahUeuh.LIONS_HEART.isOnCooldown(uuid)
                && mob.getRandom().nextDouble() * 100.0 < ConfigGreed.MOB_LIONS_HEART_ACTIVATE_CHANCE.get()) {
            HahUeuh.LIONS_HEART.activateMob(mob);
        }
    }
}
