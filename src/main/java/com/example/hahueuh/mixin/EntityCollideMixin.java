package com.example.hahueuh.mixin;

import com.example.hahueuh.network.ClientLionsHeartState;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityCollideMixin {
    @ModifyReturnValue(method = "collide", at = @At("RETURN"))
    private Vec3 hahueuh$lionsHeartFloor(Vec3 original) {
        Entity self = (Entity) (Object) this;
        if (self != Minecraft.getInstance().player || !ClientLionsHeartState.isActive()
                || ClientLionsHeartState.isDescending()) {
            return original;
        }
        if (original.y >= 0) return original;

        double floor = ClientLionsHeartState.floorLevel();
        double currentY = self.getY();
        double targetY = currentY + original.y;
        if (targetY >= floor) return original;

        boolean liquidBelow = !self.level().getFluidState(BlockPos.containing(self.getX(), targetY, self.getZ())).isEmpty();
        if (!liquidBelow && !ClientLionsHeartState.isAirWalkEnabled()) {
            ClientLionsHeartState.lowerFloorLevel(targetY);
            return original;
        }

        return new Vec3(original.x, floor - currentY, original.z);
    }
}
