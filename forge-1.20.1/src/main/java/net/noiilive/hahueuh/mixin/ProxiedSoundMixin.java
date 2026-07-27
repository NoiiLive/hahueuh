package net.noiilive.hahueuh.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.noiilive.hahueuh.ProxiedInteractionSound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerLevel.class)
public abstract class ProxiedSoundMixin {
    @ModifyVariable(
            method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"), argsOnly = true, index = 1)
    private Player hahueuh$unmaskPositionSound(Player player) {
        return ProxiedInteractionSound.unmask((ServerLevel) (Object) this, player);
    }

    @ModifyVariable(
            method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"), argsOnly = true, index = 1)
    private Player hahueuh$unmaskEntitySound(Player player) {
        return ProxiedInteractionSound.unmask((ServerLevel) (Object) this, player);
    }
}
