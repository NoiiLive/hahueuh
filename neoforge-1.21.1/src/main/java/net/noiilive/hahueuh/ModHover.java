package net.noiilive.hahueuh;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class ModHover {
    private ModHover() {}

    public static boolean isHoldingAloft(Entity entity) {
        if (!(entity instanceof Player player)) return false;
        UUID uuid = player.getUUID();
        return HahUeuh.LIONS_HEART.isActive(uuid)
                || HahUeuh.MATERIAL_PHASE.isActive(uuid)
                || HahUeuh.MURAK.isFlying(uuid)
                || HahUeuh.SNAPSHOT_MANAGER.isUnseenHandMobility(uuid);
    }
}
