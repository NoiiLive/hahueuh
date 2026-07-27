package net.noiilive.hahueuh.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.noiilive.hahueuh.ConfigMagicYin;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.MagicSchool;
import net.noiilive.hahueuh.ModEffects;
import net.noiilive.hahueuh.ModSounds;
import net.noiilive.hahueuh.MobTargetUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.EntityHitResult;
import net.noiilive.hahueuh.BlackHoleEntity;
import net.noiilive.hahueuh.MinyaRingEntity;
import net.noiilive.hahueuh.MinyaSpikeEntity;
import net.noiilive.hahueuh.ModEntities;
import net.noiilive.hahueuh.PocketDimension;
import net.noiilive.hahueuh.capability.PlayerData;
import net.noiilive.hahueuh.capability.PlayerDataEvents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Spells {
    public static final ResourceLocation SHAMAK = new ResourceLocation(HahUeuh.MODID, "shamak");
    public static final ResourceLocation EL_SHAMAK = new ResourceLocation(HahUeuh.MODID, "el_shamak");
    public static final ResourceLocation UL_SHAMAK = new ResourceLocation(HahUeuh.MODID, "ul_shamak");
    public static final ResourceLocation AL_SHAMAK = new ResourceLocation(HahUeuh.MODID, "al_shamak");
    public static final ResourceLocation MINYA = new ResourceLocation(HahUeuh.MODID, "minya");
    public static final ResourceLocation EL_MINYA = new ResourceLocation(HahUeuh.MODID, "el_minya");
    public static final ResourceLocation UL_MINYA = new ResourceLocation(HahUeuh.MODID, "ul_minya");

    private static final int SHAMAK_CLOUD_EXPAND_TICKS = 24;
    private static final double UL_SHAMAK_CAST_RANGE = 20.0;
    private static final int MINYA_STAKE_COUNT = 3;
    private static final double MINYA_TARGET_RANGE = 24.0;
    private static final int MINYA_WINDUP_TICKS = 13;
    private static final double EL_MINYA_RADIUS_MIN = 1.3;
    private static final double EL_MINYA_RADIUS_MAX = 3.4;
    private static final double EL_MINYA_HEIGHT_MIN = 0.1;
    private static final double EL_MINYA_HEIGHT_MAX = 2.4;
    private static final double EL_MINYA_MIN_SPACING = 1.0;

    private Spells() {}

    public static void registerAll() {
        SpellRegistry.register(new Spell(
                SHAMAK,
                ConfigMagicYin.SHAMAK_TOTAL_MANA::get,
                ConfigMagicYin.SHAMAK_MANA_PER_TICK::get,
                ConfigMagicYin.SHAMAK_COOLDOWN_SECONDS::get,
                player -> MagicSchool.YIN.acquiredBy(PlayerData.get(player)),
                Spells::castShamak));

        SpellRegistry.register(new Spell(
                EL_SHAMAK,
                ConfigMagicYin.EL_SHAMAK_TOTAL_MANA::get,
                ConfigMagicYin.EL_SHAMAK_MANA_PER_TICK::get,
                ConfigMagicYin.EL_SHAMAK_COOLDOWN_SECONDS::get,
                player -> MagicSchool.YIN.acquiredBy(PlayerData.get(player)),
                Spells::castElShamak));

        SpellRegistry.register(new Spell(
                UL_SHAMAK,
                ConfigMagicYin.UL_SHAMAK_TOTAL_MANA::get,
                ConfigMagicYin.UL_SHAMAK_MANA_PER_TICK::get,
                ConfigMagicYin.UL_SHAMAK_COOLDOWN_SECONDS::get,
                player -> MagicSchool.YIN.acquiredBy(PlayerData.get(player)),
                Spells::castUlShamak));

        SpellRegistry.register(new Spell(
                AL_SHAMAK,
                ConfigMagicYin.AL_SHAMAK_TOTAL_MANA::get,
                ConfigMagicYin.AL_SHAMAK_MANA_PER_TICK::get,
                ConfigMagicYin.AL_SHAMAK_COOLDOWN_SECONDS::get,
                player -> MagicSchool.YIN.acquiredBy(PlayerData.get(player)),
                Spells::castAlShamak));

        SpellRegistry.register(new Spell(
                MINYA,
                ConfigMagicYin.MINYA_TOTAL_MANA::get,
                ConfigMagicYin.MINYA_MANA_PER_TICK::get,
                ConfigMagicYin.MINYA_COOLDOWN_SECONDS::get,
                player -> MagicSchool.YIN.acquiredBy(PlayerData.get(player)),
                Spells::castMinya));

        SpellRegistry.register(new Spell(
                EL_MINYA,
                ConfigMagicYin.EL_MINYA_TOTAL_MANA::get,
                ConfigMagicYin.EL_MINYA_MANA_PER_TICK::get,
                ConfigMagicYin.EL_MINYA_COOLDOWN_SECONDS::get,
                player -> MagicSchool.YIN.acquiredBy(PlayerData.get(player)),
                Spells::castElMinya));

        SpellRegistry.register(new Spell(
                UL_MINYA,
                ConfigMagicYin.UL_MINYA_TOTAL_MANA::get,
                ConfigMagicYin.UL_MINYA_MANA_PER_TICK::get,
                ConfigMagicYin.UL_MINYA_COOLDOWN_SECONDS::get,
                player -> MagicSchool.YIN.acquiredBy(PlayerData.get(player)),
                Spells::castUlMinya));
    }

    private static void castUlMinya(ServerPlayer caster) {
        int targetId = HahUeuh.SPELL_CASTING.consumeUlMinyaTarget(caster.getUUID());
        if (targetId < 0) return;
        net.minecraft.world.entity.Entity targetEntity = caster.serverLevel().getEntity(targetId);
        LivingEntity target = targetEntity instanceof LivingEntity live && live.isAlive() ? live : null;
        if (target == null) return;

        ServerLevel level = caster.serverLevel();
        MinyaRingEntity ring = new MinyaRingEntity(ModEntities.MINYA_RING.get(), level);
        ring.bind(target, caster,
                ConfigMagicYin.UL_MINYA_BIND_SECONDS.get() * 20,
                (float) ConfigMagicYin.UL_MINYA_DAMAGE.get().doubleValue(),
                ConfigMagicYin.UL_MINYA_RING_SCALE.get());
        level.addFreshEntity(ring);

        level.playSound(null, target.blockPosition(), ModSounds.MINYA_SUMMON.get(),
                SoundSource.PLAYERS, 1.2f, 0.8f);
    }

    private static void castMinya(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();

        HitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getHitResultOnViewVector(
                caster, e -> e != caster && e.isAlive() && !e.isSpectator() && e instanceof LivingEntity,
                MINYA_TARGET_RANGE);
        LivingEntity locked = hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le ? le : null;

        float damage = (float) ConfigMagicYin.MINYA_DAMAGE.get().doubleValue();
        for (int i = 0; i < MINYA_STAKE_COUNT; i++) {
            MinyaSpikeEntity stake = new MinyaSpikeEntity(level, caster);
            stake.setDamage(damage);
            if (locked != null) stake.setHomingTarget(locked);

            double side = (i - 1) * 0.85;
            double back = 1.4 + 0.3 * Math.abs(i - 1);
            double up = 1.35;
            if (i == 0) stake.markLaunchSoundEmitter();
            stake.beginCharge(caster, side, up, back, MINYA_WINDUP_TICKS);
            level.addFreshEntity(stake);
        }

        level.playSound(null, caster.blockPosition(), ModSounds.MINYA_SUMMON.get(),
                SoundSource.PLAYERS, 1.2f, 1.0f);
    }

    private static void castElMinya(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();

        HitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getHitResultOnViewVector(
                caster, e -> e != caster && e.isAlive() && !e.isSpectator() && e instanceof LivingEntity,
                MINYA_TARGET_RANGE);
        LivingEntity locked = hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le ? le : null;

        int count = ConfigMagicYin.EL_MINYA_STAKE_COUNT.get();
        float damage = (float) ConfigMagicYin.EL_MINYA_DAMAGE.get().doubleValue();
        List<double[]> slots = randomizedSlotsAroundCaster(caster.getRandom(), count,
                EL_MINYA_RADIUS_MIN, EL_MINYA_RADIUS_MAX, EL_MINYA_HEIGHT_MIN, EL_MINYA_HEIGHT_MAX, EL_MINYA_MIN_SPACING);

        for (int i = 0; i < count; i++) {
            MinyaSpikeEntity stake = new MinyaSpikeEntity(level, caster);
            stake.setElMinya(true);
            stake.setDamage(damage);
            if (locked != null) stake.setHomingTarget(locked);

            double[] slot = slots.get(i);
            if (i == 0) stake.markLaunchSoundEmitter();
            stake.beginCharge(caster, slot[0], slot[2], slot[1], MINYA_WINDUP_TICKS);
            level.addFreshEntity(stake);
        }

        level.playSound(null, caster.blockPosition(), ModSounds.MINYA_SUMMON.get(),
                SoundSource.PLAYERS, 1.3f, 0.85f);
    }

    private static List<double[]> randomizedSlotsAroundCaster(RandomSource rnd, int count,
            double radiusMin, double radiusMax, double heightMin, double heightMax, double minSpacing) {
        List<double[]> slots = new ArrayList<>(count);
        int maxAttempts = 40;
        for (int i = 0; i < count; i++) {
            double bestSide = 0, bestBack = 0, bestUp = 0, bestScore = -1;
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                double theta = rnd.nextDouble() * Math.PI * 2.0;
                double radius = radiusMin + rnd.nextDouble() * (radiusMax - radiusMin);
                double side = radius * Math.sin(theta);
                double back = radius * Math.cos(theta);
                double up = heightMin + rnd.nextDouble() * (heightMax - heightMin);

                double nearestSqr = Double.MAX_VALUE;
                for (double[] existing : slots) {
                    double dSide = side - existing[0];
                    double dBack = back - existing[1];
                    double dUp = up - existing[2];
                    double distSqr = dSide * dSide + dBack * dBack + dUp * dUp;
                    if (distSqr < nearestSqr) nearestSqr = distSqr;
                }

                if (slots.isEmpty() || nearestSqr >= minSpacing * minSpacing) {
                    bestSide = side; bestBack = back; bestUp = up;
                    break;
                }
                if (nearestSqr > bestScore) {
                    bestScore = nearestSqr;
                    bestSide = side; bestBack = back; bestUp = up;
                }
            }
            slots.add(new double[] {bestSide, bestBack, bestUp});
        }
        return slots;
    }

    private static void castElShamak(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 center = caster.position();
        double radius = ConfigMagicYin.EL_SHAMAK_RADIUS.get();
        int effectTicks = ConfigMagicYin.EL_SHAMAK_EFFECT_SECONDS.get() * 20;

        HahUeuh.SPELL_CASTING.spawnExpandingCloud(level, center, ParticleTypes.SQUID_INK,
                radius, SHAMAK_CLOUD_EXPAND_TICKS);

        AABB box = caster.getBoundingBox().inflate(radius);
        double r2 = radius * radius;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && e.isAlive() && e.distanceToSqr(center) <= r2)) {
            target.addEffect(new MobEffectInstance(ModEffects.BODILY_DISCONNECT.get(),
                    effectTicks, 0, false, true, true));
            if (target instanceof Mob mob) MobTargetUtil.clearTarget(mob);
        }

        level.playSound(null, caster.blockPosition(), ModSounds.EL_SHAMAK_USE.get(),
                SoundSource.PLAYERS, 1.4f, 0.5f);
    }

    private static void castUlShamak(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getViewVector(1.0f).scale(UL_SHAMAK_CAST_RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, caster));
        Vec3 spawn = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();

        BlackHoleEntity hole = new BlackHoleEntity(ModEntities.BLACK_HOLE.get(), level);
        hole.setPos(spawn.x, spawn.y, spawn.z);
        hole.setCaster(caster.getUUID());
        level.addFreshEntity(hole);

        level.playSound(null, hole.blockPosition(), ModSounds.BLACKHOLE_SUMMON.get(),
                SoundSource.PLAYERS, 1.6f, 0.4f);
    }

    private static void castAlShamak(ServerPlayer caster) {
        int targetId = HahUeuh.SPELL_CASTING.consumeBanishTarget(caster.getUUID());
        if (targetId < 0) return;
        net.minecraft.world.entity.Entity target = caster.serverLevel().getEntity(targetId);
        if (target == null || !target.isAlive()) return;

        ServerLevel level = caster.serverLevel();
        int seconds = ConfigMagicYin.AL_SHAMAK_BANISH_SECONDS.get();

        List<net.minecraft.world.entity.Entity> group = new ArrayList<>();
        group.add(target);
        double radius = ConfigMagicYin.AL_SHAMAK_AOE_RADIUS.get();
        if (radius > 0.0) {
            AABB box = target.getBoundingBox().inflate(radius);
            double r2 = radius * radius;
            for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != caster && e != target && e.isAlive() && !e.isSpectator()
                            && e.distanceToSqr(target) <= r2)) {
                group.add(nearby);
            }
        }

        Map<net.minecraft.world.entity.Entity, double[]> origins = new LinkedHashMap<>();
        for (net.minecraft.world.entity.Entity e : group) {
            origins.put(e, new double[] {e.getX(), e.getY(), e.getZ(), e.getBbHeight()});
        }

        Set<UUID> banished = new HashSet<>(HahUeuh.POCKET_DIMENSION.banishGroup(group, caster.getUUID(), seconds));
        boolean bannedAnyPlayer = false;
        int trappedNonPlayers = 0;
        for (Map.Entry<net.minecraft.world.entity.Entity, double[]> entry : origins.entrySet()) {
            net.minecraft.world.entity.Entity entity = entry.getKey();
            if (!banished.contains(entity.getUUID())) continue;
            double[] p = entry.getValue();
            PocketDimension.spawnTransitParticles(level, p[0], p[1], p[2], (float) p[3]);
            if (entity instanceof ServerPlayer) bannedAnyPlayer = true;
            else trappedNonPlayers++;
        }

        if (!banished.isEmpty()) {
            level.playSound(null, caster.blockPosition(), ModSounds.AL_SHAMAK_USE.get(),
                    SoundSource.PLAYERS, 1.2f, 1.0f);
        }

        if (trappedNonPlayers > 0) {
            PlayerData.get(caster).setHasTrappedEntities(true);
            PlayerDataEvents.sync(caster);
            if (!bannedAnyPlayer) {
                HahUeuh.SPELL_CASTING.overrideNextCooldown(caster, ConfigMagicYin.AL_SHAMAK_STORE_COOLDOWN_SECONDS.get());
            }
        }
    }

    private static void castShamak(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 center = caster.position();
        double radius = ConfigMagicYin.SHAMAK_RADIUS.get();
        int effectTicks = ConfigMagicYin.SHAMAK_EFFECT_SECONDS.get() * 20;

        HahUeuh.SPELL_CASTING.spawnExpandingCloud(level, center, ParticleTypes.SQUID_INK,
                radius, SHAMAK_CLOUD_EXPAND_TICKS);

        AABB box = caster.getBoundingBox().inflate(radius);
        double r2 = radius * radius;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && e.isAlive() && e.distanceToSqr(center) <= r2)) {
            target.addEffect(new MobEffectInstance(ModEffects.SENSORY_DEPRIVATION.get(),
                    effectTicks, 0, false, true, true));
            if (target instanceof Mob mob) MobTargetUtil.clearTarget(mob);
        }

        level.playSound(null, caster.blockPosition(), ModSounds.SHAMAK_USE.get(),
                SoundSource.PLAYERS, 1.2f, 0.6f);
    }
}
