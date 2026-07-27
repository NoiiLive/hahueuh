package net.noiilive.hahueuh.mixin;

import net.noiilive.hahueuh.client.ClientMagicState;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(MouseHandler.class)
public class MouseInvertMixin {
    @ModifyArgs(
            method = "turnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void hahueuh$overrideLook(Args args) {
        if (ClientMagicState.bodilyDisconnected()) {
            args.set(0, 0.0);
            args.set(1, 0.0);
        } else if (ClientMagicState.sensoryDeprived()) {
            args.set(0, -(double) args.get(0));
            args.set(1, -(double) args.get(1));
        }
    }
}
