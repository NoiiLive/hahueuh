package net.noiilive.hahueuh.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.noiilive.hahueuh.ModHover;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerFloatKickMixin {
    @Inject(method = "noBlocksAround", at = @At("HEAD"), cancellable = true)
    private void hahueuh$allowModHover(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ModHover.isHoldingAloft(entity)) {
            cir.setReturnValue(false);
        }
    }
}
