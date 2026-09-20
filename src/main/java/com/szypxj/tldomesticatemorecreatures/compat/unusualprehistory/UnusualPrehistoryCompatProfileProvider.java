package com.szypxj.tldomesticatemorecreatures.compat.unusualprehistory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.szypxj.tldomesticatemorecreatures.api.creature.TamingInfo;
import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureCompatProfile;
import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureCompatProfileContext;
import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureCompatProfileProvider;
import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureDiet;
import com.szypxj.tldomesticatemorecreatures.compat.ModCompatResources;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Reads Unusual Prehistory 2's Paleopedia as the authoritative lore source. */
public final class UnusualPrehistoryCompatProfileProvider implements CreatureCompatProfileProvider {
    private static final String MOD_ID = "unusual_prehistory";
    private static final ConcurrentHashMap<String, Optional<Lore>> LORE_CACHE = new ConcurrentHashMap<>();

    @Override public String id() { return "tl_domesticate_more_creatures:profile/unusual_prehistory"; }
    @Override public int priority() { return 840; }
    @Override public boolean supports(ResourceLocation id) { return id != null && MOD_ID.equals(id.getNamespace()); }

    @Override
    public CreatureCompatProfile resolve(CreatureCompatProfileContext context) {
        if (context == null || !supports(context.entityTypeId())) return CreatureCompatProfile.EMPTY;
        String path = context.entityTypeId().getPath();
        Lore localized = lore(path, context.languageCode()).orElse(null);
        // Diet classification intentionally uses the English official entry so translations cannot change semantics.
        CreatureDiet diet = lore(path, "en_us").map(Lore::diet).orElse(CreatureDiet.UNKNOWN);
        Component description = localized == null || localized.description().isBlank()
                ? Component.empty() : Component.literal(localized.description());
        Component species = "living_ooze".equals(path)
                ? Component.empty()
                : Component.translatable("compat.tl_domesticate_more_creatures.species.prehistoric_creature");
        TamingInfo taming = "ulughbegsaurus".equals(path)
                ? new TamingInfo(true, "UNUSUAL_NATIVE", 1, List.of())
                : TamingInfo.NOT_TAMEABLE;
        return new CreatureCompatProfile(context.entityTypeId(), context.entityTypeId(), diet, species, description, taming);
    }

    private static Optional<Lore> lore(String entityPath, String language) {
        String key = language + "|" + entityPath;
        return LORE_CACHE.computeIfAbsent(key, ignored -> loadLore(entityPath, language));
    }

    private static Optional<Lore> loadLore(String entityPath, String language) {
        Optional<String> raw = ModCompatResources.readLocalized(
                MOD_ID,
                language,
                lang -> "assets/unusual_prehistory/patchouli_books/paleopedia/" + lang
                        + "/entries/mobs/" + entityPath + ".json"
        );
        if (raw.isEmpty() && "living_ooze".equals(entityPath)) {
            raw = ModCompatResources.readLocalized(
                    MOD_ID,
                    language,
                    lang -> "assets/unusual_prehistory/patchouli_books/paleopedia/" + lang
                            + "/entries/features/mobs/living_ooze.json"
            );
        }
        if (raw.isEmpty()) return Optional.empty();
        try {
            JsonObject root = JsonParser.parseString(raw.get()).getAsJsonObject();
            String description = "";
            if (root.has("pages") && root.get("pages").isJsonArray()) {
                for (var pageElement : root.getAsJsonArray("pages")) {
                    if (!pageElement.isJsonObject()) continue;
                    JsonObject page = pageElement.getAsJsonObject();
                    if (page.has("image_tooltip") && page.get("image_tooltip").isJsonPrimitive()) {
                        description = page.get("image_tooltip").getAsString().trim();
                        if (!description.isBlank()) break;
                    }
                    if (page.has("text") && page.get("text").isJsonPrimitive() && description.isBlank()) {
                        description = page.get("text").getAsString().trim();
                    }
                }
            }
            return Optional.of(new Lore(description, explicitDiet(raw.get())));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static CreatureDiet explicitDiet(String officialJson) {
        String text = officialJson == null ? "" : officialJson.toLowerCase(Locale.ROOT);
        boolean herb = text.contains("herbivore") || text.contains("herbivorous");
        boolean omni = text.contains("omnivore") || text.contains("omnivorous");
        boolean carn = text.contains("carnivore") || text.contains("carnivorous") || text.contains("meat-eating");
        boolean scav = text.contains("scavenger") || text.contains("scavenging");
        int hits = (herb ? 1 : 0) + (omni ? 1 : 0) + (carn ? 1 : 0) + (scav ? 1 : 0);
        if (hits != 1) return CreatureDiet.UNKNOWN;
        if (herb) return CreatureDiet.HERBIVORE;
        if (omni) return CreatureDiet.OMNIVORE;
        if (carn) return CreatureDiet.CARNIVORE;
        return CreatureDiet.SCAVENGER;
    }

    private record Lore(String description, CreatureDiet diet) {}
}
