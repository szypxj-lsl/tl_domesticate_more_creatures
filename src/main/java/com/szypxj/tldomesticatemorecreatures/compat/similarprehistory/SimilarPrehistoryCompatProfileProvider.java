package com.szypxj.tldomesticatemorecreatures.compat.similarprehistory;

import com.szypxj.tldomesticatemorecreatures.api.creature.TamingInfo;
import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureCompatProfile;
import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureCompatProfileContext;
import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureCompatProfileProvider;
import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureDiet;
import com.szypxj.tldomesticatemorecreatures.compat.ModCompatResources;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Canonical baby/adult metadata bridge for Similar Prehistory 2.1.1. */
public final class SimilarPrehistoryCompatProfileProvider implements CreatureCompatProfileProvider {
    private static final String MOD_ID = "similar_prehistory";
    private static final Map<String, String> BABY_TO_ADULT = Map.of(
            "dakota_baby", "dakotaraptor",
            "sino_baby", "sinosaurus",
            "oxa_baby", "oxalaia",
            "latenivenatrix_baby", "latenivenatrix",
            "charo_baby", "charonosaurus",
            "coahuilababy", "coahuilasaurus"
    );
    private static final Set<String> NATIVE_TAMEABLES = Set.of(
            "dakotaraptor", "sinosaurus", "oxalaia", "charonosaurus", "coahuilasaurus"
    );
    /** Diets stated clearly by Similar Prehistory's bundled encyclopedia text. */
    private static final Map<String, CreatureDiet> OFFICIAL_DIETS = Map.of(
            "charonosaurus", CreatureDiet.HERBIVORE,
            "dakotaraptor", CreatureDiet.CARNIVORE,
            "latenivenatrix", CreatureDiet.CARNIVORE,
            "oxalaia", CreatureDiet.CARNIVORE
    );

    @Override
    public String id() {
        return "tl_domesticate_more_creatures:profile/similar_prehistory";
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
    public ResourceLocation canonicalEntityTypeId(ResourceLocation entityTypeId) {
        if (!supports(entityTypeId)) {
            return entityTypeId;
        }
        String adultPath = BABY_TO_ADULT.get(entityTypeId.getPath());
        if (adultPath == null) {
            return entityTypeId;
        }
        ResourceLocation adultId = ResourceLocation.tryBuild(MOD_ID, adultPath);
        return adultId == null ? entityTypeId : adultId;
    }

    public ResourceLocation representativeEntityTypeId(ResourceLocation entityTypeId) {
        return canonicalEntityTypeId(entityTypeId);
    }

    @Override
    public CreatureCompatProfile resolve(CreatureCompatProfileContext context) {
        if (context == null || !supports(context.entityTypeId())) {
            return CreatureCompatProfile.EMPTY;
        }
        ResourceLocation canonical = canonicalEntityTypeId(context.entityTypeId());
        ResourceLocation representative = representativeEntityTypeId(context.entityTypeId());
        String adultPath = canonical == null ? context.entityTypeId().getPath() : canonical.getPath();
        TamingInfo nativeTaming = NATIVE_TAMEABLES.contains(adultPath)
                ? new TamingInfo(true, "SIMILAR_NATIVE", 1, List.of())
                : TamingInfo.NOT_TAMEABLE;
        CreatureDiet diet = OFFICIAL_DIETS.getOrDefault(adultPath, CreatureDiet.UNKNOWN);
        Component description = officialLore(adultPath)
                .<Component>map(Component::literal)
                .orElse(Component.empty());
        return new CreatureCompatProfile(
                canonical,
                representative,
                diet,
                Component.translatable("compat.tl_domesticate_more_creatures.species.dinosaur"),
                description,
                nativeTaming
        );
    }

    private static Optional<String> officialLore(String adultPath) {
        if (adultPath == null || adultPath.isBlank()) {
            return Optional.empty();
        }
        // Similar 2.1.1 bundles its encyclopedia under the legacy unusualprehistory resource namespace.
        return ModCompatResources.readUtf8(
                        MOD_ID,
                        "assets/unusualprehistory/encyclopedia/en_us/" + adultPath + ".txt"
                )
                .map(SimilarPrehistoryCompatProfileProvider::cleanLore)
                .filter(text -> !text.isBlank());
    }

    private static String cleanLore(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.replace("<NEWLINE>", "\n")
                .replace("\r", "")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }
}
