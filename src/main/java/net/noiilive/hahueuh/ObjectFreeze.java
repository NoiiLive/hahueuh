package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.AbilityCooldownPayload;
import net.noiilive.hahueuh.network.GreedVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ObjectFreeze {
    private static final double TARGET_RANGE = 5.0;
    private static final double LAUNCH_SPEED = 2.0;
    private static final double LAUNCH_LIFT = 0.3;
    private static final int LAUNCH_DURATION_TICKS = 15;
    private static final double LAUNCH_SUBSTEP_LENGTH = 0.3;
    private static final float LAUNCH_DAMAGE_PER_BLOCK = 1.0f;
    private static final float SHOTGUN_INACCURACY = 10.0f;

    private record LaunchState(Vec3 velocity, int remainingTicks) {}

    private final Map<UUID, Integer> cooldownUntilTick = new ConcurrentHashMap<>();
    private final Map<UUID, LaunchState> launches = new ConcurrentHashMap<>();
    private MinecraftServer server;

    public void activate(ServerPlayer player) {
        if (server == null) return;
        UUID uuid = player.getUUID();

        if (!HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().canUseGreed(uuid)
                || HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().getGreedVariant(uuid) != GreedVariant.LIONSHEART) {
            player.displayClientMessage(Component.translatable("hahueuh.message.no_greed_authority")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (!HahUeuh.LIONS_HEART.isActive(uuid)) {
            player.displayClientMessage(Component.translatable("hahueuh.message.object_freeze_needs_lions_heart")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        int remainingCooldown = player.isCreative() ? 0 : cooldownRemainingTicks(uuid);
        if (remainingCooldown > 0) {
            int seconds = (int) Math.ceil(remainingCooldown / 20.0);
            player.displayClientMessage(Component.translatable("hahueuh.message.object_freeze_cooldown", seconds)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        LivingEntity target = raycastTarget(player);
        if (target != null) {
            launchEntity(player, target);
            player.displayClientMessage(Component.translatable("hahueuh.message.object_freeze_launched")
                    .withStyle(ChatFormatting.GOLD), true);
        } else {
            throwHeldItem(player);
            player.displayClientMessage(Component.translatable("hahueuh.message.object_freeze_thrown")
                    .withStyle(ChatFormatting.GOLD), true);
        }

        player.swing(InteractionHand.MAIN_HAND);
        if (!player.isCreative()) {
            int cooldownSeconds = ConfigGreed.OBJECT_FREEZE_COOLDOWN_SECONDS.getAsInt();
            if (cooldownSeconds > 0) {
                cooldownUntilTick.put(uuid, server.getTickCount() + HahUeuh.GREED_COMPAT.scaleCooldownTicks(uuid, cooldownSeconds * 20));
                PacketDistributor.sendToPlayer(player,
                        new AbilityCooldownPayload(HahUeuhAbilities.OBJECT_FREEZE_ABILITY, HahUeuh.GREED_COMPAT.scaleCooldownTicks(uuid, cooldownSeconds * 20)));
            }
        }
    }

    private LivingEntity raycastTarget(LivingEntity caster) {
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(caster,
                e -> e != caster && e.isAlive() && !e.isSpectator() && e instanceof LivingEntity, TARGET_RANGE);
        if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private void launchEntity(LivingEntity caster, LivingEntity target) {
        Vec3 direction = caster.getViewVector(1.0f);
        Vec3 velocity = direction.scale(LAUNCH_SPEED).add(0.0, LAUNCH_LIFT, 0.0);

        target.setNoGravity(true);
        target.setOnGround(false);
        target.fallDistance = 0;
        target.setDeltaMovement(velocity);
        if (target instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
        }
        launches.put(target.getUUID(), new LaunchState(velocity, LAUNCH_DURATION_TICKS));

        caster.level().playSound(null, caster.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (server == null || launches.isEmpty()) return;
        for (Map.Entry<UUID, LaunchState> entry : new ArrayList<>(launches.entrySet())) {
            UUID uuid = entry.getKey();
            Entity target = findEntity(uuid);
            if (target == null || !target.isAlive()) {
                launches.remove(uuid);
                continue;
            }

            boolean stoppedByBlock = advanceLaunch(target, entry.getValue().velocity());
            int remaining = entry.getValue().remainingTicks() - 1;
            if (stoppedByBlock || remaining <= 0) {
                endLaunch(target);
                launches.remove(uuid);
            } else {
                launches.put(uuid, new LaunchState(entry.getValue().velocity(), remaining));
            }
        }
    }

    private boolean advanceLaunch(Entity target, Vec3 velocity) {
        if (!(target.level() instanceof ServerLevel level)) return true;
        boolean destructionAllowed = level.getGameRules().getBoolean(ModGameRules.REZERO_BLOCK_DESTRUCTION);

        Vec3 start = target.position();
        double dist = velocity.length();
        int steps = Math.max(1, (int) Math.ceil(dist / LAUNCH_SUBSTEP_LENGTH));
        Vec3 step = velocity.scale(1.0 / steps);
        AABB baseBox = target.getBoundingBox();
        Vec3 pos = start;
        boolean stoppedByBlock = false;
        int blocksDestroyed = 0;

        for (int i = 0; i < steps; i++) {
            Vec3 next = pos.add(step);
            AABB nextBox = baseBox.move(next.subtract(start));
            BlockPos min = BlockPos.containing(nextBox.minX, nextBox.minY, nextBox.minZ);
            BlockPos max = BlockPos.containing(nextBox.maxX, nextBox.maxY, nextBox.maxZ);
            boolean blockedHere = false;
            for (BlockPos pos2 : BlockPos.betweenClosed(min, max)) {
                BlockState state = level.getBlockState(pos2);
                if (state.isAir() || state.getBlock() instanceof LiquidBlock) continue;
                if (state.getCollisionShape(level, pos2).isEmpty()) continue;
                if (destructionAllowed && state.getDestroySpeed(level, pos2) >= 0) {
                    level.sendParticles(ParticleTypes.EXPLOSION,
                            pos2.getX() + 0.5, pos2.getY() + 0.5, pos2.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
                    level.destroyBlock(pos2.immutable(), ModGameRules.rollDrops(level));
                    blocksDestroyed++;
                } else {
                    blockedHere = true;
                }
            }
            if (blockedHere) {
                stoppedByBlock = true;
                break;
            }
            pos = next;
        }

        if (target instanceof ServerPlayer sp) {
            sp.connection.teleport(pos.x, pos.y, pos.z, sp.getYRot(), sp.getXRot());
        } else {
            target.setPos(pos.x, pos.y, pos.z);
        }
        target.setDeltaMovement(velocity);
        target.setOnGround(false);

        if (blocksDestroyed > 0) {
            target.hurt(target.damageSources().flyIntoWall(), blocksDestroyed * LAUNCH_DAMAGE_PER_BLOCK);
        }

        return stoppedByBlock;
    }

    private void endLaunch(Entity target) {
        target.setNoGravity(false);
        target.setDeltaMovement(Vec3.ZERO);
        if (target instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
        }
    }

    public void forceResetOnRollback(ServerPlayer player) {
        if (launches.remove(player.getUUID()) != null) {
            endLaunch(player);
        }
    }

    private Entity findEntity(UUID id) {
        if (server == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) return e;
        }
        return null;
    }

    private void throwHeldItem(LivingEntity caster) {
        ItemStack held = caster.getMainHandItem();
        boolean shotgun = !held.isEmpty() && caster instanceof Player p && p.isShiftKeyDown();

        int count;
        ItemStack visual;
        if (held.isEmpty()) {
            count = 1;
            visual = ItemStack.EMPTY;
        } else if (shotgun) {
            count = held.getCount();
            visual = held.copyWithCount(1);
            caster.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        } else {
            count = 1;
            visual = held.copyWithCount(1);
            held.shrink(1);
        }

        float inaccuracy = count > 1 ? SHOTGUN_INACCURACY : 0.0f;
        for (int i = 0; i < count; i++) {
            FrozenObjectProjectile projectile = new FrozenObjectProjectile(caster.level(), caster, visual, inaccuracy);
            caster.level().addFreshEntity(projectile);
        }
    }

    public void activateMob(Mob mob) {
        if (server == null) return;
        UUID uuid = mob.getUUID();
        if (cooldownRemainingTicks(uuid) > 0) return;

        LivingEntity target = raycastTarget(mob);
        if (target != null) {
            launchEntity(mob, target);
        } else {
            throwHeldItem(mob);
        }

        int cooldownSeconds = ConfigGreed.OBJECT_FREEZE_COOLDOWN_SECONDS.getAsInt();
        if (cooldownSeconds > 0) {
            cooldownUntilTick.put(uuid, server.getTickCount() + HahUeuh.GREED_COMPAT.scaleCooldownTicks(uuid, cooldownSeconds * 20));
        }
    }

    private int cooldownRemainingTicks(UUID uuid) {
        Integer until = cooldownUntilTick.get(uuid);
        if (until == null || server == null) return 0;
        return Math.max(0, until - server.getTickCount());
    }

    public Map<UUID, Integer> captureCooldownRemaining() {
        Map<UUID, Integer> result = new HashMap<>();
        if (server == null) return result;
        int tick = server.getTickCount();
        cooldownUntilTick.forEach((uuid, until) -> {
            int remaining = until - tick;
            if (remaining > 0) result.put(uuid, remaining);
        });
        return result;
    }

    public void restoreCooldownRemaining(Map<UUID, Integer> remainingByUuid) {
        if (server == null) return;
        cooldownUntilTick.clear();
        int tick = server.getTickCount();
        remainingByUuid.forEach((uuid, remaining) -> cooldownUntilTick.put(uuid, tick + remaining));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int remaining = remainingByUuid.getOrDefault(player.getUUID(), 0);
            PacketDistributor.sendToPlayer(player, new AbilityCooldownPayload(HahUeuhAbilities.OBJECT_FREEZE_ABILITY, remaining));
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.server = event.getServer();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        cooldownUntilTick.clear();
        launches.clear();
        this.server = null;
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            launches.remove(player.getUUID());
        }
    }
}
