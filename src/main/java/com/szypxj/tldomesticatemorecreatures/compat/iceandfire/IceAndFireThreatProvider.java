package com.szypxj.tldomesticatemorecreatures.compat.iceandfire;

import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CombatThreatChannel;
import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CreatureThreatProfile;
import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CreatureThreatProvider;
import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CreatureThreatProviderRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Stage-aware Ice and Fire threat bridge. It intentionally uses reflection so the mod stays optional.
 */
public final class IceAndFireThreatProvider implements CreatureThreatProvider {
    private static final String MOD_ID = "iceandfire";
    private static final String CONFIG_CLASS = "com.github.alexthe666.iceandfire.IafConfig";
    private static final Set<String> DRAGONS = Set.of(
            "fire_dragon",
            "ice_dragon",
            "lightning_dragon",
            "black_frost_dragon"
    );

    // Native Ice and Fire treats stage 3+ as adult. Stage 3 spans the middle of the 0..125 day interpolation,
    // so 62 days is used only for the species-level TCB representative. Live TDMC scans never use this value.
    private static final int REPRESENTATIVE_ADULT_STAGE = 3;
    private static final double REPRESENTATIVE_ADULT_AGE_DAYS = 62.0D;
    private static final double MAX_ATTRIBUTE_AGE_DAYS = 125.0D;

    public static void register() {
        CreatureThreatProviderRegistry.register(new IceAndFireThreatProvider());
    }

    private IceAndFireThreatProvider() {
    }

    @Override
    public String id() {
        return "tl_domesticate_more_creatures:threat/iceandfire";
    }

    @Override
    public int priority() {
        return 800;
    }

    @Override
    public boolean supports(ResourceLocation entityTypeId) {
        return entityTypeId != null
                && MOD_ID.equals(entityTypeId.getNamespace())
                && DRAGONS.contains(entityTypeId.getPath());
    }

    @Override
    public CreatureThreatProfile liveProfile(LivingEntity entity, CreatureThreatProfile fallback) {
        if (entity == null) {
            return fallback;
        }
        int stage = reflectedStage(entity);
        if (stage <= 0) {
            return fallback;
        }

        ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        List<CombatThreatChannel> channels = new ArrayList<>(fallback.combatChannels());
        addBreathChannel(channels, id == null ? "" : id.getPath(), stage);
        return new CreatureThreatProfile(
                fallback.maxHealth(),
                fallback.movementSpeed(),
                fallback.meleeDamage(),
                channels,
                "iceandfire:stage_" + stage
        );
    }

    @Override
    public CreatureThreatProfile representativeProfile(
            EntityType<?> type,
            ResourceLocation entityTypeId,
            CreatureThreatProfile fallback
    ) {
        if (entityTypeId == null) {
            return fallback;
        }

        // The three elemental dragons share the same native age interpolation. Black Frost is kept stage-aware
        // for live scans but falls back here because its representative combat formula is not exposed as one of
        // the standard elemental dragon configurations.
        String path = entityTypeId.getPath();
        if (!path.equals("fire_dragon") && !path.equals("ice_dragon") && !path.equals("lightning_dragon")) {
            return new CreatureThreatProfile(
                    fallback.maxHealth(), fallback.movementSpeed(), fallback.meleeDamage(),
                    fallback.combatChannels(), "iceandfire:adult"
            );
        }

        double maximumHealth = reflectedConfigNumber("dragonHealth", fallback.maxHealth());
        double maximumAttack = 1.0D + reflectedConfigNumber("dragonAttackDamage", Math.max(0.0D, fallback.meleeDamage() - 1.0D));
        double health = interpolate(maximumHealth * 0.04D, maximumHealth, REPRESENTATIVE_ADULT_AGE_DAYS);
        double melee = Math.round(interpolate(1.0D, maximumAttack, REPRESENTATIVE_ADULT_AGE_DAYS));
        double movement = interpolate(0.15D, 0.40D, REPRESENTATIVE_ADULT_AGE_DAYS);

        List<CombatThreatChannel> channels = new ArrayList<>();
        addBreathChannel(channels, path, REPRESENTATIVE_ADULT_STAGE);
        return new CreatureThreatProfile(
                health,
                movement,
                melee,
                channels,
                "iceandfire:adult_stage_3"
        );
    }

    private static void addBreathChannel(List<CombatThreatChannel> channels, String path, int stage) {
        String configField;
        double control;
        if ("fire_dragon".equals(path)) {
            configField = "dragonAttackDamageFire";
            control = 0.25D;
        } else if ("ice_dragon".equals(path)) {
            configField = "dragonAttackDamageIce";
            control = 0.70D;
        } else if ("lightning_dragon".equals(path)) {
            configField = "dragonAttackDamageLightning";
            control = 0.55D;
        } else {
            return;
        }
        double scale = reflectedConfigNumber(configField, 0.0D);
        double rawDamage = Math.max(0.0D, stage) * scale;
        if (rawDamage <= 0.0D) {
            return;
        }
        // Native breath damages an area and elemental variants add status/chain effects. Keep the AOE/control
        // bonuses bounded in CombatThreatChannel rather than adding all targets as if one target took every hit.
        channels.add(new CombatThreatChannel(
                "iceandfire:dragon_breath",
                rawDamage,
                rawDamage,
                0.0D,
                4.0D,
                control
        ));
    }

    private static int reflectedStage(LivingEntity entity) {
        try {
            Method method = entity.getClass().getMethod("getDragonStage");
            Object value = method.invoke(entity);
            return value instanceof Number number ? Math.max(0, number.intValue()) : 0;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 0;
        }
    }

    private static double reflectedConfigNumber(String fieldName, double fallback) {
        try {
            Class<?> config = Class.forName(CONFIG_CLASS);
            Field field = config.getField(fieldName);
            Object value = field.get(null);
            if (value instanceof Number number && Double.isFinite(number.doubleValue())) {
                return Math.max(0.0D, number.doubleValue());
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
        return Math.max(0.0D, fallback);
    }

    private static double interpolate(double minimum, double maximum, double ageDays) {
        double age = Math.max(0.0D, Math.min(MAX_ATTRIBUTE_AGE_DAYS, ageDays));
        return minimum + (maximum - minimum) * (age / MAX_ATTRIBUTE_AGE_DAYS);
    }
}
