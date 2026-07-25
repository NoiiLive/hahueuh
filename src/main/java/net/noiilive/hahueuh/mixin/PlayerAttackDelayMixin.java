package net.noiilive.hahueuh.mixin;

import net.noiilive.hahueuh.DualWield;
import net.noiilive.hahueuh.network.ClientDualWieldState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerAttackDelayMixin {
    @Inject(method = "getCurrentItemAttackStrengthDelay", at = @At("HEAD"), cancellable = true)
    private void hahueuh$dualWieldDelay(CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide()) return;
        if (!DualWield.isDualWielding(self)) return;

        ItemStack next = ClientDualWieldState.offhandNext() ? self.getOffhandItem() : self.getMainHandItem();
        cir.setReturnValue(DualWield.attackDelayTicks(next));
    }
}
