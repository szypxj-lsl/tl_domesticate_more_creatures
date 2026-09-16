package com.szypxj.tldomesticatemorecreatures.registry;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.menu.AttributePanelMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, TlDomesticateMoreCreatures.MOD_ID);
    public static final RegistryObject<MenuType<AttributePanelMenu>> ATTRIBUTE_PANEL = MENUS.register(
            "attribute_panel",
            () -> IForgeMenuType.create(AttributePanelMenu::new)
    );

    private ModMenus() {
    }

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
