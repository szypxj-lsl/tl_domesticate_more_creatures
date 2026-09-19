package com.szypxj.tldomesticatemorecreatures.compat.wanancientbeasts;

import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CombatThreatChannel;
import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CreatureThreatProfile;
import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CreatureThreatProvider;
import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CreatureThreatProviderRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/** Adds Wan combat mechanics that are not represented by the ordinary attack attribute. */
public final class WanThreatProvider implements CreatureThreatProvider {
    private static final String MOD_ID = "wan_ancient_beasts";

    public static void register() { CreatureThreatProviderRegistry.register(new WanThreatProvider()); }
    private WanThreatProvider() {}

    @Override public String id() { return "tl_domesticate_more_creatures:threat/wan_ancient_beasts"; }
    @Override public int priority() { return 760; }
    @Override public boolean supports(ResourceLocation id) { return id != null && MOD_ID.equals(id.getNamespace()); }

    @Override
    public CreatureThreatProfile liveProfile(LivingEntity entity, CreatureThreatProfile fallback) {
        ResourceLocation id = entity == null ? null : net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return enrich(id, fallback, "wan_ancient_beasts:live");
    }

    @Override
    public CreatureThreatProfile representativeProfile(EntityType<?> type, ResourceLocation id, CreatureThreatProfile fallback) {
        return enrich(id, fallback, "wan_ancient_beasts:representative");
    }

    private static CreatureThreatProfile enrich(ResourceLocation id, CreatureThreatProfile fallback, String form) {
        if (id == null || fallback == null) return fallback;
        List<CombatThreatChannel> channels = new ArrayList<>(fallback.combatChannels());
        if ("charger".equals(id.getPath()) && fallback.meleeDamage() > 0.0D) {
            // Wan's vehicleAbility doubles attackMultiplier before charge collision transitions into its attack.
            double chargeDamage = fallback.meleeDamage() * 2.0D;
            channels.add(new CombatThreatChannel(
                    "wan_ancient_beasts:charger_charge", chargeDamage, chargeDamage, 0.0D, 1.0D, 0.20D
            ));
        }
        // Toxlacanth's venom cloud is status/control-only in 1.4.3; no invented raw damage is added here.
        return new CreatureThreatProfile(
                fallback.maxHealth(), fallback.movementSpeed(), fallback.meleeDamage(), channels, form
        );
    }
}
