package net.noiilive.hahueuh.mixin;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.noiilive.hahueuh.MiasmaContamination;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ShapedRecipe.class, ShapelessRecipe.class})
public abstract class CraftingRecipeContaminationMixin {
    @Inject(
            method = "assemble(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"))
    private void hahueuh$carryContamination(CraftingContainer input, RegistryAccess registries,
                                            CallbackInfoReturnable<ItemStack> cir) {
        MiasmaContamination.carryContamination(input, cir.getReturnValue());
    }
}
