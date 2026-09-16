package com.szypxj.tldomesticatemorecreatures.api.attribute;

import net.minecraftforge.eventbus.api.Event;

public final class TdmcAttributeRegistrationEvent extends Event {
    public TdmcAttributeHandle register(String ownerModId, TdmcAttributeDefinition definition) {
        return TdmcAttributeRegistry.register(ownerModId, definition);
    }
}
