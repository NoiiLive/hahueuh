package net.noiilive.hahueuh.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChunkDataProvider implements ICapabilitySerializable<CompoundTag> {
    private final ChunkMana mana = new ChunkMana();
    private final ChunkMiasma miasma = new ChunkMiasma();
    private final LazyOptional<ChunkMana> manaOptional = LazyOptional.of(() -> mana);
    private final LazyOptional<ChunkMiasma> miasmaOptional = LazyOptional.of(() -> miasma);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.CHUNK_MANA) return manaOptional.cast();
        if (cap == ModCapabilities.CHUNK_MIASMA) return miasmaOptional.cast();
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("Mana", mana.serializeNBT());
        tag.put("Miasma", miasma.serializeNBT());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        mana.deserializeNBT(tag.getCompound("Mana"));
        miasma.deserializeNBT(tag.getCompound("Miasma"));
    }

    public void invalidate() {
        manaOptional.invalidate();
        miasmaOptional.invalidate();
    }
}
