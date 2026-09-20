package com.szypxj.tldomesticatemorecreatures.api.creature.compat;

import com.szypxj.tldomesticatemorecreatures.api.creature.TamingInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Unified optional compatibility metadata shared by TDMC, TCB and TSE. */
public record CreatureCompatProfile(
        ResourceLocation canonicalEntityTypeId,
        ResourceLocation representativeEntityTypeId,
        CreatureDiet diet,
        Component species,
        Component description,
        TamingInfo nativeTamingInfo
) {
    public static final CreatureCompatProfile EMPTY = new CreatureCompatProfile(
            null,
            null,
            CreatureDiet.UNKNOWN,
            Component.empty(),
            Component.empty(),
            TamingInfo.NOT_TAMEABLE
    );

    public CreatureCompatProfile {
        diet = diet == null ? CreatureDiet.UNKNOWN : diet;
        species = species == null ? Component.empty() : species;
        description = description == null ? Component.empty() : description;
        nativeTamingInfo = nativeTamingInfo == null ? TamingInfo.NOT_TAMEABLE : nativeTamingInfo;
    }

    public CreatureCompatProfile withIdentityDefaults(ResourceLocation entityTypeId) {
        Objects.requireNonNull(entityTypeId, "entityTypeId");
        ResourceLocation canonical = canonicalEntityTypeId == null ? entityTypeId : canonicalEntityTypeId;
        ResourceLocation representative = representativeEntityTypeId == null ? canonical : representativeEntityTypeId;
        return new CreatureCompatProfile(canonical, representative, diet, species, description, nativeTamingInfo);
    }
}
