package net.noiilive.hahueuh.mixin;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityIgnoresBlockTriggersMixin {
    @Inject(method = "isIgnoringBlockTriggers", at = @At("HEAD"), cancellable = true)
    private void hahueuh$lionsHeartIgnoresBlockTriggers(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayer player && HahUeuh.LIONS_HEART.isActive(player.getUUID())) {
            cir.setReturnValue(true);
        }
    }
}
