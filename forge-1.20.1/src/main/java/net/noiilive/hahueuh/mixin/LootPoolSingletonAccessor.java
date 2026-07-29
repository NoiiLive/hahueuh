package net.noiilive.hahueuh.mixin;

import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LootPoolSingletonContainer.class)
public interface LootPoolSingletonAccessor {
    @Accessor("weight")
    int hahueuh$getWeight();

    @Accessor("functions")
    LootItemFunction[] hahueuh$getFunctions();
}
