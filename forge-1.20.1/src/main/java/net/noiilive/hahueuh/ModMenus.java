package net.noiilive.hahueuh;

import net.noiilive.hahueuh.menu.BookOfWisdomBindMenu;
import net.noiilive.hahueuh.menu.EfficientEnchantingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, HahUeuh.MODID);

    public static final RegistryObject<MenuType<EfficientEnchantingMenu>> EFFICIENT_ENCHANTING =
            MENUS.register("efficient_enchanting",
                    () -> IForgeMenuType.create((id, inv, data) -> new EfficientEnchantingMenu(id, inv)));

    public static final RegistryObject<MenuType<BookOfWisdomBindMenu>> BOOK_OF_WISDOM_BIND =
            MENUS.register("book_of_wisdom_bind",
                    () -> IForgeMenuType.create((id, inv, data) -> new BookOfWisdomBindMenu(id, inv)));
}
