package net.noiilive.hahueuh;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.noiilive.hahueuh.network.ModNetworking;
import net.noiilive.hahueuh.network.GuiltywhipCrackSyncPacket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Guiltywhip {

    private static final double SEGMENT_HIT_RADIUS = 0.35;
    private static final double GRAPPLE_BASE_PULL = 0.35;
    private static final double GRAPPLE_PULL_PER_BLOCK = 0.19;
    private static final double GRAPPLE_MAX_PULL = 1.45;
    private static final double GRAPPLE_SELF_LIFT = 0.28;
    private static final double GRAPPLE_TARGET_LIFT = 0.2;
    private static final double GRAPPLE_VALIDATE_FUDGE = 3.0;
    private static final String GRAPPLE_FALL_TAG = "hahueuh_grapple_fall";
    private static final int GRAPPLE_FALL_SETTLE = 3;
    private static final int GRAB_GUARD_TICKS = 15;

    private final Map<UUID, GuiltywhipPhysics.State> states = new ConcurrentHashMap<>();
    private final Map<UUID, Pull> pendingPulls = new ConcurrentHashMap<>();
    private final Map<UUID, GrabGuard> grabGuards = new ConcurrentHashMap<>();

    private record GrabGuard(int attackerId, long expiresAt) {}

    public void handleCrack(ServerPlayer player, boolean sweep) {
        if (!(player.getMainHandItem().getItem() instanceof GuiltywhipItem)) return;
        GuiltywhipPhysics.State state = states.get(player.getUUID());
        if (state == null || !GuiltywhipPhysics.canCrack(state)) return;

        GuiltywhipPhysics.crack(state, player, sweep, GuiltywhipPhysics.handAnchor(player));
        player.serverLevel().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.6f);
        player.serverLevel().playSound(null, player.blockPosition(),
                SoundEvents.LEASH_KNOT_BREAK, SoundSource.PLAYERS, 0.9f, 1.5f);

        for (ServerPlayer other : player.serverLevel().players()) {
            if (other != player) {
                ModNetworking.sendToPlayer(other, new GuiltywhipCrackSyncPacket(player.getUUID(), sweep));
            }
        }
    }

    public void handleGrapple(ServerPlayer player, int targetId, Vec3 blockPoint) {
        if (!(player.getMainHandItem().getItem() instanceof GuiltywhipItem)) return;
        GuiltywhipPhysics.State state = states.get(player.getUUID());
        if (state == null || !GuiltywhipPhysics.canGrapple(state)) return;

        net.minecraft.world.entity.Entity target = null;
        if (targetId >= 0) {
            net.minecraft.world.entity.Entity candidate = player.serverLevel().getEntity(targetId);
            if (candidate != null && candidate != player && GuiltywhipPhysics.grabbable(candidate)
                    && candidate.distanceTo(player)
                            <= GuiltywhipPhysics.GRAPPLE_LENGTH + GRAPPLE_VALIDATE_FUDGE) {
                target = candidate;
            }
        }
        Vec3 point = null;
        if (target == null) {
            if (blockPoint != null && blockPoint.distanceTo(player.getEyePosition())
                    <= GuiltywhipPhysics.GRAPPLE_LENGTH + GRAPPLE_VALIDATE_FUDGE) {
                point = blockPoint;
            } else {
                target = GuiltywhipPhysics.findGrabTarget(player);
                if (target == null) point = GuiltywhipPhysics.findGrapplePoint(player);
            }
        }

        GuiltywhipPhysics.grapple(state, player, GuiltywhipPhysics.handAnchor(player),
                target, point);
        Pull pull = target != null ? new Pull(target.getId(), null)
                : point != null ? new Pull(-1, point) : null;
        if (pull != null) {
            pendingPulls.put(player.getUUID(), pull);
        } else {
            pendingPulls.remove(player.getUUID());
        }

        player.serverLevel().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5f, 1.9f);
        if (pull != null) {
            player.serverLevel().playSound(null, player.blockPosition(),
                    SoundEvents.LEASH_KNOT_PLACE, SoundSource.PLAYERS, 0.9f, 1.2f);
        } else {
            player.serverLevel().playSound(null, player.blockPosition(),
                    SoundEvents.LEASH_KNOT_BREAK, SoundSource.PLAYERS, 0.5f, 1.7f);
        }

        net.noiilive.hahueuh.network.GuiltywhipGrappleSyncPacket sync =
                new net.noiilive.hahueuh.network.GuiltywhipGrappleSyncPacket(
                        player.getUUID(),
                        target == null ? -1 : target.getId(),
                        point != null,
                        point == null ? 0.0 : point.x,
                        point == null ? 0.0 : point.y,
                        point == null ? 0.0 : point.z);
        boolean matchedRequest = target != null
                ? target.getId() == targetId
                : targetId < 0 && (point != null) == (blockPoint != null);
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other != player || !matchedRequest) {
                ModNetworking.sendToPlayer(other, sync);
            }
        }
    }

    private record Pull(int targetId, Vec3 point) {}

    private void clearGrappleFallOnGround(ServerPlayer player) {
        long stamp = player.getPersistentData().getLong(GRAPPLE_FALL_TAG);
        if (stamp > 0 && player.onGround()
                && player.level().getGameTime() - stamp > GRAPPLE_FALL_SETTLE) {
            player.getPersistentData().remove(GRAPPLE_FALL_TAG);
        }
    }

    private void applyPull(ServerPlayer player, Pull pull) {
        if (pull.point() != null) {
            Vec3 to = pull.point().subtract(player.position());
            double dist = to.length();
            if (dist < 1.0e-4) return;
            Vec3 launch = to.normalize()
                    .scale(Math.min(GRAPPLE_MAX_PULL, GRAPPLE_BASE_PULL + dist * GRAPPLE_PULL_PER_BLOCK))
                    .add(0.0, GRAPPLE_SELF_LIFT, 0.0);
            player.setDeltaMovement(player.getDeltaMovement().scale(0.2).add(launch));
            player.hurtMarked = true;
            player.resetFallDistance();
            player.getPersistentData().putLong(GRAPPLE_FALL_TAG, player.level().getGameTime());
            player.connection.send(
                    new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
            return;
        }

        net.minecraft.world.entity.Entity target = player.serverLevel().getEntity(pull.targetId());
        if (target == null || !target.isAlive()) return;
        Vec3 to = player.position().add(0.0, 0.2, 0.0).subtract(target.position());
        double dist = to.length();
        if (dist < 1.0e-4) return;
        Vec3 drag = to.normalize()
                .scale(Math.min(GRAPPLE_MAX_PULL, GRAPPLE_BASE_PULL + dist * GRAPPLE_PULL_PER_BLOCK))
                .add(0.0, GRAPPLE_TARGET_LIFT, 0.0);
        target.setDeltaMovement(target.getDeltaMovement().scale(0.2).add(drag));
        target.hurtMarked = true;
        target.resetFallDistance();
        grabGuards.put(player.getUUID(), new GrabGuard(target.getId(),
                player.level().getGameTime() + GRAB_GUARD_TICKS));
        if (target instanceof ServerPlayer pulled) {
            pulled.connection.send(
                    new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(pulled));
        }
        player.serverLevel().playSound(null, target.blockPosition(),
                SoundEvents.LEASH_KNOT_BREAK, SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    @SubscribeEvent
    public void onLivingFall(net.minecraftforge.event.entity.living.LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.getPersistentData().getLong(GRAPPLE_FALL_TAG) > 0) {
            entity.getPersistentData().remove(GRAPPLE_FALL_TAG);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onGrabbedDamage(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        GrabGuard guard = grabGuards.get(player.getUUID());
        if (guard == null) return;
        if (player.level().getGameTime() > guard.expiresAt()) {
            grabGuards.remove(player.getUUID());
            return;
        }
        net.minecraft.world.entity.Entity attacker = event.getSource().getEntity();
        if (attacker != null && attacker.getId() == guard.attackerId()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onAttackEntity(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        if (event.getEntity().getMainHandItem().getItem() instanceof GuiltywhipItem) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            clearGrappleFallOnGround(player);
            if (!(player.getMainHandItem().getItem() instanceof GuiltywhipItem)
                    || player.isSpectator()) {
                states.remove(uuid);
                pendingPulls.remove(uuid);
                continue;
            }

            Vec3 anchor = GuiltywhipPhysics.handAnchor(player);
            GuiltywhipPhysics.State state = states.computeIfAbsent(uuid,
                    k -> new GuiltywhipPhysics.State(anchor));
            GuiltywhipPhysics.step(state, anchor, player);

            if (state.grappleTicks == GuiltywhipPhysics.GRAPPLE_HOLD_TICKS) {
                Pull pull = pendingPulls.remove(uuid);
                if (pull != null) applyPull(player, pull);
            } else if (state.grappleTicks <= 0) {
                pendingPulls.remove(uuid);
            }

            if (state.crackTicks > 0) {
                sweep(player, state);
            }
        }
    }

    private void sweep(ServerPlayer player, GuiltywhipPhysics.State state) {
        ServerLevel level = player.serverLevel();
        for (int i = GuiltywhipPhysics.NODES - 1; i >= 1; i--) {
            AABB box = new AABB(state.prev[i], state.pos[i]).inflate(SEGMENT_HIT_RADIUS);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != player && e.isAlive() && !e.isSpectator()
                            && !state.hitThisCrack.contains(e.getId()))) {
                state.hitThisCrack.add(target.getId());
                float damage = GuiltywhipCombat.damageAt(player, target, i);
                if (target.hurt(player.damageSources().playerAttack(player), damage)) {
                    GuiltywhipCombat.applyHitEffects(player, target);
                    GuiltywhipCombat.damageWeapon(player);
                    Vec3 push = state.crackDir.scale(0.35).add(0.0, 0.12, 0.0);
                    target.push(push.x, push.y, push.z);
                    level.playSound(null, target.blockPosition(),
                            SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.7f, 1.4f);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        states.remove(event.getEntity().getUUID());
        pendingPulls.remove(event.getEntity().getUUID());
        grabGuards.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        states.clear();
        pendingPulls.clear();
        grabGuards.clear();
    }
}
