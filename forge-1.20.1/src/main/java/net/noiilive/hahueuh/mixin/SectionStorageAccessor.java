package net.noiilive.hahueuh.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(SectionStorage.class)
public interface SectionStorageAccessor {
    @Accessor("storage")
    Long2ObjectMap<Optional<?>> hahueuh$getStorage();

    @Accessor("dirty")
    LongLinkedOpenHashSet hahueuh$getDirty();
}
