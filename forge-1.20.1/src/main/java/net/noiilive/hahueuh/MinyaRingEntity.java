package net.noiilive.hahueuh;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class MinyaRingEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_RADIUS =
            SynchedEntityData.defineId(MinyaRingEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ANCHOR_DROP =
            SynchedEntityData.defineId(MinyaRingEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_BEAMING =
            SynchedEntityData.defineId(MinyaRingEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int FORM_TICKS = 5;
    private static final int BEAM_TICKS = 30;
    private static final int PARTICLE_COLUMN_HEIGHT = 40;
    private static final double MIN_RING_RADIUS = 0.5;

    private int boundTargetId = -1;
    private UUID casterUuid;
    private int bindTicksRemaining;
    private int beamTicksRemaining;
    private float damage = 40.0f;
    private boolean executed;

    public MinyaRingEntity(EntityType<? extends MinyaRingEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void bind(LivingEntity target, LivingEntity caster, int bindTicks, float damage, double widthScale) {
        this.boundTargetId = target.getId();
        this.casterUuid = caster != null ? caster.getUUID() : null;
        this.bindTicksRemaining = Math.max(1, bindTicks);
        this.damage = damage;

        double halfHeight = target.getBbHeight() * 0.5;
        double radius = Math.max(MIN_RING_RADIUS, target.getBbWidth() * widthScale * 0.5);
        this.getEntityData().set(DATA_RADIUS, (float) radius);
        this.getEntityData().set(DATA_ANCHOR_DROP, (float) halfHeight);
        this.setPos(target.getX(), target.getY() + halfHeight, target.getZ());

        target.addEffect(new MobEffectInstance(ModEffects.LEASHED.get(), this.bindTicksRemaining, 0, false, true, true));
    }

    public float radius() {
        return this.getEntityData().get(DATA_RADIUS);
    }

    public float anchorDrop() {
        return this.getEntityData().get(DATA_ANCHOR_DROP);
    }

    public boolean isBeaming() {
        return this.getEntityData().get(DATA_BEAMING);
    }

    public float renderScale(float partialTicks) {
        float age = tickCount + partialTicks;
        return Mth.clamp(age / FORM_TICKS, 0f, 1f);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;

        if (executed) {
            if (--beamTicksRemaining <= 0) discard();
            return;
        }

        LivingEntity target = resolveTarget(server);
        if (target == null || !target.isAlive()) {
            discard();
            return;
        }

        holdLeashed(target);

        if (--bindTicksRemaining <= 0) {
            execute(server, target);
        }
    }

    private void holdLeashed(LivingEntity target) {
        Vec3 vel = target.getDeltaMovement();
        target.setDeltaMovement(0.0, Math.min(vel.y, 0.0), 0.0);
        target.hasImpulse = false;

        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        if (dx * dx + dz * dz > 0.0025) {
            if (target instanceof net.minecraft.server.level.ServerPlayer sp) {
                sp.connection.teleport(this.getX(), target.getY(), this.getZ(), sp.getYRot(), sp.getXRot());
            } else {
                target.setPos(this.getX(), target.getY(), this.getZ());
            }
        }
    }

    private void execute(ServerLevel server, LivingEntity target) {
        executed = true;
        beamTicksRemaining = BEAM_TICKS;
        this.getEntityData().set(DATA_BEAMING, true);
        target.removeEffect(ModEffects.LEASHED.get());

        Vec3 centre = this.position();
        LivingEntity caster = resolveCaster(server);
        var source = caster != null ? damageSources().indirectMagic(caster, this) : damageSources().magic();
        target.hurt(source, damage);

        for (int i = 0; i < PARTICLE_COLUMN_HEIGHT; i++) {
            double y = centre.y + i;
            server.sendParticles(ParticleTypes.PORTAL, centre.x, y, centre.z, 3, 0.35, 0.05, 0.35, 0.02);
            if (i % 2 == 0) {
                server.sendParticles(ParticleTypes.END_ROD, centre.x, y, centre.z, 1, 0.1, 0.05, 0.1, 0.01);
            }
        }
        server.sendParticles(ParticleTypes.EXPLOSION, centre.x, centre.y + 0.1, centre.z, 1, 0.0, 0.0, 0.0, 0.0);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, centre.x, centre.y + 0.5, centre.z, 40, 0.6, 0.6, 0.6, 0.1);
        server.playSound(null, blockPosition(), ModSounds.MINYA_EXPLODE.get(), SoundSource.PLAYERS, 1.4f, 0.6f);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!executed && level() instanceof ServerLevel server) {
            LivingEntity target = resolveTarget(server);
            if (target != null) target.removeEffect(ModEffects.LEASHED.get());
        }
        super.remove(reason);
    }

    private LivingEntity resolveTarget(ServerLevel server) {
        if (boundTargetId < 0) return null;
        Entity e = server.getEntity(boundTargetId);
        return e instanceof LivingEntity living ? living : null;
    }

    private LivingEntity resolveCaster(ServerLevel server) {
        if (casterUuid == null) return null;
        return server.getEntity(casterUuid) instanceof LivingEntity living ? living : null;
    }

    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling()
                .inflate(radius() + 2.0)
                .expandTowards(0.0, 320.0, 0.0);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_RADIUS, 1.0f);
        this.entityData.define(DATA_ANCHOR_DROP, 1.0f);
        this.entityData.define(DATA_BEAMING, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Caster")) casterUuid = tag.getUUID("Caster");
        boundTargetId = tag.getInt("BoundTarget");
        bindTicksRemaining = tag.getInt("BindTicksRemaining");
        damage = tag.getFloat("Damage");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (casterUuid != null) tag.putUUID("Caster", casterUuid);
        tag.putInt("BoundTarget", boundTargetId);
        tag.putInt("BindTicksRemaining", bindTicksRemaining);
        tag.putFloat("Damage", damage);
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }
}
