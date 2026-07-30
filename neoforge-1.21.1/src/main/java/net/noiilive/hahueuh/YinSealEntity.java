package net.noiilive.hahueuh;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class YinSealEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(YinSealEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_UNBREAKABLE =
            SynchedEntityData.defineId(YinSealEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_FORM_TICKS =
            SynchedEntityData.defineId(YinSealEntity.class, EntityDataSerializers.INT);

    private static final double SEAL_SCALE = 1.25;
    private static final double LIFT_HEIGHT = 3.0;
    private static final int RISE_TICKS = 20;
    private static final int EXPAND_TICKS = 6;
    private static final double PARTICLE_RADIUS_FACTOR = 0.8;

    private UUID sealedUuid;
    private UUID casterUuid;
    private float health;
    private int ticksRemaining;

    public YinSealEntity(EntityType<? extends YinSealEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SCALE, 2.0f);
        builder.define(DATA_UNBREAKABLE, false);
        builder.define(DATA_FORM_TICKS, 0);
    }

    private static int riseTicks() {
        return RISE_TICKS;
    }

    private static int expandTicks() {
        return EXPAND_TICKS;
    }

    private double particleRadius() {
        return scale() * PARTICLE_RADIUS_FACTOR;
    }

    public boolean fullyFormed() {
        return entityData.get(DATA_FORM_TICKS) >= riseTicks() + expandTicks();
    }

    public float renderScale(float partialTicks) {
        int form = entityData.get(DATA_FORM_TICKS);
        int rise = riseTicks();
        int expand = expandTicks();
        if (form < rise) return 0.0f;
        if (form >= rise + expand) return scale();

        float progress = Math.min(1.0f, (form - rise + partialTicks) / expand);
        return scale() * progress;
    }

    public void bind(LivingEntity target, UUID caster) {
        this.sealedUuid = target.getUUID();
        this.casterUuid = caster;
        this.health = ConfigMagicYin.OL_SHAMAK_SEAL_HEALTH.get().floatValue();
        this.ticksRemaining = ConfigMagicYin.OL_SHAMAK_DURATION_SECONDS.get() * 20;

        float scale = (float) (target.getBbHeight() * SEAL_SCALE);
        this.entityData.set(DATA_SCALE, Math.max(1.0f, scale));
        this.entityData.set(DATA_UNBREAKABLE, OlShamak.hasWitchFactor(target));
        setPos(target.getX(), centredY(target), target.getZ());
    }

    public UUID sealedUuid() {
        return sealedUuid;
    }

    public UUID casterUuid() {
        return casterUuid;
    }

    public float scale() {
        return this.entityData.get(DATA_SCALE);
    }

    public boolean unbreakable() {
        return this.entityData.get(DATA_UNBREAKABLE);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || amount <= 0.0f) return false;

        if (unbreakable() && !isReidStrike(source)) {
            playSound(SoundEvents.SHIELD_BLOCK, 0.8f, 0.4f);
            return false;
        }

        health -= amount;
        playSound(SoundEvents.GLASS_BREAK, 0.5f, 1.6f);
        if (level() instanceof ServerLevel server) {
            double r = particleRadius() * 0.6;
            server.sendParticles(ParticleTypes.SQUID_INK, getX(), centreY(), getZ(), 20, r, r, r, 0.05);
        }
        if (health <= 0.0f) {
            HahUeuh.OL_SHAMAK.release(this, true);
        }
        return true;
    }

    private static boolean isReidStrike(DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity attacker)) return false;
        ItemStack held = attacker.getMainHandItem();
        return !held.isEmpty() && held.is(ModItems.DRAGON_SWORD_REID.get());
    }

    private double centredY(LivingEntity sealed) {
        return sealed.getY() + sealed.getBbHeight() * 0.5 - getBbHeight() * 0.5;
    }

    private double centreY() {
        return getY() + getBbHeight() * 0.5;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (fullyFormed()) spawnAmbientParticles();
            return;
        }

        HahUeuh.OL_SHAMAK.ensureTracked(this);
        LivingEntity sealed = HahUeuh.OL_SHAMAK.resolveSealed(this);
        if (sealed == null || !sealed.isAlive()) {
            HahUeuh.OL_SHAMAK.release(this, false);
            return;
        }

        int form = entityData.get(DATA_FORM_TICKS);
        int rise = riseTicks();
        int expand = expandTicks();

        if (form < rise) {
            raise(sealed, rise);
        } else if (form == rise + expand) {
            level().playSound(null, blockPosition(), SoundEvents.END_PORTAL_FRAME_FILL,
                    SoundSource.PLAYERS, 1.2f, 0.8f);
            if (level() instanceof ServerLevel server) {
                double radius = particleRadius();
                server.sendParticles(ParticleTypes.SQUID_INK, getX(), centreY(), getZ(),
                        80, radius * 0.5, radius * 0.5, radius * 0.5, 0.1);
                server.sendParticles(ParticleTypes.PORTAL, getX(), centreY(), getZ(),
                        60, radius * 0.4, radius * 0.4, radius * 0.4, 0.5);
                server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), centreY(), getZ(),
                        40, radius * 0.4, radius * 0.3, radius * 0.4, 0.03);
            }
        }
        if (form <= rise + expand) entityData.set(DATA_FORM_TICKS, form + 1);

        setPos(sealed.getX(), centredY(sealed), sealed.getZ());
        holdStill(sealed);

        if (form < rise + expand) return;
        if (--ticksRemaining <= 0) {
            HahUeuh.OL_SHAMAK.release(this, true);
        }
    }

    private void raise(LivingEntity sealed, int rise) {
        double perTick = LIFT_HEIGHT / rise;
        if (perTick <= 0.0) return;
        if (!level().noCollision(sealed, sealed.getBoundingBox().move(0.0, perTick, 0.0))) return;

        sealed.teleportTo(sealed.getX(), sealed.getY() + perTick, sealed.getZ());
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.PORTAL,
                    sealed.getX(), sealed.getY() + sealed.getBbHeight() * 0.5, sealed.getZ(),
                    3, 0.3, 0.4, 0.3, 0.3);
        }
    }

    private static void holdStill(LivingEntity sealed) {
        sealed.setDeltaMovement(Vec3.ZERO);
        sealed.hurtMarked = true;
        sealed.fallDistance = 0f;
        if (sealed instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
        if (sealed instanceof ServerPlayer player) {
            player.connection.send(
                    new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
        }
    }

    private void spawnAmbientParticles() {
        double radius = particleRadius();
        for (int i = 0; i < 3; i++) {
            double theta = random.nextDouble() * Math.PI * 2.0;
            double phi = Math.acos(2.0 * random.nextDouble() - 1.0);
            double x = getX() + radius * Math.sin(phi) * Math.cos(theta);
            double y = centreY() + radius * Math.cos(phi);
            double z = getZ() + radius * Math.sin(phi) * Math.sin(theta);
            level().addParticle(ParticleTypes.SQUID_INK, x, y, z, 0.0, 0.0, 0.0);
            level().addParticle(ParticleTypes.PORTAL, x, y, z, 0.0, 0.0, 0.0);
            level().addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.01, 0.0);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Sealed")) sealedUuid = tag.getUUID("Sealed");
        if (tag.hasUUID("Caster")) casterUuid = tag.getUUID("Caster");
        health = tag.getFloat("Health");
        ticksRemaining = tag.getInt("TicksRemaining");
        entityData.set(DATA_SCALE, tag.contains("Scale") ? tag.getFloat("Scale") : 2.0f);
        entityData.set(DATA_UNBREAKABLE, tag.getBoolean("Unbreakable"));
        entityData.set(DATA_FORM_TICKS, tag.getInt("FormTicks"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (sealedUuid != null) tag.putUUID("Sealed", sealedUuid);
        if (casterUuid != null) tag.putUUID("Caster", casterUuid);
        tag.putFloat("Health", health);
        tag.putInt("TicksRemaining", ticksRemaining);
        tag.putFloat("Scale", scale());
        tag.putBoolean("Unbreakable", unbreakable());
        tag.putInt("FormTicks", entityData.get(DATA_FORM_TICKS));
    }

    public void burst() {
        if (!(level() instanceof ServerLevel server)) return;
        double radius = particleRadius();
        server.sendParticles(ParticleTypes.SQUID_INK, getX(), centreY(), getZ(),
                60, radius * 0.6, radius * 0.6, radius * 0.6, 0.1);
        server.sendParticles(ParticleTypes.PORTAL, getX(), centreY(), getZ(),
                50, radius * 0.5, radius * 0.5, radius * 0.5, 0.4);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), centreY(), getZ(),
                30, radius * 0.5, radius * 0.5, radius * 0.5, 0.03);
        server.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.7f);
    }
}
