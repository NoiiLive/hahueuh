package net.noiilive.hahueuh.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RandomizableContainerBlockEntity.class)
public interface LootTableHolderAccessor {
    @Accessor("lootTable")
    ResourceLocation hahueuh$getLootTable();
}
