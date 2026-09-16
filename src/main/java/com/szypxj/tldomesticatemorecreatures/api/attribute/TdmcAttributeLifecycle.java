package com.szypxj.tldomesticatemorecreatures.api.attribute;

import net.minecraftforge.common.MinecraftForge;

public final class TdmcAttributeLifecycle {
    private static boolean initialized;

    private TdmcAttributeLifecycle() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        MinecraftForge.EVENT_BUS.post(new TdmcAttributeRegistrationEvent());
        TdmcAttributeRegistry.markReady();
        MinecraftForge.EVENT_BUS.post(new TdmcAttributeRegistryReadyEvent());
    }
}
