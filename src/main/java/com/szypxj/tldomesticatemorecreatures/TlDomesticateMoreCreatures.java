package com.szypxj.tldomesticatemorecreatures;

import com.szypxj.tldomesticatemorecreatures.brewing.NarcoticBrewingRecipes;
import com.szypxj.tldomesticatemorecreatures.config.ClientConfig;
import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.compat.CompatBootstrap;
import com.szypxj.tldomesticatemorecreatures.compat.arsnouveau.ArsNouveauManaCompat;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.equipment.PetEquipmentConfigManager;
import com.szypxj.tldomesticatemorecreatures.registry.ModCreativeTabs;
import com.szypxj.tldomesticatemorecreatures.registry.ModEffects;
import com.szypxj.tldomesticatemorecreatures.registry.ModItems;
import com.szypxj.tldomesticatemorecreatures.registry.ModMenus;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingConfigManager;
import com.szypxj.tldomesticatemorecreatures.riding.provider.RideStateBridge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TlDomesticateMoreCreatures.MOD_ID)
public final class TlDomesticateMoreCreatures {
    public static final String MOD_ID = "tl_domesticate_more_creatures";

    public TlDomesticateMoreCreatures(FMLJavaModLoadingContext context) {
        LegacyMigrationService.migrateConfigFiles();

        IEventBus modEventBus = context.getModEventBus();
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, MOD_ID + "-client.toml");
        ModItems.register(modEventBus);
        ModEffects.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenus.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ArsNouveauManaCompat.register();
            NetworkHandler.register();
            RidingConfigManager.initialize();
            RideStateBridge.register();
            PetEquipmentConfigManager.initialize();
            CompatBootstrap.init();
            NarcoticBrewingRecipes.register();
        });
    }
}
