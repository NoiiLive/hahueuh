package net.noiilive.hahueuh.mixin;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.noiilive.hahueuh.MiasmaContamination;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractCookingRecipe.class)
public abstract class CookingRecipeContaminationMixin {
    @Inject(
            method = "assemble(Lnet/minecraft/world/Container;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"))
    private void hahueuh$carryContamination(Container input, RegistryAccess registries,
                                            CallbackInfoReturnable<ItemStack> cir) {
        MiasmaContamination.carryContamination(input, cir.getReturnValue());
    }
}
