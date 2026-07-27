package net.noiilive.hahueuh;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class MinyaSpikeEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Boolean> DATA_SHARD =
            SynchedEntityData.defineId(MinyaSpikeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CHARGING =
            SynchedEntityData.defineId(MinyaSpikeEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double STAKE_SPEED = 0.75;
    private static final double SHARD_SPEED = 0.55;
    private static final double TURN_RATE = 0.3;
    private static final double EL_TURN_RATE = 0.6;
    private static final double STAKE_HOMING_RANGE = 12.0;
    private static final double MAX_TRAVEL_BLOCKS = 48.0;
    private static final int MAX_AGE_TICKS = 60;
    private static final int SHARD_MAX_AGE_TICKS = 40;
    private static final int SHARD_COUNT = 3;
    private static final double SHARD_SPAWN_MARGIN = 0.4;
    private static final double WALL_CLEARANCE_DOT = 0.35;

    private int homingTargetId = -1;
    private float damage = 6.0f;
    private boolean elMinya = false;

    private int windupRemaining = 0;
    private double slotSide, slotUp, slotBack;

    private int flightTicks = 0;
    private Vec3 launchOrigin;
    private boolean playLaunchSound = false;

    public MinyaSpikeEntity(EntityType<? extends MinyaSpikeEntity> type, Level level) {
        super(type, level);
    }

    public MinyaSpikeEntity(Level level, LivingEntity shooter) {
        super(ModEntities.MINYA_SPIKE.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SHARD, false);
        builder.define(DATA_CHARGING, false);
    }

    public boolean isShard() {
        return this.getEntityData().get(DATA_SHARD);
    }

    private void setShard(boolean shard) {
        this.getEntityData().set(DATA_SHARD, shard);
    }

    public boolean isCharging() {
        return this.getEntityData().get(DATA_CHARGING);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setHomingTarget(Entity target) {
        this.homingTargetId = target != null ? target.getId() : -1;
    }

    public void markLaunchSoundEmitter() {
        this.playLaunchSound = true;
    }

    public void setElMinya(boolean elMinya) {
        this.elMinya = elMinya;
    }

    public void beginCharge(LivingEntity caster, double side, double up, double back, int windupTicks) {
        this.slotSide = side;
        this.slotUp = up;
        this.slotBack = back;
        this.windupRemaining = Math.max(1, windupTicks);
        this.getEntityData().set(DATA_CHARGING, true);
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);
        repositionToSlot(caster);

        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.WITCH, this.getX(), this.getY(), this.getZ(), 12, 0.15, 0.15, 0.15, 0.0);
            level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 6, 0.1, 0.1, 0.1, 0.01);
        }
    }

    public void launch(Vec3 direction) {
        double speed = isShard() ? SHARD_SPEED : STAKE_SPEED;
        Vec3 vel = direction.normalize().scale(speed);
        this.setDeltaMovement(vel);
        this.getEntityData().set(DATA_CHARGING, false);
        this.setNoGravity(true);
        this.launchOrigin = this.position();
        this.flightTicks = 0;
        faceMovement(vel);

        if (playLaunchSound && this.level() instanceof ServerLevel level) {
            level.playSound(null, this.blockPosition(), ModSounds.MINYA_SHOOT.get(), SoundSource.PLAYERS, 1.1f, 1.0f);
        }
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
                && entity != this.getOwner();
    }

    @Override
    public void tick() {
        if (this.level() instanceof ServerLevel level) {
            if (isCharging()) {
                tickCharge(level);
                return;
            }
            flightTicks++;
            int maxAge = isShard() ? SHARD_MAX_AGE_TICKS : MAX_AGE_TICKS;
            if (flightTicks > maxAge) {
                onMiss(level);
                return;
            }
            if (launchOrigin != null && this.position().distanceToSqr(launchOrigin) > MAX_TRAVEL_BLOCKS * MAX_TRAVEL_BLOCKS) {
                shatterInPlace(level);
                return;
            }
            steerTowardTarget(level);
        } else {
            this.level().addParticle(ParticleTypes.WITCH, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        }
        super.tick();
    }

    private void tickCharge(ServerLevel level) {
        Entity owner = this.getOwner();
        LivingEntity caster = owner instanceof LivingEntity le ? le : null;
        if (caster == null || !caster.isAlive()) {
            launch(Vec3.directionFromRotation(this.getXRot(), this.getYRot()));
            return;
        }

        repositionToSlot(caster);
        if (--windupRemaining <= 0) {
            LivingEntity target = resolveTarget(level);
            Vec3 aim = target != null
                    ? target.getBoundingBox().getCenter().subtract(this.position())
                    : caster.getViewVector(1.0f);
            launch(aim);
        }
    }

    private void repositionToSlot(LivingEntity caster) {
        Vec3 look = caster.getViewVector(1.0f);
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0e-6) flat = new Vec3(0.0, 0.0, 1.0);
        flat = flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0.0, flat.x);

        Vec3 eye = caster.getEyePosition();
        Vec3 pos = eye.add(flat.scale(-slotBack)).add(right.scale(slotSide)).add(0.0, slotUp, 0.0);
        this.setPos(pos.x, pos.y, pos.z);
        this.setDeltaMovement(Vec3.ZERO);
        faceMovement(look);
    }

    private void faceMovement(Vec3 dir) {
        double horiz = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        this.setYRot((float) Math.toDegrees(Mth.atan2(-dir.x, dir.z)));
        this.setXRot((float) Math.toDegrees(-Mth.atan2(dir.y, horiz)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    private void steerTowardTarget(ServerLevel level) {
        LivingEntity target = resolveTarget(level);
        if (target == null) {
            double range = isShard() ? ConfigMagicYin.MINYA_SHARD_HOMING_RANGE.get() : STAKE_HOMING_RANGE;
            target = findNearest(level, range);
            if (target == null) return;
            this.homingTargetId = target.getId();
        }

        double speed = this.getDeltaMovement().length();
        if (speed < 1.0e-4) speed = isShard() ? SHARD_SPEED : STAKE_SPEED;

        double turn = elMinya ? EL_TURN_RATE : TURN_RATE;
        Vec3 desired = target.getBoundingBox().getCenter().subtract(this.position()).normalize();
        Vec3 current = this.getDeltaMovement().normalize();
        Vec3 steered = current.add(desired.subtract(current).scale(turn)).normalize().scale(speed);
        this.setDeltaMovement(steered);
        faceMovement(steered);
    }

    private LivingEntity resolveTarget(ServerLevel level) {
        if (homingTargetId < 0) return null;
        Entity e = level.getEntity(homingTargetId);
        if (e instanceof LivingEntity living && living.isAlive() && !living.isSpectator()) return living;
        this.homingTargetId = -1;
        return null;
    }

    private LivingEntity findNearest(ServerLevel level, double range) {
        AABB box = this.getBoundingBox().inflate(range);
        LivingEntity best = null;
        double bestSqr = range * range;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != this.getOwner() && e.isAlive() && !e.isSpectator())) {
            double d = candidate.distanceToSqr(this);
            if (d <= bestSqr) {
                bestSqr = d;
                best = candidate;
            }
        }
        return best;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) return;
        if (!(result.getEntity() instanceof LivingEntity target)) return;

        Entity owner = this.getOwner();
        target.hurt(ModDamageTypes.minya(this.level(), this, owner), this.damage);
        LivingEntity caster = owner instanceof LivingEntity le ? le : null;
        HahUeuh.CRYSTALLIZE.crystallize(target, caster, ConfigMagicYin.MINYA_CRYSTALLIZE_SECONDS.getAsInt() * 20);

        if (this.level() instanceof ServerLevel level) {
            if (elMinya && !isShard()) {
                HahUeuh.EL_MINYA_CHAIN.recordHit(level, target, caster);
            }
            level.playSound(null, this.blockPosition(), ModSounds.MINYA_EXPLODE.get(),
                    SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level() instanceof ServerLevel level) {
            onProjectileEnd(level, result);
        }
        this.discard();
    }

    private void onMiss(ServerLevel level) {
        onProjectileEnd(level, null);
        this.discard();
    }

    private void onProjectileEnd(ServerLevel level, BlockHitResult blockHit) {
        if (isShard()) {
            Vec3 pos = blockHit != null ? blockHit.getLocation() : this.position();
            level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 10, 0.15, 0.15, 0.15, 0.05);
            level.playSound(null, BlockPos.containing(pos), ModSounds.MINYA_EXPLODE.get(), SoundSource.PLAYERS, 0.7f, 1.3f);
        } else {
            fragment(level, blockHit);
        }
    }

    private void shatterInPlace(ServerLevel level) {
        level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 12, 0.2, 0.2, 0.2, 0.1);
        level.playSound(null, this.blockPosition(), ModSounds.MINYA_EXPLODE.get(), SoundSource.PLAYERS, 0.5f, 1.3f);
        this.discard();
    }

    private void fragment(ServerLevel level, BlockHitResult blockHit) {
        if (isShard()) return;

        float shardDamage = (float) (this.damage * ConfigMagicYin.MINYA_SHARD_DAMAGE_PERCENT.get() / 100.0);
        Entity owner = this.getOwner();
        LivingEntity shooter = owner instanceof LivingEntity le ? le : null;
        Vec3 base = this.getDeltaMovement().lengthSqr() > 1.0e-6
                ? this.getDeltaMovement().normalize()
                : Vec3.directionFromRotation(this.getXRot(), this.getYRot());

        Vec3 spawnPos = this.position();
        Vec3 pushNormal = Vec3.ZERO;
        if (blockHit != null) {
            Direction face = blockHit.getDirection();
            pushNormal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
            spawnPos = blockHit.getLocation().add(pushNormal.scale(SHARD_SPAWN_MARGIN));
        }

        for (int i = 0; i < SHARD_COUNT; i++) {
            MinyaSpikeEntity shard = shooter != null
                    ? new MinyaSpikeEntity(level, shooter)
                    : new MinyaSpikeEntity(ModEntities.MINYA_SPIKE.get(), level);
            shard.setShard(true);
            shard.setElMinya(this.elMinya);
            shard.setDamage(shardDamage);
            shard.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

            double spread = (i - 1) * 0.6;
            Vec3 dir = base.add(spread, 0.15, spread * 0.5);
            LivingEntity near = shard.findNearest(level, ConfigMagicYin.MINYA_SHARD_HOMING_RANGE.get());
            if (near != null) {
                shard.setHomingTarget(near);
                dir = near.getBoundingBox().getCenter().subtract(spawnPos);
            }
            if (pushNormal.lengthSqr() > 1.0e-6) {
                Vec3 normDir = dir.normalize();
                double inward = normDir.dot(pushNormal);
                if (inward < WALL_CLEARANCE_DOT) {
                    dir = normDir.add(pushNormal.scale(WALL_CLEARANCE_DOT - inward)).normalize();
                }
            }
            shard.launch(dir);
            level.addFreshEntity(shard);
        }

        level.playSound(null, this.blockPosition(), ModSounds.MINYA_EXPLODE.get(), SoundSource.PLAYERS, 0.9f, 1.1f);
    }

    @Override
    public boolean canUsePortal(boolean allowVehicles) {
        return false;
    }
}
