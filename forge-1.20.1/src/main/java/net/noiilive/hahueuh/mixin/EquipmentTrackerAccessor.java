package net.noiilive.hahueuh.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface EquipmentTrackerAccessor {
    @Accessor("lastHandItemStacks")
    NonNullList<ItemStack> hahueuh$getLastHandItemStacks();

    @Accessor("lastArmorItemStacks")
    NonNullList<ItemStack> hahueuh$getLastArmorItemStacks();
}
