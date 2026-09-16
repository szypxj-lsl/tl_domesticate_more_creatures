package com.szypxj.tldomesticatemorecreatures.registry;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TlDomesticateMoreCreatures.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB =
            CREATIVE_MODE_TABS.register("main",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("creativetab.tl_domesticate_more_creatures.main"))
                            .icon(() -> ModItems.PET_EXPERIENCE_POTION.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                output.accept(ModItems.PET_EXPERIENCE_POTION.get());
                                output.accept(ModItems.EMPTY_PET_EXPERIENCE_BOTTLE.get());
                                output.accept(ModItems.PET_EXPERIENCE_BOTTLE.get());
                                output.accept(ModItems.ATTRIBUTE_RESET_CRYSTAL.get());
                                output.accept(ModItems.NARCOTIC.get());
                                output.accept(ModItems.STRONG_NARCOTIC.get());
                                output.accept(ModItems.CONCENTRATED_NARCOTIC.get());
                                output.accept(ModItems.NARCOTIC_ARROW.get());
                                output.accept(ModItems.STRONG_NARCOTIC_ARROW.get());
                                output.accept(ModItems.CONCENTRATED_NARCOTIC_ARROW.get());
                                output.accept(ModItems.CLUB.get());
                                output.accept(ModItems.SUPER_SPYGLASS.get());
                            })
                            .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus bus) {
        CREATIVE_MODE_TABS.register(bus);
    }
}