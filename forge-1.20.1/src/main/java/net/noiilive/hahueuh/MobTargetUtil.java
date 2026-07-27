package net.noiilive.hahueuh;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;

public final class MobTargetUtil {
    private MobTargetUtil() {}

    public static void clearTarget(Mob mob) {
        if (mob instanceof Warden warden) {
            warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        } else {
            mob.setTarget(null);
        }
    }
}
