package net.noiilive.hahueuh.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.noiilive.hahueuh.HahUeuh;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LionsHeartArmorBypassMixin {
    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"), cancellable = true)
    private void hahueuh$lionsHeartBypassArmor(DamageSource source, float amount,
                                               CallbackInfoReturnable<Float> cir) {
        if (HahUeuh.LIONS_HEART.attackBypassesReductions(source)) {
            cir.setReturnValue(amount);
        }
    }

    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("HEAD"), cancellable = true)
    private void hahueuh$lionsHeartBypassEnchantments(DamageSource source, float amount,
                                                      CallbackInfoReturnable<Float> cir) {
        if (HahUeuh.LIONS_HEART.attackBypassesReductions(source)) {
            cir.setReturnValue(amount);
        }
    }
}
