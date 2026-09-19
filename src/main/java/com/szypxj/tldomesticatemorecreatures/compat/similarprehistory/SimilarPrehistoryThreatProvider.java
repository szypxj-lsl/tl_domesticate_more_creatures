package com.szypxj.tldomesticatemorecreatures.compat.similarprehistory;

import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CombatThreatChannel;
import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CreatureThreatProfile;
import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CreatureThreatProvider;
import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CreatureThreatProviderRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Treats MCreator baby/adult entity IDs as two forms of one combat profile for radar purposes. */
public final class SimilarPrehistoryThreatProvider implements CreatureThreatProvider {
    private static final Map<String, String> BABY_TO_ADULT = Map.of(
            "dakota_baby", "dakotaraptor",
            "sino_baby", "sinosaurus",
            "oxa_baby", "oxalaia",
            "latenivenatrix_baby", "latenivenatrix",
            "charo_baby", "charonosaurus",
            "coahuilababy", "coahuilasaurus"
    );

    public static void register() { CreatureThreatProviderRegistry.register(new SimilarPrehistoryThreatProvider()); }
    private SimilarPrehistoryThreatProvider() {}

    @Override public String id() { return "tl_domesticate_more_creatures:threat/similar_prehistory"; }
    @Override public int priority() { return 770; }
    @Override public boolean supports(ResourceLocation id) { return id != null && "similar_prehistory".equals(id.getNamespace()); }

    @Override
    public CreatureThreatProfile liveProfile(LivingEntity entity, CreatureThreatProfile fallback) {
        ResourceLocation id = entity == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null) return fallback;
        return enrichSino(id.getPath(), fallback, BABY_TO_ADULT.containsKey(id.getPath()) ? "similar_prehistory:baby" : "similar_prehistory:adult");
    }

    @Override
    public CreatureThreatProfile representativeProfile(EntityType<?> type, ResourceLocation id, CreatureThreatProfile fallback) {
        if (id == null) return fallback;
        String adultPath = BABY_TO_ADULT.get(id.getPath());
        CreatureThreatProfile base = fallback;
        if (adultPath != null) {
            EntityType<?> adultType = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse("similar_prehistory:" + adultPath));
            CreatureThreatProfile adult = defaultProfile(adultType);
            if (adult != null) base = adult;
        }
        String effectivePath = adultPath == null ? id.getPath() : adultPath;
        return enrichSino(effectivePath, base, "similar_prehistory:adult");
    }

    private static CreatureThreatProfile enrichSino(String path, CreatureThreatProfile fallback, String form) {
        List<CombatThreatChannel> channels = new ArrayList<>(fallback.combatChannels());
        if ("sinosaurus".equals(path) || "sino_baby".equals(path)) {
            // 1.4.3's venom arrow has an explicit raw arrow base damage of 0.8 plus several strong debuffs.
            channels.add(new CombatThreatChannel(
                    "similar_prehistory:sino_venom", 0.8D, 0.8D, 0.0D, 1.0D, 1.0D
            ));
        }
        return new CreatureThreatProfile(
                fallback.maxHealth(), fallback.movementSpeed(), fallback.meleeDamage(), channels, form
        );
    }

    private static CreatureThreatProfile defaultProfile(EntityType<?> type) {
        if (type == null || !DefaultAttributes.hasSupplier(type)) return null;
        try {
            @SuppressWarnings("unchecked") EntityType<? extends LivingEntity> living = (EntityType<? extends LivingEntity>) type;
            AttributeSupplier supplier = DefaultAttributes.getSupplier(living);
            if (supplier == null) return null;
            double health = supplier.hasAttribute(Attributes.MAX_HEALTH) ? supplier.getBaseValue(Attributes.MAX_HEALTH) : 0.0D;
            double damage = supplier.hasAttribute(Attributes.ATTACK_DAMAGE) ? supplier.getBaseValue(Attributes.ATTACK_DAMAGE) : 0.0D;
            double speed = supplier.hasAttribute(Attributes.MOVEMENT_SPEED) ? supplier.getBaseValue(Attributes.MOVEMENT_SPEED) : 0.0D;
            return CreatureThreatProfile.basic(health, damage, speed);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
