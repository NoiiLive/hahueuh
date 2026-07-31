package net.noiilive.hahueuh;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.noiilive.hahueuh.network.WitchFactorAuthority;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HahUeuh.MODID);

    public static final RegistryObject<CreativeModeTab> HAHUEUH = CREATIVE_MODE_TABS.register("hahueuh",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.hahueuh"))
                    .icon(() -> new ItemStack(ModItems.SAGES_BOX.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.MEMORIES_OF_THE_WORLD.get());
                        output.accept(ModItems.DRAGON_SWORD_REID.get());
                        output.accept(ModItems.BOWEL_HUNTER_KUKRI.get());
                        output.accept(ModItems.BOWEL_HUNTER_WHITE_BLADE.get());
                        output.accept(ModItems.BOWEL_HUNTER_BLACK_BLADE.get());
                        output.accept(ModItems.SPIKED_CLUB.get());
                        output.accept(ModItems.MORNINGSTAR.get());
                        output.accept(ModItems.GUILTYWHIP.get());
                        output.accept(ModItems.SAGES_BOX.get());
                        output.accept(SagesBoxItem.withAuthority(WitchFactorAuthority.SLOTH));
                        output.accept(SagesBoxItem.withAuthority(WitchFactorAuthority.GREED));
                        output.accept(ModItems.BOOK_OF_WISDOM_COPY.get());
                        for (net.noiilive.hahueuh.network.BoundVisionAbility ability
                                : net.noiilive.hahueuh.network.BoundVisionAbility.values()) {
                            ItemStack bound = new ItemStack(ModItems.BOOK_OF_WISDOM_COPY.get());
                            BookOfWisdomCopyItem.setBoundAbility(bound, ability.ordinal());
                            output.accept(bound);
                        }
                    })
                    .build());

    private ModCreativeTabs() {}
}
