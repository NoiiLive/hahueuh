package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.ClientVisionOfLifeGlowState;
import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.network.SlothVariant;
import net.noiilive.hahueuh.network.WitchFactorAuthority;
import net.noiilive.hahueuh.snapshot.PlayerAuthorityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.UUID;

public final class WitchFactorEntity extends PathfinderMob {
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID =
            SynchedEntityData.defineId(WitchFactorEntity.class, EntityDataSerializers.BYTE);
    private static final String NBT_ASSIGNED_AUTHORITY = "AssignedAuthority";

    private WitchFactorAuthority assignedAuthority = WitchFactorAuthority.NONE;
    private UUID targetUuid;

    public WitchFactorEntity(EntityType<? extends WitchFactorEntity> entityType, Level level) {
        super(entityType, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.13)
                .add(Attributes.FOLLOW_RANGE, 256.0);
    }

    public WitchFactorAuthority getAssignedAuthority() {
        return assignedAuthority;
    }

    public void setAssignedAuthority(WitchFactorAuthority authority) {
        this.assignedAuthority = authority == null ? WitchFactorAuthority.NONE : authority;
    }

    @Override
    public boolean isInvisible() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypes.GENERIC_KILL);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLAGS_ID, (byte) 0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            spawnVisionOfLifeParticles();
        } else {
            setClimbing(this.horizontalCollision);
        }
    }

    private void spawnVisionOfLifeParticles() {
        if (ClientVisionOfLifeGlowState.categoryOf(getId()) == null) return;
        for (int i = 0; i < 2; i++) {
            this.level().addParticle(ParticleTypes.PORTAL,
                    getRandomX(0.5), getRandomY() - 0.25, getRandomZ(0.5),
                    (this.random.nextDouble() - 0.5) * 2.0, -this.random.nextDouble(), (this.random.nextDouble() - 0.5) * 2.0);
        }
    }

    @Override
    public boolean onClimbable() {
        return isClimbing();
    }

    public boolean isClimbing() {
        return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
    }

    public void setClimbing(boolean climbing) {
        byte flags = this.entityData.get(DATA_FLAGS_ID);
        flags = climbing ? (byte) (flags | 1) : (byte) (flags & -2);
        this.entityData.set(DATA_FLAGS_ID, flags);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SeekCompatiblePlayerGoal());
    }

    private static final double MOB_SCAN_RADIUS = 128.0;

    private int scoreOf(UUID uuid) {
        return switch (assignedAuthority) {
            case SLOTH -> HahUeuh.SLOTH_COMPAT.getScore(uuid);
            case GREED -> HahUeuh.GREED_COMPAT.getScore(uuid);
            case NONE -> 0;
        };
    }

    private LivingEntity findMostCompatibleTarget() {
        if (assignedAuthority == WitchFactorAuthority.NONE || !(level() instanceof ServerLevel serverLevel)) return null;
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();

        LivingEntity best = null;
        int bestScore = Integer.MIN_VALUE;
        for (ServerPlayer candidate : serverLevel.getServer().getPlayerList().getPlayers()) {
            UUID candidateUuid = candidate.getUUID();
            boolean eligible = switch (assignedAuthority) {
                case SLOTH -> am.canUseSloth(candidateUuid) && !am.hasWitchFactorSloth(candidateUuid);
                case GREED -> am.canUseGreed(candidateUuid) && !am.hasWitchFactorGreed(candidateUuid);
                case NONE -> false;
            };
            if (eligible && !am.isSageCandidate(candidateUuid) && am.hasOtherWitchFactor(candidateUuid, assignedAuthority)) {
                eligible = false;
            }
            if (!eligible) continue;

            int score = scoreOf(candidate.getUUID());
            if (best == null || score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }

        if (ConfigMain.MOB_WITCH_FACTORS_ENABLED.get()) {
            for (LivingEntity mob : serverLevel.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(MOB_SCAN_RADIUS),
                    e -> e.isAlive() && MobWitchFactor.isEligibleMobType(e)
                            && e.getY() >= ConfigMain.MOB_WITCH_FACTOR_NATURAL_SPAWN_MIN_Y.get()
                            && e.getData(ModAttachments.MOB_WITCH_FACTOR.get()) == WitchFactorAuthority.NONE)) {
                switch (assignedAuthority) {
                    case SLOTH -> HahUeuh.SLOTH_COMPAT.ensureStartingScore(mob.getUUID());
                    case GREED -> HahUeuh.GREED_COMPAT.ensureStartingScore(mob.getUUID());
                    case NONE -> {}
                }
                int score = scoreOf(mob.getUUID());
                if (best == null || score > bestScore) {
                    best = mob;
                    bestScore = score;
                }
            }
        }
        return best;
    }

    private final class SeekCompatiblePlayerGoal extends Goal {
        private LivingEntity target;
        private int retargetCooldown;

        SeekCompatiblePlayerGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public boolean canUse() {
            target = findMostCompatibleTarget();
            retargetCooldown = 0;
            WitchFactorEntity.this.targetUuid = target == null ? null : target.getUUID();
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && findMostCompatibleTarget() != null;
        }

        @Override
        public void stop() {
            target = null;
            WitchFactorEntity.this.targetUuid = null;
            getNavigation().stop();
        }

        @Override
        public void tick() {
            if (retargetCooldown-- <= 0) {
                retargetCooldown = ConfigMain.WITCH_FACTOR_RETARGET_SECONDS.getAsInt() * 20;
                target = findMostCompatibleTarget();
                WitchFactorEntity.this.targetUuid = target == null ? null : target.getUUID();
            }
            if (target != null) {
                getNavigation().moveTo(target, ConfigMain.WITCH_FACTOR_SPEED.get());
                if (!(target instanceof Player)
                        && WitchFactorEntity.this.getBoundingBox().intersects(target.getBoundingBox())) {
                    WitchFactorEntity.this.absorbMob(target);
                }
            }
        }
    }

    @Override
    public void playerTouch(Player player) {
        super.playerTouch(player);
        if (assignedAuthority == WitchFactorAuthority.NONE || isRemoved()) return;
        if (level().isClientSide || !(player instanceof ServerPlayer target)) return;
        if (targetUuid == null || !targetUuid.equals(target.getUUID())) return;

        WitchFactorGrant.grant(target, assignedAuthority);
        target.displayClientMessage(Component.translatable("hahueuh.message.witch_factor_absorbed",
                Component.translatable(assignedAuthority.translationKey)).withStyle(ChatFormatting.LIGHT_PURPLE), true);

        HahUeuh.MOB_WITCH_FACTOR.unregisterWandering(this);
        discard();
    }

    private void absorbMob(LivingEntity mob) {
        if (assignedAuthority == WitchFactorAuthority.NONE || isRemoved()) return;
        if (level().isClientSide || !(mob instanceof Mob target) || targetUuid == null || !targetUuid.equals(target.getUUID())) return;
        if (target.getData(ModAttachments.MOB_WITCH_FACTOR.get()) != WitchFactorAuthority.NONE) return;

        target.setData(ModAttachments.MOB_WITCH_FACTOR.get(), assignedAuthority);
        String variantId = switch (assignedAuthority) {
            case SLOTH -> SlothVariant.randomForMob(target.getRandom()).id;
            case GREED -> GreedVariant.randomForMob(target.getRandom()).id;
            case NONE -> "";
        };
        target.setData(ModAttachments.MOB_WITCH_FACTOR_VARIANT.get(), variantId);
        target.setPersistenceRequired();
        HahUeuh.MOB_WITCH_FACTOR.registerMobHolder(target.getUUID(), assignedAuthority);
        HahUeuh.MOB_WITCH_FACTOR.unregisterWandering(this);
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(NBT_ASSIGNED_AUTHORITY, assignedAuthority.id);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(NBT_ASSIGNED_AUTHORITY, Tag.TAG_STRING)) {
            assignedAuthority = WitchFactorAuthority.byId(tag.getString(NBT_ASSIGNED_AUTHORITY));
        }
    }
}
