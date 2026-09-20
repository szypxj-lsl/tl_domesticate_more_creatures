package com.szypxj.tldomesticatemorecreatures.compat.wanancientbeasts;

import com.szypxj.tldomesticatemorecreatures.api.creature.TamingInfo;
import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureCompatProfile;
import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureCompatProfileContext;
import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureCompatProfileProvider;
import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureDiet;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

/** Metadata proven from Wan 1.4.3 entity tags/classes; unknown fields intentionally remain unknown. */
public final class WanCompatProfileProvider implements CreatureCompatProfileProvider {
    static final Set<String> ANCIENT_MOBS = Set.of(
            "eater", "walker", "crusher", "glider", "soarer", "surfer", "charger", "raider", "snatcher"
    );
    static final Set<String> NATIVE_TAMEABLES = Set.of(
            "charger", "glider", "snatcher", "soarer", "surfer", "walker"
    );

    @Override public String id() { return "tl_domesticate_more_creatures:profile/wan_ancient_beasts"; }
    @Override public int priority() { return 840; }
    @Override public boolean supports(ResourceLocation id) {
        return id != null && "wan_ancient_beasts".equals(id.getNamespace());
    }

    @Override
    public CreatureCompatProfile resolve(CreatureCompatProfileContext context) {
        if (context == null || !supports(context.entityTypeId())) return CreatureCompatProfile.EMPTY;
        String path = context.entityTypeId().getPath();
        CreatureDiet diet = "eater".equals(path) ? CreatureDiet.CARNIVORE : CreatureDiet.UNKNOWN;
        Component species;
        if (ANCIENT_MOBS.contains(path)) {
            species = Component.translatable("compat.tl_domesticate_more_creatures.species.ancient_beast");
        } else if ("toxlacanth".equals(path)) {
            species = Component.translatable("compat.tl_domesticate_more_creatures.species.fish");
        } else {
            species = Component.empty();
        }
        TamingInfo nativeTaming = NATIVE_TAMEABLES.contains(path)
                ? new TamingInfo(true, "WAN_NATIVE", 1, List.of())
                : TamingInfo.NOT_TAMEABLE;
        // Wan 1.4.3 contains localized names/advancements but no species lore resource; leave description empty.
        return new CreatureCompatProfile(
                context.entityTypeId(), context.entityTypeId(), diet, species, Component.empty(), nativeTaming
        );
    }
}
