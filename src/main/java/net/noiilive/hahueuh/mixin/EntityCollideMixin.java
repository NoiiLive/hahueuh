package net.noiilive.hahueuh.mixin;

import net.noiilive.hahueuh.network.ClientLionsHeartState;
import net.noiilive.hahueuh.network.ClientMaterialPhaseState;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityCollideMixin {
    private static final double PHASE_DESCEND_SPEED = 0.18;

    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private void hahueuh$materialPhase(Vec3 desired, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        if (self != Minecraft.getInstance().player || !ClientMaterialPhaseState.isActive()) return;

        double y = desired.y;
        if (ClientLionsHeartState.isActive()) {
            if (ClientLionsHeartState.isDescending()) {
                double targetY = self.getY() + y;
                boolean solidBelow = !self.level().getBlockState(BlockPos.containing(self.getX(), targetY, self.getZ())).isAir();
                if (solidBelow && y < -PHASE_DESCEND_SPEED) {
                    y = -PHASE_DESCEND_SPEED;
                }
            } else if (y < 0) {
                double floor = ClientLionsHeartState.floorLevel();
                double currentY = self.getY();
                if (currentY + y < floor) {
                    y = floor - currentY;
                }
            }
        }
        cir.setReturnValue(new Vec3(desired.x, y, desired.z));
    }

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
