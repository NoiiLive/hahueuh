package net.noiilive.hahueuh.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.noiilive.hahueuh.client.SensoryDeprivationClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public class MouseInvertMixin {
    @Redirect(
            method = "turnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void hahueuh$overrideLook(LocalPlayer player, double yaw, double pitch) {
        if (SensoryDeprivationClient.isDeprived()) {
            player.turn(-yaw, -pitch);
        } else {
            player.turn(yaw, pitch);
        }
    }
}
