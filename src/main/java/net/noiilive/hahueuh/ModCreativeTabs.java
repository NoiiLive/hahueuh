package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.BoundVisionAbility;
import net.noiilive.hahueuh.network.WitchFactorAuthority;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HahUeuh.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HAHUEUH = CREATIVE_MODE_TABS.register("hahueuh",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.hahueuh"))
                    .icon(() -> new ItemStack(ModItems.SAGES_BOX.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.DRAGON_SWORD_REID.get());
                        output.accept(ModItems.SAGES_BOX.get());
                        for (WitchFactorAuthority authority : new WitchFactorAuthority[]{WitchFactorAuthority.SLOTH, WitchFactorAuthority.GREED}) {
                            ItemStack stack = new ItemStack(ModItems.SAGES_BOX.get());
                            stack.set(ModDataComponents.SAGES_BOX_SIN.get(), authority.id);
                            output.accept(stack);
                        }
                        for (BoundVisionAbility ability : BoundVisionAbility.values()) {
                            ItemStack stack = new ItemStack(ModItems.BOOK_OF_WISDOM_COPY.get());
                            stack.set(ModDataComponents.BOUND_VISION_ABILITY.get(), ability.ordinal());
                            output.accept(stack);
                        }
                    })
                    .build());

    private ModCreativeTabs() {}
}
