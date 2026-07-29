package net.noiilive.hahueuh.mixin;

import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompositeEntryBase.class)
public interface CompositeEntryAccessor {
    @Accessor("children")
    LootPoolEntryContainer[] hahueuh$getChildren();
}
