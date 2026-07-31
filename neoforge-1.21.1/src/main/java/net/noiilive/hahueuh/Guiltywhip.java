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
import net.noiilive.hahueuh.network.GuiltywhipCrackSyncPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Guiltywhip {

    private static final double SEGMENT_HIT_RADIUS = 0.35;

    private final Map<UUID, GuiltywhipPhysics.State> states = new ConcurrentHashMap<>();

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
                PacketDistributor.sendToPlayer(other, new GuiltywhipCrackSyncPayload(player.getUUID(), sweep));
            }
        }
    }

    @SubscribeEvent
    public void onAttackEntity(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        if (event.getEntity().getMainHandItem().getItem() instanceof GuiltywhipItem) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            if (!(player.getMainHandItem().getItem() instanceof GuiltywhipItem)
                    || player.isSpectator()) {
                states.remove(uuid);
                continue;
            }

            Vec3 anchor = GuiltywhipPhysics.handAnchor(player);
            GuiltywhipPhysics.State state = states.computeIfAbsent(uuid,
                    k -> new GuiltywhipPhysics.State(anchor));
            GuiltywhipPhysics.step(state, anchor, player);

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
                net.minecraft.world.damagesource.DamageSource src =
                        player.damageSources().playerAttack(player);
                float damage = GuiltywhipCombat.damageAt(player, target, i, src);
                if (target.hurt(src, damage)) {
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
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        states.clear();
    }
}
