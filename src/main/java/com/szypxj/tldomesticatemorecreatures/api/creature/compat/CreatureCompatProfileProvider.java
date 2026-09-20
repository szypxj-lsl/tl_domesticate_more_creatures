package com.szypxj.tldomesticatemorecreatures.api.creature.compat;

import net.minecraft.resources.ResourceLocation;

/** Optional third-party compatibility metadata provider. Implementations must not be required for absent mods. */
public interface CreatureCompatProfileProvider {
    String id();

    default int priority() {
        return 0;
    }

    boolean supports(ResourceLocation entityTypeId);

    default ResourceLocation canonicalEntityTypeId(ResourceLocation entityTypeId) {
        return entityTypeId;
    }

    CreatureCompatProfile resolve(CreatureCompatProfileContext context);
}
