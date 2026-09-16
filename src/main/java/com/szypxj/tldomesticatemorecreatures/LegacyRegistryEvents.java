package com.szypxj.tldomesticatemorecreatures;

import com.szypxj.tldomesticatemorecreatures.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID)
public final class LegacyRegistryEvents {
    private LegacyRegistryEvents() {
    }

    @SubscribeEvent
    public static void onMissingMappings(MissingMappingsEvent event) {
        for (MissingMappingsEvent.Mapping<Item> mapping : event.getMappings(
                ForgeRegistries.Keys.ITEMS,
                LegacyMigrationService.LEGACY_MOD_ID
        )) {
            switch (mapping.getKey().getPath()) {
                case "attribute_reset_crystal" -> mapping.remap(ModItems.ATTRIBUTE_RESET_CRYSTAL.get());
                case "pet_experience_potion" -> mapping.remap(ModItems.PET_EXPERIENCE_POTION.get());
                default -> {
                }
            }
        }
    }
}
