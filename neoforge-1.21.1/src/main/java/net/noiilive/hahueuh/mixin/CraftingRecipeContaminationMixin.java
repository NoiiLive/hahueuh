package net.noiilive.hahueuh.mixin;

import net.noiilive.hahueuh.MiasmaContamination;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ShapedRecipe.class, ShapelessRecipe.class})
public abstract class CraftingRecipeContaminationMixin {
    @Inject(
            method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"))
    private void hahueuh$carryContamination(CraftingInput input, HolderLookup.Provider registries,
                                            CallbackInfoReturnable<ItemStack> cir) {
        MiasmaContamination.carryContamination(input, cir.getReturnValue());
    }
}
