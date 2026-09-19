package com.szypxj.tldomesticatemorecreatures.compat.saintsdragons;

import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CombatThreatChannel;
import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CreatureThreatProfile;
import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CreatureThreatProvider;
import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CreatureThreatProviderRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads Saint's Dragons' own DragonAttributeConfig at runtime, including independent ability damage values.
 * No Saint's Dragons classes are linked at compile time.
 */
public final class SaintsDragonsThreatProvider implements CreatureThreatProvider {
    private static final String MOD_ID = "saintsdragons";
    private static final String CONFIG_LOADER = "com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader";
    private static final Set<String> CONFIGURED_DRAGONS = Set.of(
            "cindervane",
            "raevyx",
            "varasuchus",
            "ignivorus",
            "stegonaut",
            "volitans",
            "nulljaw",
            "atroxiia",
            "draconian_swarm"
    );

    public static void register() {
        CreatureThreatProviderRegistry.register(new SaintsDragonsThreatProvider());
    }

    private SaintsDragonsThreatProvider() {
    }

    @Override
    public String id() {
        return "tl_domesticate_more_creatures:threat/saintsdragons";
    }

    @Override
    public int priority() {
        return 800;
    }

    @Override
    public boolean supports(ResourceLocation entityTypeId) {
        return entityTypeId != null
                && MOD_ID.equals(entityTypeId.getNamespace())
                && CONFIGURED_DRAGONS.contains(entityTypeId.getPath());
    }

    @Override
    public CreatureThreatProfile liveProfile(LivingEntity entity, CreatureThreatProfile fallback) {
        ResourceLocation id = entity == null
                ? null
                : net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        ConfigView config = readConfig(id);
        if (config == null) {
            return fallback;
        }
        List<CombatThreatChannel> channels = buildChannels(id, config);
        return new CreatureThreatProfile(
                // Keep the current instance's health for live scanning; this avoids turning juveniles into adults.
                fallback.maxHealth() > 0.0D ? fallback.maxHealth() : config.maxHealth,
                Math.max(fallback.movementSpeed(), normalizedFlyingSpeed(config.flyingSpeed)),
                fallback.meleeDamage(),
                channels,
                liveFormKey(entity)
        );
    }

    @Override
    public CreatureThreatProfile representativeProfile(
            EntityType<?> type,
            ResourceLocation entityTypeId,
            CreatureThreatProfile fallback
    ) {
        ConfigView config = readConfig(entityTypeId);
        if (config == null) {
            return fallback;
        }
        return new CreatureThreatProfile(
                config.maxHealth > 0.0D ? config.maxHealth : fallback.maxHealth(),
                Math.max(fallback.movementSpeed(), normalizedFlyingSpeed(config.flyingSpeed)),
                fallback.meleeDamage(),
                buildChannels(entityTypeId, config),
                "saintsdragons:adult"
        );
    }

    private static List<CombatThreatChannel> buildChannels(ResourceLocation id, ConfigView config) {
        List<CombatThreatChannel> channels = new ArrayList<>();
        for (Map.Entry<String, AbilityView> entry : config.abilities.entrySet()) {
            AbilityView ability = entry.getValue();
            if (ability == null || !ability.enabled || ability.damage <= 0.0D) {
                continue;
            }
            String key = entry.getKey();
            double targets = inferredTargets(key);
            double control = inferredControl(key, ability.stunTicks);
            channels.add(new CombatThreatChannel(
                    "saintsdragons:" + key,
                    ability.damage,
                    ability.damage,
                    0.0D,
                    targets,
                    control
            ));
        }

        // Some damage is intentionally stored in the mod's extra configuration rather than the ability map.
        for (Map.Entry<String, Double> entry : config.extraDoubles.entrySet()) {
            String key = entry.getKey();
            double damage = entry.getValue() == null ? 0.0D : entry.getValue();
            if (!isIndependentDamageExtra(key) || damage <= 0.0D) {
                continue;
            }
            channels.add(new CombatThreatChannel(
                    "saintsdragons:" + key,
                    damage,
                    damage,
                    0.0D,
                    inferredTargets(key),
                    inferredControl(key, 0.0D)
            ));
        }

        // Raevyx roar uses a hard-coded 5 raw lightning damage per strike in the native ability and therefore
        // is not represented by DragonAttributeConfig. Treat it as an AOE/control channel, without applying
        // the temporary supercharged double-strike state to the species representative.
        if (id != null && "raevyx".equals(id.getPath())) {
            channels.add(new CombatThreatChannel(
                    "saintsdragons:raevyx_roar_lightning",
                    5.0D,
                    15.0D,
                    0.0D,
                    3.0D,
                    0.80D
            ));
        }
        return channels;
    }

    private static ConfigView readConfig(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        try {
            Class<?> loaderClass = Class.forName(CONFIG_LOADER);
            Object loader = loaderClass.getMethod("getInstance").invoke(null);
            Object config = loaderClass.getMethod("getConfig", ResourceLocation.class).invoke(loader, id);
            if (config == null) {
                return null;
            }
            Class<?> configClass = config.getClass();
            double maxHealth = number(configClass.getMethod("maxHealth").invoke(config));
            double flyingSpeed = number(configClass.getMethod("flyingSpeed").invoke(config));

            Map<String, AbilityView> abilities = new java.util.LinkedHashMap<>();
            Object rawAbilities = configClass.getMethod("abilities").invoke(config);
            if (rawAbilities instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!(entry.getKey() instanceof String key) || entry.getValue() == null) {
                        continue;
                    }
                    Object override = entry.getValue();
                    Class<?> overrideClass = override.getClass();
                    double damage = nullableNumber(overrideClass.getMethod("damage").invoke(override));
                    double stunTicks = nullableNumber(overrideClass.getMethod("stunDurationTicks").invoke(override));
                    Object enabledValue = overrideClass.getMethod("enabled").invoke(override);
                    boolean enabled = !(enabledValue instanceof Boolean flag) || flag;
                    abilities.put(key, new AbilityView(damage, stunTicks, enabled));
                }
            }

            Map<String, Double> extras = new java.util.LinkedHashMap<>();
            Object rawExtras = configClass.getMethod("extraDoubles").invoke(config);
            if (rawExtras instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String key && entry.getValue() instanceof Number value) {
                        extras.put(key, Math.max(0.0D, value.doubleValue()));
                    }
                }
            }
            return new ConfigView(maxHealth, flyingSpeed, Map.copyOf(abilities), Map.copyOf(extras));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    private static String liveFormKey(LivingEntity entity) {
        if (entity == null) {
            return "";
        }
        try {
            Method isBaby = entity.getClass().getMethod("isBaby");
            Object value = isBaby.invoke(entity);
            if (value instanceof Boolean flag) {
                return flag ? "saintsdragons:juvenile" : "saintsdragons:adult";
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return "";
    }

    private static boolean isIndependentDamageExtra(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("damage")
                && !normalized.contains("self_damage")
                && !normalized.contains("multiplier")
                && !normalized.contains("reduction")
                && !normalized.contains("threshold");
    }

    private static double inferredTargets(String key) {
        String normalized = key == null ? "" : key.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("explosion") || normalized.contains("volley") || normalized.contains("storm")
                || normalized.contains("slam") || normalized.contains("roar") || normalized.contains("swipe")) {
            return 4.0D;
        }
        if (normalized.contains("beam") || normalized.contains("breath")) {
            return 2.0D;
        }
        return 1.0D;
    }

    private static double inferredControl(String key, double stunTicks) {
        String normalized = key == null ? "" : key.toLowerCase(java.util.Locale.ROOT);
        double control = stunTicks > 0.0D ? Math.min(1.0D, stunTicks / 60.0D) : 0.0D;
        if (normalized.contains("grab") || normalized.contains("stun") || normalized.contains("roar")) {
            control = Math.max(control, 0.60D);
        }
        return control;
    }

    private static double normalizedFlyingSpeed(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    private static double number(Object value) {
        return value instanceof Number number && Double.isFinite(number.doubleValue())
                ? Math.max(0.0D, number.doubleValue())
                : 0.0D;
    }

    private static double nullableNumber(Object value) {
        return value == null ? 0.0D : number(value);
    }

    private record AbilityView(double damage, double stunTicks, boolean enabled) {
    }

    private record ConfigView(
            double maxHealth,
            double flyingSpeed,
            Map<String, AbilityView> abilities,
            Map<String, Double> extraDoubles
    ) {
    }
}
