package net.noiilive.hahueuh;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, HahUeuh.MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, HahUeuh.MODID);

    public static final RegistryObject<PocketVoidBlock> POCKET_VOID = BLOCKS.register("pocket_void",
            () -> new PocketVoidBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0f, 3_600_000.0f)
                    .noLootTable()
                    .isValidSpawn((s, l, p, e) -> false)
                    .pushReaction(PushReaction.BLOCK)
                    .lightLevel(s -> 0)));

    public static final RegistryObject<BlockEntityType<PocketVoidBlockEntity>> POCKET_VOID_BE =
            BLOCK_ENTITIES.register("pocket_void",
                    () -> BlockEntityType.Builder.of(PocketVoidBlockEntity::new, POCKET_VOID.get()).build(null));

    private ModBlocks() {}
}
