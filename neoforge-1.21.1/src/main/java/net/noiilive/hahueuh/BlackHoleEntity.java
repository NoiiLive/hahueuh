package net.noiilive.hahueuh;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class BlackHoleEntity extends Entity {
    private static final EntityDataAccessor<Boolean> DATA_EMPOWERED =
            SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int FORMATION_TICKS = 25;
    private static final int CORE_DAMAGE_INTERVAL = 10;
    private static final double CENTRE_OFFSET_Y = 1.5;
    private static final double MAX_PULL = 0.35;
    private static final double BLOCK_SUCK_SPEED = 0.45;
    private static final double EXPLOSION_STRENGTH = 1.3;
    private static final double EMPOWERED_STRENGTH_MULTIPLIER = 2.0;

    private UUID casterUuid;

    public BlackHoleEntity(EntityType<? extends BlackHoleEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void setCaster(UUID casterUuid) {
        this.casterUuid = casterUuid;
    }

    public void setEmpowered(boolean empowered) {
        this.entityData.set(DATA_EMPOWERED, empowered);
    }

    public boolean isEmpowered() {
        return this.entityData.get(DATA_EMPOWERED);
    }

    private double pullRadius() {
        return isEmpowered() ? ConfigMagicYin.AL_KARUM_PULL_RADIUS.get() : ConfigMagicYin.UL_SHAMAK_PULL_RADIUS.get();
    }

    private double eventHorizonRadius() {
        return isEmpowered() ? ConfigMagicYin.AL_KARUM_EVENT_HORIZON_RADIUS.get()
                : ConfigMagicYin.UL_SHAMAK_EVENT_HORIZON_RADIUS.get();
    }

    private float coreDamage() {
        return (isEmpowered() ? ConfigMagicYin.AL_KARUM_CORE_DAMAGE.get() : ConfigMagicYin.UL_SHAMAK_CORE_DAMAGE.get())
                .floatValue();
    }

    private int blocksPerTick() {
        return isEmpowered() ? ConfigMagicYin.AL_KARUM_BLOCKS_PER_TICK.get() : ConfigMagicYin.UL_SHAMAK_BLOCKS_PER_TICK.get();
    }

    private double strengthMultiplier() {
        return isEmpowered() ? EMPOWERED_STRENGTH_MULTIPLIER : 1.0;
    }

    private int particleCount(int base) {
        return isEmpowered() ? (int) Math.round(base * EMPOWERED_STRENGTH_MULTIPLIER) : base;
    }

    private int activeTicks() {
        int seconds = isEmpowered() ? ConfigMagicYin.AL_KARUM_DURATION_SECONDS.get()
                : ConfigMagicYin.UL_SHAMAK_DURATION_SECONDS.get();
        return seconds * 20;
    }

    private Vec3 centre() {
        return position().add(0.0, CENTRE_OFFSET_Y, 0.0);
    }

    public float renderScale(float partialTicks) {
        float age = tickCount + partialTicks;
        float progress = age < FORMATION_TICKS ? Mth.clamp(age / FORMATION_TICKS, 0f, 1f) : 1f;
        return progress * (isEmpowered() ? (float) EMPOWERED_STRENGTH_MULTIPLIER : 1f);
    }

    @Override
    public void tick() {
        super.tick();
        int age = tickCount;
        int activeEnd = FORMATION_TICKS + activeTicks();

        if (!(level() instanceof ServerLevel server)) return;

        Vec3 centre = centre();
        if (age >= activeEnd) {
            explode(server, centre);
            discard();
            return;
        }

        if (age < FORMATION_TICKS) {
            spawnFormationInk(server, centre);
            return;
        }
        applySuction(server, centre);
        suckBlocks(server, centre);
        if (age % CORE_DAMAGE_INTERVAL == 0) damageCore(server, centre);
        spawnActiveInk(server, centre);
    }

    private void explode(ServerLevel server, Vec3 centre) {
        double pullR = pullRadius();
        double mult = strengthMultiplier();
        AABB box = new AABB(centre, centre).inflate(pullR);
        for (Entity e : server.getEntities(this, box,
                e -> e != this && e.isAlive() && !HahUeuh.LIONS_HEART.isFrozen(e))) {
            Vec3 out = e.position().subtract(centre);
            double dist = out.length();
            if (dist > pullR) continue;
            Vec3 dir = dist < 0.05 ? new Vec3(0, 1, 0) : out.normalize();
            double strength = (EXPLOSION_STRENGTH * (1.0 - dist / pullR) + 0.25) * mult;
            e.setDeltaMovement(dir.scale(strength).add(0.0, 0.45 * mult, 0.0));
            e.hasImpulse = true;
            e.fallDistance = 0.0f;
            if (e instanceof ServerPlayer sp) {
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
            }
        }

        suckBlocks(server, centre);

        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, centre.x, centre.y, centre.z, particleCount(1), 0, 0, 0, 0);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, centre.x, centre.y, centre.z, particleCount(60), 1.0, 1.0, 1.0, 0.1);
        for (int i = 0; i < particleCount(120); i++) {
            Vec3 dir = randomUnit();
            Vec3 vel = dir.scale(0.8 + random.nextDouble() * 1.0);
            var type = random.nextBoolean() ? ParticleTypes.SQUID_INK : ParticleTypes.REVERSE_PORTAL;
            server.sendParticles(type, centre.x, centre.y, centre.z, 0, vel.x, vel.y, vel.z, 1.0);
        }
        server.playSound(null, blockPosition(), ModSounds.BLACKHOLE_EXPLODE.get(), SoundSource.PLAYERS, 1.6f, 0.5f);
    }

    private void applySuction(ServerLevel server, Vec3 centre) {
        double pullR = pullRadius();
        double ehR = eventHorizonRadius();
        AABB box = new AABB(centre, centre).inflate(pullR);
        for (Entity e : server.getEntities(this, box,
                e -> e != this && e.isAlive() && !HahUeuh.LIONS_HEART.isFrozen(e))) {
            Vec3 toCentre = centre.subtract(e.position());
            double dist = toCentre.length();
            if (dist > pullR || dist < 0.05) continue;

            if (dist <= ehR && (e instanceof ItemEntity || e instanceof FallingBlockEntity)) {
                e.discard();
                continue;
            }

            double strength = MAX_PULL * strengthMultiplier() * (1.0 - dist / pullR);
            Vec3 pull = toCentre.normalize().scale(strength);
            e.setDeltaMovement(e.getDeltaMovement().add(pull));
            e.hasImpulse = true;
            e.fallDistance = 0.0f;
            if (e instanceof ServerPlayer sp) {
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
            }
        }
    }

    private void damageCore(ServerLevel server, Vec3 centre) {
        double ehR = eventHorizonRadius();
        float damage = coreDamage();
        if (damage <= 0f) return;
        AABB core = new AABB(centre, centre).inflate(ehR);
        LivingEntity caster = resolveCaster(server);
        var source = caster != null ? damageSources().indirectMagic(caster, this) : damageSources().magic();
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, core,
                e -> e.isAlive() && e.distanceToSqr(centre) <= ehR * ehR)) {
            e.hurt(source, damage);
        }
    }

    private void suckBlocks(ServerLevel server, Vec3 centre) {
        int perTick = blocksPerTick();
        if (perTick <= 0) return;
        if (!server.getGameRules().getBoolean(ModGameRules.REZERO_BLOCK_DESTRUCTION)) return;

        double pullR = pullRadius();
        double ehR = eventHorizonRadius();
        for (int i = 0; i < perTick; i++) {
            double r = ehR + random.nextDouble() * (pullR - ehR);
            double theta = random.nextDouble() * Math.PI * 2.0;
            double phi = Math.acos(2.0 * random.nextDouble() - 1.0);
            BlockPos pos = BlockPos.containing(
                    centre.x + r * Math.sin(phi) * Math.cos(theta),
                    centre.y + r * Math.cos(phi),
                    centre.z + r * Math.sin(phi) * Math.sin(theta));

            BlockState state = server.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) continue;
            if (state.getDestroySpeed(server, pos) < 0) continue;
            if (state.hasBlockEntity()) continue;

            FallingBlockEntity fb = FallingBlockEntity.fall(server, pos, state);
            fb.setHurtsEntities(0f, 0);
            fb.disableDrop();
            Vec3 toCentre = centre.subtract(fb.position());
            fb.setDeltaMovement(toCentre.normalize().scale(BLOCK_SUCK_SPEED * strengthMultiplier()));
            fb.hasImpulse = true;
        }
    }

    private void spawnFormationInk(ServerLevel server, Vec3 centre) {
        double pullR = pullRadius();
        for (int i = 0; i < particleCount(34); i++) {
            Vec3 dir = randomUnit();
            Vec3 p = centre.add(dir.scale(pullR * (0.4 + random.nextDouble() * 0.6)));
            Vec3 vel = centre.subtract(p).normalize().scale(0.65);
            var type = random.nextInt(3) == 0 ? ParticleTypes.PORTAL : ParticleTypes.SQUID_INK;
            server.sendParticles(type, p.x, p.y, p.z, 0, vel.x, vel.y, vel.z, 1.0);
        }
    }

    private void spawnActiveInk(ServerLevel server, Vec3 centre) {
        double ehR = eventHorizonRadius();
        for (int i = 0; i < particleCount(18); i++) {
            Vec3 dir = randomUnit();
            Vec3 p = centre.add(dir.scale(ehR * (1.0 + random.nextDouble() * 1.5)));
            Vec3 tangent = new Vec3(-dir.z, 0, dir.x).normalize().scale(0.28);
            Vec3 inward = centre.subtract(p).normalize().scale(0.18);
            Vec3 vel = tangent.add(inward);
            var type = random.nextInt(3) == 0 ? ParticleTypes.PORTAL : ParticleTypes.SQUID_INK;
            server.sendParticles(type, p.x, p.y, p.z, 0, vel.x, vel.y, vel.z, 1.0);
        }
    }

    private Vec3 randomUnit() {
        double theta = random.nextDouble() * Math.PI * 2.0;
        double phi = Math.acos(2.0 * random.nextDouble() - 1.0);
        return new Vec3(Math.sin(phi) * Math.cos(theta), Math.cos(phi), Math.sin(phi) * Math.sin(theta));
    }

    private LivingEntity resolveCaster(ServerLevel server) {
        if (casterUuid == null) return null;
        return server.getEntity(casterUuid) instanceof LivingEntity living ? living : null;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(isEmpowered() ? 8.0 : 4.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_EMPOWERED, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Caster")) casterUuid = tag.getUUID("Caster");
        if (tag.contains("Empowered")) setEmpowered(tag.getBoolean("Empowered"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (casterUuid != null) tag.putUUID("Caster", casterUuid);
        tag.putBoolean("Empowered", isEmpowered());
    }
}
