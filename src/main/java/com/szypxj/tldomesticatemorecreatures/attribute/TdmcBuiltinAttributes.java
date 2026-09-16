package com.szypxj.tldomesticatemorecreatures.attribute;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeDefinition;
import com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeFlags;
import com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeHandle;
import com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeRegistry;
import com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeValueFormat;
import com.szypxj.tldomesticatemorecreatures.config.StatDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class TdmcBuiltinAttributes {
    private static final Map<ResourceLocation, String> LEGACY_IDS = new ConcurrentHashMap<>();

    private TdmcBuiltinAttributes() {
    }

    public static TdmcAttributeDefinition ensureRegistered(
            StatDefinition legacy,
            int displayOrder,
            TdmcAttributeValueFormat valueFormat
    ) {
        ResourceLocation id = ResourceLocation.tryBuild(TlDomesticateMoreCreatures.MOD_ID, legacy.id());
        if (id == null) {
            throw new IllegalArgumentException("Invalid TDMC attribute id: " + legacy.id());
        }
        LEGACY_IDS.putIfAbsent(id, legacy.id());

        TdmcAttributeFlags flags = new TdmcAttributeFlags(
                true,
                true,
                true,
                true,
                true,
                true,
                false
        );
        TdmcAttributeDefinition base = TdmcAttributeDefinition.builder(id, legacy.nameKey())
                .icon(builtinIcon(legacy.id(), legacy.icon()))
                .displayOrder(displayOrder)
                .bounds(0.0D, Double.MAX_VALUE)
                .defaultValue(0.0D)
                .valueFormat(valueFormat)
                .flags(flags)
                .build();

        TdmcAttributeHandle handle = TdmcAttributeRegistry.registerIfAbsent(
                TlDomesticateMoreCreatures.MOD_ID,
                base
        );
        return handle == null ? null : handle.definition();
    }


    private static String builtinIcon(String id, String fallback) {
        String base = TlDomesticateMoreCreatures.MOD_ID + ":textures/gui/attribute/";
        return switch (id) {
            case "health" -> base + "health.png";
            case "damage" -> base + "damage.png";
            case "speed" -> base + "speed.png";
            case "swim_speed" -> base + "swim_speed.png";
            case "resistance" -> base + "resistance.png";
            case "armor" -> base + "armor.png";
            case "torpor" -> base + "torpor.png";
            default -> fallback;
        };
    }

    public static boolean isBuiltin(ResourceLocation id) {
        return id != null && LEGACY_IDS.containsKey(id);
    }

    public static Optional<String> legacyId(ResourceLocation id) {
        return Optional.ofNullable(LEGACY_IDS.get(id));
    }
}
