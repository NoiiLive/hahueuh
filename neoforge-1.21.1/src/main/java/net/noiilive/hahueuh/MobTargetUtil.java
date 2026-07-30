package net.noiilive.hahueuh;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public final class MobTargetUtil {
    private MobTargetUtil() {}

    public static boolean isPacified(LivingEntity entity) {
        return entity.hasEffect(ModEffects.BODILY_DISCONNECT)
                || entity.hasEffect(ModEffects.SENSORY_DEPRIVATION);
    }

    public static void clearTarget(Mob mob) {
        mob.setTarget(null);
        mob.setLastHurtByMob(null);

        var brain = mob.getBrain();
        brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        brain.eraseMemory(MemoryModuleType.ANGRY_AT);
        brain.eraseMemory(MemoryModuleType.HURT_BY);
        brain.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
    }
}
