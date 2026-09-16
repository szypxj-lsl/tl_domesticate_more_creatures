package com.szypxj.tldomesticatemorecreatures.api.attribute;

import net.minecraftforge.eventbus.api.Event;

import java.util.List;

public final class TdmcAttributeRegistryReadyEvent extends Event {
    public List<TdmcAttributeHandle> attributes() {
        return TdmcAttributeRegistry.getAll();
    }
}
