package net.noiilive.hahueuh;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, HahUeuh.MODID);

    public static final DeferredBlock<PocketVoidBlock> POCKET_VOID = HahUeuh.BLOCKS.registerBlock(
            "pocket_void",
            PocketVoidBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0f, 3_600_000.0f)
                    .noLootTable()
                    .isValidSpawn((s, l, p, e) -> false)
                    .pushReaction(PushReaction.BLOCK)
                    .lightLevel(s -> 0));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PocketVoidBlockEntity>> POCKET_VOID_BE =
            BLOCK_ENTITIES.register("pocket_void",
                    () -> BlockEntityType.Builder.of(PocketVoidBlockEntity::new, POCKET_VOID.get()).build(null));

    private ModBlocks() {}
}
