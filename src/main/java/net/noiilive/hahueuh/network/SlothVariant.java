package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.ConfigSloth;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public enum SlothVariant {
    INVISIBLE_PROVIDENCE("invisibleprovidence", "Invisible Providence", "hahueuh.variant.invisible_providence", 1.0f),
    UNSEEN_HANDS("unseenhands", "Unseen Hands", "hahueuh.variant.unseen_hands", 1.0f),
    SEKHMET("sekhmet", "Sekhmet", "hahueuh.variant.sekhmet", 0.5f);

    public final String id;
    public final String displayName;
    public final String translationKey;
    public final float reachMultiplier;

    SlothVariant(String id, String displayName, String translationKey, float reachMultiplier) {
        this.id = id;
        this.displayName = displayName;
        this.translationKey = translationKey;
        this.reachMultiplier = reachMultiplier;
    }

    public static SlothVariant byId(String id) {
        if (id != null) {
            for (SlothVariant v : values()) {
                if (v.id.equalsIgnoreCase(id) || v.name().equalsIgnoreCase(id)) return v;
            }
        }
        return INVISIBLE_PROVIDENCE;
    }

    public static SlothVariant byOrdinal(int ordinal) {
        SlothVariant[] values = values();
        return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : INVISIBLE_PROVIDENCE;
    }

    public static SlothVariant randomWeighted(RandomSource random) {
        int wInvisible = Math.max(0, ConfigSloth.VARIANT_WEIGHT_INVISIBLE_PROVIDENCE.getAsInt());
        int wUnseen = Math.max(0, ConfigSloth.VARIANT_WEIGHT_UNSEEN_HANDS.getAsInt());
        int wSekhmet = Math.max(0, ConfigSloth.VARIANT_WEIGHT_SEKHMET.getAsInt());
        int total = wInvisible + wUnseen + wSekhmet;
        if (total <= 0) return INVISIBLE_PROVIDENCE;

        int roll = random.nextInt(total);
        if (roll < wInvisible) return INVISIBLE_PROVIDENCE;
        if (roll < wInvisible + wUnseen) return UNSEEN_HANDS;
        return SEKHMET;
    }

    public static SlothVariant randomForMob(RandomSource random) {
        SlothVariant[] values = values();
        return values[random.nextInt(values.length)];
    }

    public static float sekhmetSize(UUID uuid) {
        double min = ConfigSloth.SEKHMET_MIN_SIZE.get();
        double max = ConfigSloth.SEKHMET_MAX_SIZE.get();
        if (max < min) { double tmp = min; min = max; max = tmp; }
        float f = (uuid.hashCode() & 0x7fffffff) / (float) Integer.MAX_VALUE;
        return (float) (min + f * (max - min));
    }

    public static float sekhmetShoulderOffset(float size) {
        return 0.3f + 0.03f * size;
    }

    public static float sekhmetHandSplay(float size) {
        return 0.3f * size + 0.15f;
    }

    public static final double SEKHMET_SHOULDER_HEIGHT = 0.7;

    public static final double SEKHMET_BACK_OFFSET = 0.1;


    public static final double UNSEEN_HAND_BACK = 0.12;
    public static final float UNSEEN_ANCHOR_HEIGHT = 0.6f;
    public static final float UNSEEN_CONTROL_BACK = 0.9f;
    private static final float UNSEEN_SIDE_MIN = 0.5f;
    private static final float UNSEEN_SIDE_RANGE = 1.5f;
    private static final float UNSEEN_RISE_UP = 2.1f;
    private static final float UNSEEN_RISE_DOWN = -0.9f;
    private static final float UNSEEN_BOW_MIN = 1.4f;
    private static final float UNSEEN_BOW_RANGE = 1.4f;

    public static int unseenHandCount(UUID uuid) {
        int min = ConfigSloth.UNSEEN_HANDS_MIN.getAsInt();
        int max = ConfigSloth.UNSEEN_HANDS_MAX.getAsInt();
        if (max < min) { int t = min; min = max; max = t; }
        int span = max - min + 1;
        return min + Math.min(span - 1, (int) (rnd(uuid, 0, 0) * span));
    }


    public static float unseenHandSide(int i) { return (i % 2 == 0) ? 1f : -1f; }

    public static float unseenHandSideOffset(UUID uuid, int i) {
        return unseenHandSide(i) * (UNSEEN_SIDE_MIN + rnd(uuid, i, 1) * UNSEEN_SIDE_RANGE);
    }
    public static float unseenHandRise(UUID uuid, int i) {
        return -UNSEEN_RISE_DOWN + rnd(uuid, i, 2) * (UNSEEN_RISE_UP + UNSEEN_RISE_DOWN);
    }
    public static float unseenHandDistBias(UUID uuid, int i) { return rnd(uuid, i, 3) - 0.5f; }
    public static float unseenHandBow(UUID uuid, int i) {
        return unseenHandSide(i) * (UNSEEN_BOW_MIN + rnd(uuid, i, 7) * UNSEEN_BOW_RANGE);
    }

    public static float unseenHandStepPhase(UUID uuid, int i, int count) {
        float even = (float) i / Math.max(1, count);
        float jitter = (rnd(uuid, i, 8) - 0.5f) * 2f;
        float phase = even + jitter;
        return phase - (float) Math.floor(phase);
    }

    private static final float FLAIL_SPEED = 4.5f;
    private static final float FLAIL_YAW_RANGE = 110f;
    private static final float FLAIL_PITCH_RANGE = 85f;
    private static final float TAU = (float) (Math.PI * 2.0);

    private static double flailWave(UUID uuid, int i, double timeSec, int saltBase) {
        float f1 = 0.8f + rnd(uuid, i, saltBase) * 0.9f;
        float f2 = 1.7f + rnd(uuid, i, saltBase + 1) * 1.3f;
        float p1 = rnd(uuid, i, saltBase + 2) * TAU;
        float p2 = rnd(uuid, i, saltBase + 3) * TAU;
        double t = timeSec * FLAIL_SPEED;
        return Math.sin(t * f1 + p1) * 0.6 + Math.sin(t * f2 + p2) * 0.4;
    }

    public static float unseenHandFlailYaw(UUID uuid, int i, double timeSec) {
        return (float) (flailWave(uuid, i, timeSec, 9) * FLAIL_YAW_RANGE);
    }

    public static float unseenHandFlailPitch(UUID uuid, int i, double timeSec) {
        return (float) (flailWave(uuid, i, timeSec, 13) * FLAIL_PITCH_RANGE);
    }

    public static float unseenHandFlailReachMul(UUID uuid, int i, double timeSec) {
        return (float) (0.7 + 0.3 * flailWave(uuid, i, timeSec, 17));
    }

    public static Vec3 direction(float yawDeg, float pitchDeg) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double cp = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * cp, -Math.sin(pitch), Math.cos(yaw) * cp);
    }


    public static final int UNSEEN_MOBILITY_GROUND_SCAN = 48;

    public static double findGroundY(BlockGetter level, double x, double startY, double z, int maxScan) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(x), Mth.floor(startY), Mth.floor(z));
        for (int i = 0; i < maxScan; i++) {
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty()) {
                return pos.getY() + 1;
            }
            pos.move(0, -1, 0);
        }
        return Double.NaN;
    }

    public static void freezeWalkAnimation(LivingEntity entity) {
        entity.walkAnimation.setSpeed(0f);
        entity.walkAnimation.position = 0f;
    }

    public static float attackDamageBonus(LivingEntity owner) {
        float bonus = 0f;
        AttributeInstance attackDamage = owner.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            bonus += (float) (attackDamage.getValue() - attackDamage.getBaseValue());
        }
        if (owner.hasEffect(MobEffects.DAMAGE_BOOST)) {
            bonus += (owner.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() + 1) * 3.0f;
        }
        if (owner.hasEffect(MobEffects.WEAKNESS)) {
            bonus -= (owner.getEffect(MobEffects.WEAKNESS).getAmplifier() + 1) * 4.0f;
        }
        return bonus;
    }

    private static float rnd(UUID uuid, int index, int salt) {
        long h = uuid.getMostSignificantBits() ^ (uuid.getLeastSignificantBits() * 0x9E3779B97F4A7C15L);
        h ^= (long) (index + 1) * 0xC2B2AE3D27D4EB4FL;
        h ^= (long) (salt + 1) * 0x165667B19E3779F9L;
        h ^= h >>> 33; h *= 0xFF51AFD7ED558CCDL; h ^= h >>> 33;
        return (h >>> 40) / (float) (1 << 24);
    }
}
