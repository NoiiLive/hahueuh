package net.noiilive.hahueuh.mixin;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ServerEntityPhaseMixin {
    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private void hahueuh$serverMaterialPhase(Vec3 desired, CallbackInfoReturnable<Vec3> cir) {
        if ((Object) this instanceof ServerPlayer player && HahUeuh.MATERIAL_PHASE.isActive(player.getUUID())) {
            cir.setReturnValue(desired);
        }
    }
}
