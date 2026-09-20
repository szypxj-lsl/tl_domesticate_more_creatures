package com.szypxj.tldomesticatemorecreatures.compat.fossil;

import com.szypxj.tldomesticatemorecreatures.api.creature.TamingInfo;
import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureCompatProfile;
import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureCompatProfileContext;
import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureCompatProfileProvider;
import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureCompatProfileRegistry;
import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureDiet;
import com.szypxj.tldomesticatemorecreatures.compat.ModCompatResources;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.lang.reflect.Method;
import java.util.Optional;

/** Authoritative metadata bridge for every Fossils & Archeology prehistoric entity. */
public final class FossilCompatProfileProvider implements CreatureCompatProfileProvider {
    private static final String MOD_ID = "fossil";
    private static final String INFO_CLASS =
            "com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.PrehistoricEntityInfo";

    public static void register() {
        CreatureCompatProfileRegistry.register(new FossilCompatProfileProvider());
    }

    public FossilCompatProfileProvider() {
    }

    @Override
    public String id() {
        return "tl_domesticate_more_creatures:profile/fossil";
    }

    @Override
    public int priority() {
        return 850;
    }

    @Override
    public boolean supports(ResourceLocation entityTypeId) {
        return entityTypeId != null && MOD_ID.equals(entityTypeId.getNamespace());
    }

    @Override
    public CreatureCompatProfile resolve(CreatureCompatProfileContext context) {
        if (context == null || !supports(context.entityTypeId())) {
            return CreatureCompatProfile.EMPTY;
        }
        Object info = findInfo(context.entityType(), context.entityTypeId());
        Object data = invoke(info, "data");
        CreatureDiet diet = mapDiet(enumName(invoke(data, "diet")));
        Component species = species(enumName(invoke(info, "mobType")));
        Component description = officialLore(context).<Component>map(Component::literal).orElse(Component.empty());
        TamingInfo nativeTaming = FossilTamingInfoProvider.infoFor(context.entityType());
        return new CreatureCompatProfile(
                context.entityTypeId(),
                context.entityTypeId(),
                diet,
                species,
                description,
                nativeTaming
        );
    }

    private static CreatureDiet mapDiet(String name) {
        return switch (name) {
            case "HERBIVORE" -> CreatureDiet.HERBIVORE;
            case "OMNIVORE" -> CreatureDiet.OMNIVORE;
            case "CARNIVORE", "CARNIVORE_EGG", "PISCIVORE", "PISCI_CARNIVORE", "INSECTIVORE" ->
                    CreatureDiet.CARNIVORE;
            // PASSIVE describes behavior, not a reliable trophic category.
            default -> CreatureDiet.UNKNOWN;
        };
    }

    private static Component species(String mobType) {
        String key = switch (mobType) {
            case "DINOSAUR", "DINOSAUR_AQUATIC", "DINOSAUR_FISH" ->
                    "compat.tl_domesticate_more_creatures.species.dinosaur";
            case "ARTHROPOD" -> "compat.tl_domesticate_more_creatures.species.arthropod";
            case "FISH" -> "compat.tl_domesticate_more_creatures.species.fish";
            case "MAMMAL" -> "compat.tl_domesticate_more_creatures.species.mammal";
            case "BIRD", "VANILLA_BIRD", "TERRORBIRD" ->
                    "compat.tl_domesticate_more_creatures.species.prehistoric_bird";
            default -> "compat.tl_domesticate_more_creatures.species.prehistoric_creature";
        };
        return Component.translatable(key);
    }

    private static Optional<String> officialLore(CreatureCompatProfileContext context) {
        String entityPath = context.entityTypeId().getPath();
        return ModCompatResources.readLocalized(
                        MOD_ID,
                        context.languageCode(),
                        language -> "assets/fossil/dinopedia/" + language + "/" + entityPath + ".txt"
                )
                .map(String::trim)
                .filter(text -> !text.isBlank());
    }

    private static Object findInfo(EntityType<?> type, ResourceLocation id) {
        try {
            Class<?> infoClass = Class.forName(INFO_CLASS);
            Method entityType = infoClass.getMethod("entityType");
            Method resourceName = null;
            try {
                resourceName = infoClass.getMethod("resourceName");
            } catch (NoSuchMethodException ignored) {
            }
            Object[] values = infoClass.getEnumConstants();
            if (values == null) {
                return null;
            }
            for (Object value : values) {
                if (type != null && entityType.invoke(value) == type) {
                    return value;
                }
                if (resourceName != null && id != null) {
                    Object name = resourceName.invoke(value);
                    if (id.getPath().equals(name)) {
                        return value;
                    }
                }
                if (id != null && value instanceof Enum<?> e
                        && id.getPath().equalsIgnoreCase(e.name().toLowerCase(java.util.Locale.ROOT))) {
                    return value;
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
        return null;
    }

    private static Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static String enumName(Object value) {
        return value instanceof Enum<?> e ? e.name() : "";
    }
}
