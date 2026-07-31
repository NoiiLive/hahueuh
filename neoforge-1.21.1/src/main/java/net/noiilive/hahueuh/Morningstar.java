package net.noiilive.hahueuh;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.noiilive.hahueuh.network.MorningstarSwingSyncPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Morningstar {

    private final Map<UUID, MorningstarPhysics.State> states = new ConcurrentHashMap<>();

    public void handleSwing(ServerPlayer player, boolean spin) {
        if (!(player.getMainHandItem().getItem() instanceof MorningstarItem)) return;
        MorningstarPhysics.State state = states.get(player.getUUID());
        if (state == null) return;

        if (spin && MorningstarPhysics.tryQueueSpin(state)) {
            player.serverLevel().playSound(null, player.blockPosition(),
                    SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.7f, 1.3f);
            relaySwing(player, true);
            return;
        }
        if (!MorningstarPhysics.canSwing(state)) return;

        MorningstarPhysics.applySwing(state, player, spin);
        player.serverLevel().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 0.7f);
        player.serverLevel().playSound(null, player.blockPosition(),
                SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.8f, 1.1f);

        relaySwing(player, spin);
    }

    private void relaySwing(ServerPlayer player, boolean spin) {
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other != player) {
                PacketDistributor.sendToPlayer(other, new MorningstarSwingSyncPayload(player.getUUID(), spin));
            }
        }
    }

    @SubscribeEvent
    public void onAttackEntity(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        if (event.getEntity().getMainHandItem().getItem() instanceof MorningstarItem) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            if (!(player.getMainHandItem().getItem() instanceof MorningstarItem)
                    || player.isSpectator()) {
                states.remove(uuid);
                continue;
            }

            Vec3 anchor = MorningstarPhysics.handAnchor(player);
            MorningstarPhysics.State state = states.computeIfAbsent(uuid,
                    k -> new MorningstarPhysics.State(anchor.add(0.0, -1.0, 0.0)));
            MorningstarPhysics.step(state, anchor, player);
            playChainSounds(player, state);

            if (state.swingTicks > 0) {
                sweepHit(player, state);
            }
        }
    }

    private void playChainSounds(ServerPlayer player, MorningstarPhysics.State state) {
        ServerLevel level = player.serverLevel();
        if (state.groundImpact > 0.4) {
            level.playSound(null, state.pos.x, state.pos.y, state.pos.z,
                    SoundEvents.CHAIN_FALL, SoundSource.PLAYERS,
                    (float) Math.min(1.0, 0.4 + state.groundImpact * 0.3), 0.7f);
            return;
        }
        double moved = state.pos.distanceTo(state.prevPos);
        boolean active = state.onGround || state.swingTicks > 0 || state.pendingThrow != null;
        if (moved > 0.2 && active && player.tickCount % 3 == 0) {
            level.playSound(null, state.pos.x, state.pos.y, state.pos.z,
                    SoundEvents.CHAIN_STEP, SoundSource.PLAYERS,
                    (float) Math.min(0.6, 0.12 + moved * 0.18),
                    0.9f + level.random.nextFloat() * 0.3f);
        }
    }

    private void sweepHit(ServerPlayer player, MorningstarPhysics.State state) {
        ServerLevel level = player.serverLevel();
        if (state.spinning) {
            Vec3 anchor = MorningstarPhysics.handAnchor(player);
            for (int i = 1; i <= 5; i++) {
                double f = i / 5.0;
                Vec3 p0 = anchor.add(state.prevPos.subtract(anchor).scale(f));
                Vec3 p1 = anchor.add(state.pos.subtract(anchor).scale(f));
                AABB chainSweep = new AABB(p0, p1).inflate(0.4);
                boolean head = i == 5;
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, chainSweep,
                        e -> e != player && e.isAlive() && !e.isSpectator()
                                && !state.hitThisSwing.contains(e.getId()))) {
                    state.hitThisSwing.add(target.getId());
                    net.minecraft.world.damagesource.DamageSource src =
                            player.damageSources().playerAttack(player);
                    float damage = head
                            ? MorningstarCombat.spinHeadDamage(player, target, state, src)
                            : MorningstarCombat.chainDamage(player);
                    if (target.hurt(src, damage)) {
                        double push = head ? 0.5 : 0.3;
                        Vec3 kb = state.vel.lengthSqr() > 1.0e-4
                                ? state.vel.normalize().scale(push).add(0.0, 0.15, 0.0)
                                : new Vec3(0.0, 0.15, 0.0);
                        target.push(kb.x, kb.y, kb.z);
                        if (head) MorningstarCombat.applyHeadEffects(player, target);
                        MorningstarCombat.damageWeapon(player);
                        level.playSound(null, target.blockPosition(),
                                head ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                                SoundSource.PLAYERS, 0.8f, 1.1f);
                    }
                }
            }
            return;
        }
        AABB sweep = new AABB(state.prevPos, state.pos).inflate(MorningstarPhysics.HEAD_RADIUS + 0.3);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep,
                e -> e != player && e.isAlive() && !e.isSpectator()
                        && !state.hitThisSwing.contains(e.getId()))) {
            state.hitThisSwing.add(target.getId());
            double reach = state.pos.distanceTo(MorningstarPhysics.handAnchor(player));
            net.minecraft.world.damagesource.DamageSource source = player.damageSources().playerAttack(player);
            float damage = MorningstarCombat.headDamage(player, target, reach, source);
            if (target.hurt(source, damage)) {
                Vec3 kb = state.vel.lengthSqr() > 1.0e-4
                        ? state.vel.normalize().scale(0.5).add(0.0, 0.25, 0.0)
                        : new Vec3(0.0, 0.25, 0.0);
                target.push(kb.x, kb.y, kb.z);
                MorningstarCombat.applyHeadEffects(player, target);
                MorningstarCombat.damageWeapon(player);
                level.playSound(null, target.blockPosition(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.8f);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        states.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        states.clear();
    }
}
