package net.noiilive.hahueuh;

import net.noiilive.hahueuh.menu.BookOfWisdomBindMenu;
import net.noiilive.hahueuh.menu.EfficientEnchantingMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, HahUeuh.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<EfficientEnchantingMenu>> EFFICIENT_ENCHANTING =
            MENUS.register("efficient_enchanting",
                    () -> IMenuTypeExtension.create((id, inv, data) -> new EfficientEnchantingMenu(id, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<BookOfWisdomBindMenu>> BOOK_OF_WISDOM_BIND =
            MENUS.register("book_of_wisdom_bind",
                    () -> IMenuTypeExtension.create((id, inv, data) -> new BookOfWisdomBindMenu(id, inv)));
}
