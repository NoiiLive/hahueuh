package net.noiilive.hahueuh;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FrozenObjectProjectile extends ThrowableProjectile implements ItemSupplier {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(FrozenObjectProjectile.class, EntityDataSerializers.ITEM_STACK);

    private static final double SPEED = 4.5;
    private static final float DAMAGE = 30.0f;
    private static final double SUBSTEP_LENGTH = 0.3;
    private static final int MAX_AGE_TICKS = 60;

    private final Set<UUID> hitEntities = new HashSet<>();
    private Vec3 spawnPos = Vec3.ZERO;

    public FrozenObjectProjectile(EntityType<? extends FrozenObjectProjectile> type, Level level) {
        super(type, level);
    }

    public FrozenObjectProjectile(Level level, LivingEntity shooter, ItemStack visual, float inaccuracy) {
        super(ModEntities.FROZEN_OBJECT_PROJECTILE.get(), shooter, level);
        setItem(visual);
        Vec3 look = shooter.getViewVector(1.0f);
        this.shoot(look.x, look.y, look.z, (float) SPEED, inaccuracy);
        this.spawnPos = this.position();
    }

    public void setItem(ItemStack stack) {
        this.getEntityData().set(DATA_ITEM, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    @Override
    public ItemStack getItem() {
        return this.getEntityData().get(DATA_ITEM);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ITEM, ItemStack.EMPTY);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity)
                && entity instanceof LivingEntity
                && entity.isAlive()
                && !entity.isSpectator()
                && entity != this.getOwner()
                && !hitEntities.contains(entity.getUUID());
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel level) {
            clearBreakableBlocksAlongPath(level);
            double maxRange = ConfigGreed.GREED_PROJECTILE_DISTANCE.getAsInt();
            if (this.tickCount > MAX_AGE_TICKS || this.position().distanceToSqr(spawnPos) > maxRange * maxRange) {
                this.discard();
                return;
            }
        } else if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        }
        super.tick();
    }

    private void clearBreakableBlocksAlongPath(ServerLevel level) {
        if (!level.getGameRules().getBoolean(ModGameRules.REZERO_BLOCK_DESTRUCTION)) return;

        Vec3 velocity = this.getDeltaMovement();
        double dist = velocity.length();
        if (dist < 1.0e-6) return;

        int steps = Math.max(1, (int) Math.ceil(dist / SUBSTEP_LENGTH));
        Vec3 step = velocity.scale(1.0 / steps);
        Vec3 pos = this.position();
        for (int i = 0; i < steps; i++) {
            pos = pos.add(step);
            BlockPos blockPos = BlockPos.containing(pos);
            BlockState state = level.getBlockState(blockPos);
            if (!state.isAir() && !(state.getBlock() instanceof LiquidBlock)
                    && !state.getCollisionShape(level, blockPos).isEmpty()
                    && state.getDestroySpeed(level, blockPos) >= 0) {
                level.sendParticles(ParticleTypes.EXPLOSION,
                        blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
                level.destroyBlock(blockPos, ModGameRules.rollDrops(level));
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        if (!hitEntities.add(target.getUUID())) return;
        target.hurt(this.damageSources().thrown(this, this.getOwner()), DAMAGE);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        this.discard();
    }

    @Override
    public boolean canUsePortal(boolean allowVehicles) {
        return false;
    }
}
