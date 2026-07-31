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
                ModNetworking.sendToPlayer(other, new GuiltywhipCrackSyncPacket(player.getUUID(), sweep));
            }
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
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        states.clear();
    }
}
