package net.noiilive.hahueuh;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, HahUeuh.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> BOOK_OWNER =
            DATA_COMPONENT_TYPES.register("book_owner",
                    () -> DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BOUND_VISION_ABILITY =
            DATA_COMPONENT_TYPES.register("bound_vision_ability",
                    () -> DataComponentType.<Integer>builder().persistent(Codec.INT).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> VISION_COOLDOWN_UNTIL =
            DATA_COMPONENT_TYPES.register("vision_cooldown_until",
                    () -> DataComponentType.<Long>builder().persistent(Codec.LONG).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SAGES_BOX_SIN =
            DATA_COMPONENT_TYPES.register("sages_box_sin",
                    () -> DataComponentType.<String>builder().persistent(Codec.STRING).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> DRAGON_SWORD_SHEATHED =
            DATA_COMPONENT_TYPES.register("dragon_sword_sheathed",
                    () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL).build());

    private ModDataComponents() {}
}
