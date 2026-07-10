package net.noiilive.hahueuh.mixin;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerMovementPhaseMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "isPlayerCollidingWithAnythingNew", at = @At("HEAD"), cancellable = true)
    private void hahueuh$allowMaterialPhase(CallbackInfoReturnable<Boolean> cir) {
        if (player != null && HahUeuh.MATERIAL_PHASE.isActive(player.getUUID())) {
            cir.setReturnValue(false);
        }
    }
}
