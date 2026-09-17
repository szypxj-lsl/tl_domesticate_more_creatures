package com.szypxj.tldomesticatemorecreatures.network;

import com.szypxj.tldomesticatemorecreatures.api.attribute.DynamicAttributeDisplayRegistry;
import com.szypxj.tldomesticatemorecreatures.backpack.PetBackpackService;
import com.szypxj.tldomesticatemorecreatures.api.attribute.DynamicAttributeDisplayValue;
import com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeDefinition;
import com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeOperations;
import com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeRegistry;
import com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeValue;
import com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeValueFormat;
import com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeValueStore;
import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStats;
import com.szypxj.tldomesticatemorecreatures.api.creature.CreatureInfoApi;
import com.szypxj.tldomesticatemorecreatures.attribute.TdmcBuiltinAttributes;
import com.szypxj.tldomesticatemorecreatures.config.StatConfigManager;
import com.szypxj.tldomesticatemorecreatures.config.StatDefinition;
import com.szypxj.tldomesticatemorecreatures.config.TalentConfigManager;
import com.szypxj.tldomesticatemorecreatures.config.TalentDefinition;
import com.szypxj.tldomesticatemorecreatures.config.SpecialTalentConfigManager;
import com.szypxj.tldomesticatemorecreatures.config.SpecialTalentDefinition;
import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import com.szypxj.tldomesticatemorecreatures.elite.EliteService;
import com.szypxj.tldomesticatemorecreatures.game.AttributeService;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.game.genetics.ChildGeneticsResult;
import com.szypxj.tldomesticatemorecreatures.equipment.PetEquipmentService;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchGeneticPayload;
import com.szypxj.tldomesticatemorecreatures.imprint.ImprintService;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingFoodDisplay;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingMethod;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingRule;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingRuleManager;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingService;
import com.szypxj.tldomesticatemorecreatures.domestication.DomesticationData;
import com.szypxj.tldomesticatemorecreatures.riding.NativeRideCapabilityResolver;
import com.szypxj.tldomesticatemorecreatures.riding.RideCapabilityResolver;
import com.szypxj.tldomesticatemorecreatures.riding.RideMode;
import com.szypxj.tldomesticatemorecreatures.riding.capability.RideCapabilityProfile;
import com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingConfigManager;
import com.szypxj.tldomesticatemorecreatures.torpor.TorporService;
import com.szypxj.tldomesticatemorecreatures.talent.FuryService;
import com.szypxj.tldomesticatemorecreatures.talent.SpecialTalentService;
import com.szypxj.tldomesticatemorecreatures.talent.TalentMigrationService;
import com.szypxj.tldomesticatemorecreatures.spyglass.SpyglassRadarBaseline;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SnapshotFactory {
    private static final String MAX_HEALTH_TARGET = "minecraft:generic.max_health";

    private SnapshotFactory() {
    }

    public static PanelSnapshot panel(ServerPlayer viewer, LivingEntity target) {
        LevelService.initializeIfNeeded(target);
        SpecialTalentService.ensureTalents(target);
        ProgressData data = ProgressData.of(target);
        boolean owner = target == viewer || PetOwnershipService.isOwnedBy(target, viewer);
        List<PanelAttributeEntry> attributes = new ArrayList<>();
        int legacyOrder = 0;
        for (StatDefinition definition : AttributeService.definitionsFor(target)) {
            String legacyFormat = displayFormat(definition);
            TdmcAttributeDefinition publicDefinition = TdmcBuiltinAttributes.ensureRegistered(
                    definition,
                    legacyOrder++,
                    TdmcAttributeValueFormat.fromWireName(legacyFormat)
            );
            if (publicDefinition == null
                    || !publicDefinition.flags().visible()
                    || !publicDefinition.appliesTo(target)
                    || !AttributeService.isVisible(target, definition)) {
                continue;
            }
            ProgressData.StatPoints points = data.stat(definition.id());
            boolean available = AttributeService.isTargetAvailable(target, definition);
            boolean pointAssignable = publicDefinition.flags().upgradeable() && AttributeService.isPointAssignable(definition);
            boolean withinMax = pointAssignable && AttributeService.canReceivePoint(target, definition, points.regular());
            DynamicAttributeDisplayValue dynamicValue = panelDisplayValue(viewer, target, definition);
            StatSnapshot snapshot = new StatSnapshot(
                    definition.id(),
                    publicDefinition.nameKey(),
                    publicDefinition.icon(),
                    points.total(),
                    points.wild(),
                    points.trained(),
                    points.talent(),
                    AttributeService.displayedValue(target, definition),
                    publicDefinition.valueFormat().wireName(),
                    available,
                    pointAssignable && owner && data.unspentPoints() > 0 && available && withinMax,
                    AttributeService.pointLimit(target, definition),
                    dynamicValue != null,
                    dynamicValue == null ? currentValue(target, definition) : dynamicValue.current(),
                    dynamicValue == null ? maxValue(target, definition) : dynamicValue.max()
            );
            attributes.add(new PanelAttributeEntry(
                    builtinDisplayOrder(definition.id(), publicDefinition.displayOrder()),
                    publicDefinition.id().toString(),
                    snapshot
            ));
        }
        attributes.addAll(customAttributes(viewer, target, owner));
        attributes.sort(Comparator
                .comparingInt(PanelAttributeEntry::displayOrder)
                .thenComparing(PanelAttributeEntry::sortId));
        List<StatSnapshot> stats = attributes.stream().map(PanelAttributeEntry::snapshot).toList();

        long required = data.level() >= data.maxLevel() ? 0L : LevelService.requiredExperience(target, data.level() + 1);
        return new PanelSnapshot(
                target.getId(),
                target.getDisplayName(),
                target instanceof net.minecraft.world.entity.player.Player,
                data.tamed(),
                owner,
                EliteService.isDisplayElite(target),
                data.initialLevel(),
                data.level(),
                data.maxLevel(),
                data.experience(),
                required,
                data.unspentPoints(),
                rideStatus(target),
                data.tamed() && DomesticationData.of(target).allowOtherRiders(),
                petEquipment(target, owner),
                PetBackpackService.available(viewer, target),
                PetBackpackService.editable(viewer, target),
                ImprintService.snapshot(target),
                talents(data),
                fury(target),
                List.copyOf(stats)
        );
    }

    private static List<PanelAttributeEntry> customAttributes(ServerPlayer viewer, LivingEntity target, boolean owner) {
        List<PanelAttributeEntry> result = new ArrayList<>();
        ProgressData data = ProgressData.of(target);
        for (TdmcAttributeDefinition definition : TdmcAttributeRegistry.definitions()) {
            if (TdmcBuiltinAttributes.isBuiltin(definition.id())
                    || !definition.flags().visible()
                    || !definition.appliesTo(target)) {
                continue;
            }
            TdmcAttributeValue value = TdmcAttributeOperations.value(target, definition.id()).orElse(null);
            if (value == null) {
                continue;
            }
            int allocatedPoints = TdmcAttributeValueStore.allocatedPoints(target, definition.id());
            boolean canAllocate = owner
                    && data.unspentPoints() > 0
                    && TdmcAttributeOperations.canAllocate(target, definition.id(), 1);
            int maxPoints = pointLimit(definition);
            DynamicAttributeDisplayValue dynamicDisplay = definition.flags().dynamicDisplay()
                    ? DynamicAttributeDisplayRegistry.resolve(definition.id(), viewer, target).orElse(null)
                    : null;
            double currentValue = dynamicDisplay == null ? value.current() : dynamicDisplay.current();
            double maxValue = dynamicDisplay == null ? value.max() : dynamicDisplay.max();
            StatSnapshot snapshot = new StatSnapshot(
                    definition.id().toString(),
                    definition.nameKey(),
                    definition.icon(),
                    allocatedPoints,
                    0,
                    allocatedPoints,
                    0,
                    value.current(),
                    definition.valueFormat().wireName(),
                    true,
                    canAllocate,
                    maxPoints,
                    definition.flags().dynamicDisplay(),
                    currentValue,
                    maxValue
            );
            result.add(new PanelAttributeEntry(
                    definition.displayOrder(),
                    definition.id().toString(),
                    snapshot
            ));
        }
        return result;
    }

    private static int builtinDisplayOrder(String statId, int fallback) {
        return switch (statId) {
            case "health" -> 100;
            case "speed" -> 500;
            case "swim_speed" -> 600;
            case "damage" -> 700;
            case "resistance" -> 800;
            case "torpor" -> 1000;
            default -> fallback;
        };
    }

    private static int pointLimit(TdmcAttributeDefinition definition) {
        if (!definition.flags().upgradeable()
                || !Double.isFinite(definition.maxValue())
                || !Double.isFinite(definition.defaultValue())
                || !Double.isFinite(definition.pointIncrement())
                || definition.pointIncrement() <= 0.0D) {
            return 0;
        }
        double range = Math.max(0.0D, definition.maxValue() - definition.defaultValue());
        if (range >= (double) Integer.MAX_VALUE * definition.pointIncrement()) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, (int) Math.floor(range / definition.pointIncrement()));
    }

    private static PetEquipmentSnapshot petEquipment(LivingEntity target, boolean owner) {
        if (target instanceof net.minecraft.world.entity.player.Player || !PetOwnershipService.isTamed(target)) {
            return PetEquipmentSnapshot.NONE;
        }
        return new PetEquipmentSnapshot(
                true,
                owner,
                PetEquipmentService.slots(target).stream().map(PetEquipmentSlotSnapshot::of).toList()
        );
    }

    public static InspectSnapshot inspect(ServerPlayer viewer, LivingEntity target) {
        LevelService.initializeIfNeeded(target);
        SpecialTalentService.ensureTalents(target);
        ProgressData data = ProgressData.of(target);
        InspectSnapshot.RadarSnapshot radar = radar(target);
        String tamingMethodKey = "";
        List<InspectSnapshot.TamingFoodSnapshot> tamingFoods = List.of();
        boolean tamingActive = false;
        double tamingProgress = 0.0D;
        boolean torporAvailable = !PetOwnershipService.isTamed(target) && TorporService.isEnabled(target);
        double maxTorpor = torporAvailable ? TorporService.maxTorpor(target) : 0.0D;
        torporAvailable = torporAvailable && maxTorpor > 0.0D;
        double torpor = torporAvailable ? TorporService.currentTorpor(target) : 0.0D;
        int requiredTamingLevel = 0;

        if ((!EliteService.isElite(target) || EliteService.canBeTamed(target)) && !PetOwnershipService.isTamed(target)) {
            var rule = TamingRuleManager.ruleFor(target);
            if (rule.isPresent()) {
                TamingRule tamingRule = rule.get();
                tamingMethodKey = tamingRule.method() == TamingMethod.KNOCKOUT
                        ? "spyglass.tl_domesticate_more_creatures.taming_method_knockout"
                        : "spyglass.tl_domesticate_more_creatures.taming_method_feeding";
                List<InspectSnapshot.TamingFoodSnapshot> foods = new ArrayList<>();
                for (TamingFoodDisplay food : TamingRuleManager.displayFoodsFor(target)) {
                    Item item = ForgeRegistries.ITEMS.getValue(food.itemId());
                    if (item != null) {
                        foods.add(new InspectSnapshot.TamingFoodSnapshot(
                                item.getDescriptionId(),
                                food.amount(),
                                food.configured()
                        ));
                    }
                }
                tamingFoods = List.copyOf(foods);
                tamingActive = DomesticationData.of(target).tamingPlayerUuid().isPresent();
                tamingProgress = TamingService.tamingProgress(target);
                LevelService.initializeIfNeeded(viewer);
                int playerLevel = ProgressData.of(viewer).level();
                int requiredPlayerLevel = tamingRule.requiredPlayerLevel();
                if (playerLevel < requiredPlayerLevel) {
                    requiredTamingLevel = requiredPlayerLevel;
                }
            }
        }

        ResourceLocation entityTypeId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        List<InspectSnapshot.InspectStat> inspectStats = inspectStats(viewer, target);
        return new InspectSnapshot(
                InspectTarget.entity(target.getId()),
                target.getDisplayName(),
                ownerName(target),
                entityTypeId,
                true,
                CreatureInfoApi.getDangerRatingStats(target),
                data.level(),
                EliteService.isDisplayElite(target),
                false,
                false,
                data.paternalMutations(),
                data.maternalMutations(),
                false,
                false,
                rideStatus(target),
                ImprintService.snapshot(target),
                talents(data),
                fury(target),
                radar,
                inspectStats,
                tamingMethodKey,
                tamingFoods,
                tamingActive,
                tamingProgress,
                torporAvailable,
                torpor,
                maxTorpor,
                requiredTamingLevel
        );
    }

    public static InspectSnapshot inspectBasic(LivingEntity target) {
        ResourceLocation entityTypeId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        return new InspectSnapshot(
                InspectTarget.entity(target.getId()),
                target.getDisplayName(),
                ownerName(target),
                entityTypeId,
                false,
                CreatureInfoApi.getDangerRatingStats(target),
                0,
                false,
                false,
                false,
                0,
                0,
                false,
                false,
                RideStatusSnapshot.NONE,
                ImprintSnapshot.NONE,
                List.of(),
                FurySnapshot.NONE,
                InspectSnapshot.RadarSnapshot.NONE,
                basicInspectStats(target),
                "",
                List.of(),
                false,
                0.0D,
                false,
                0.0D,
                0.0D,
                0
        );
    }

    public static InspectSnapshot inspectEgg(InspectTarget target, Component carrierName, HatchGeneticPayload payload) {
        Component name = carrierName == null
                ? Component.translatable("spyglass.tl_domesticate_more_creatures.egg_unknown")
                : carrierName;
        if (payload == null || !payload.hasResolvedResult() || !LevelService.isAffectedEntityId(payload.childEntityId())) {
            return emptyEgg(target, name);
        }
        ResourceLocation childId = ResourceLocation.tryParse(payload.childEntityId());
        ChildGeneticsResult result = payload.result();
        if (childId == null || result == null) {
            return emptyEgg(target, name);
        }

        Map<String, Integer> resolvedTalents = TalentMigrationService.migrateLegacyTalents(
                TalentMigrationService.stableId(payload.seed(), payload.childEntityId()),
                result.talents()
        );
        Map<String, Integer> talentPoints = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : resolvedTalents.entrySet()) {
            TalentDefinition definition = TalentConfigManager.byId(entry.getKey());
            if (definition == null) {
                continue;
            }
            int talentLevel = definition.clampLevel(entry.getValue());
            for (Map.Entry<String, Integer> effect : definition.effectsFor(talentLevel).entrySet()) {
                if (effect.getValue() > 0
                        && !TorporService.STAT_ID.equals(effect.getKey())) {
                    talentPoints.merge(effect.getKey(), effect.getValue(), Integer::sum);
                }
            }
        }

        List<InspectSnapshot.InspectStat> stats = new ArrayList<>();
        for (StatDefinition definition : AttributeService.definitionsFor(childId)) {
            if (!definition.showMob() || !AttributeService.isPointAssignable(definition)) {
                continue;
            }
            int points = Math.max(0, result.wildStat(definition.id()))
                    + Math.max(0, talentPoints.getOrDefault(definition.id(), 0));
            stats.add(new InspectSnapshot.InspectStat(
                    definition.id(),
                    definition.nameKey(),
                    points,
                    points,
                    "NUMBER",
                    0.0D,
                    0.0D
            ));
        }

        net.minecraft.world.entity.EntityType<?> childType = ForgeRegistries.ENTITY_TYPES.getValue(childId);
        return new InspectSnapshot(
                target,
                name,
                Component.empty(),
                childId,
                true,
                CreatureInfoApi.getBaseStats(childType),
                result.initialLevel(),
                false,
                true,
                true,
                result.paternalMutations(),
                result.maternalMutations(),
                result.mutated(),
                result.ownerUuid() != null,
                RideStatusSnapshot.NONE,
                ImprintSnapshot.NONE,
                talents(resolvedTalents, result.specialTalents()),
                FurySnapshot.NONE,
                InspectSnapshot.RadarSnapshot.NONE,
                List.copyOf(stats),
                "",
                List.of(),
                false,
                0.0D,
                false,
                0.0D,
                0.0D,
                0
        );
    }

    public static InspectSnapshot emptyEgg(InspectTarget target, Component name) {
        return new InspectSnapshot(
                target,
                name == null ? Component.translatable("spyglass.tl_domesticate_more_creatures.egg_unknown") : name,
                Component.empty(),
                null,
                false,
                BaseStats.NONE,
                0,
                false,
                true,
                false,
                0,
                0,
                false,
                false,
                RideStatusSnapshot.NONE,
                ImprintSnapshot.NONE,
                List.of(),
                FurySnapshot.NONE,
                InspectSnapshot.RadarSnapshot.NONE,
                List.of(),
                "",
                List.of(),
                false,
                0.0D,
                false,
                0.0D,
                0.0D,
                0
        );
    }

    private static Component ownerName(LivingEntity target) {
        return PetOwnershipService.ownerPlayer(target)
                .map(Player::getDisplayName)
                .orElse(Component.empty());
    }

    private static List<InspectSnapshot.InspectStat> inspectStats(ServerPlayer viewer, LivingEntity target) {
        return panel(viewer, target).stats().stream()
                .filter(StatSnapshot::available)
                .map(SnapshotFactory::inspectStat)
                .toList();
    }

    private static InspectSnapshot.InspectStat inspectStat(StatSnapshot stat) {
        return new InspectSnapshot.InspectStat(
                stat.id(),
                stat.nameKey(),
                stat.totalPoints(),
                stat.displayedValue(),
                stat.displayFormat(),
                stat.currentValue(),
                stat.maxValue()
        );
    }

    private static List<InspectSnapshot.InspectStat> basicInspectStats(LivingEntity target) {
        List<InspectSnapshot.InspectStat> stats = new ArrayList<>();
        stats.add(new InspectSnapshot.InspectStat(
                "health",
                "attribute.name.generic.max_health",
                0,
                target.getHealth(),
                "NUMBER",
                target.getHealth(),
                target.getMaxHealth()
        ));
        stats.add(new InspectSnapshot.InspectStat(
                "damage",
                "attribute.name.generic.attack_damage",
                0,
                target.getAttributeValue(Attributes.ATTACK_DAMAGE),
                "NUMBER",
                target.getAttributeValue(Attributes.ATTACK_DAMAGE),
                0.0D
        ));
        stats.add(new InspectSnapshot.InspectStat(
                "speed",
                "attribute.name.generic.movement_speed",
                0,
                target.getAttributeValue(Attributes.MOVEMENT_SPEED),
                "NUMBER",
                target.getAttributeValue(Attributes.MOVEMENT_SPEED),
                0.0D
        ));
        stats.add(new InspectSnapshot.InspectStat(
                "resistance",
                "attribute.name.generic.armor",
                0,
                target.getAttributeValue(Attributes.ARMOR),
                "NUMBER",
                target.getAttributeValue(Attributes.ARMOR),
                0.0D
        ));
        return List.copyOf(stats);
    }

    private static InspectSnapshot.RadarSnapshot radar(LivingEntity target) {
        BaseStats baseStats = CreatureInfoApi.getDangerRatingStats(target);
        int power = SpyglassRadarBaseline.powerPercentile(baseStats.attackDamage());
        int life = SpyglassRadarBaseline.lifePercentile(baseStats.maxHealth());
        int speed = SpyglassRadarBaseline.speedPercentile(baseStats.movementSpeed());
        return new InspectSnapshot.RadarSnapshot(true, power, life, speed);
    }

    private static RideStatusSnapshot rideStatus(LivingEntity target) {
        if (target instanceof net.minecraft.world.entity.player.Player) {
            return RideStatusSnapshot.NONE;
        }
        if (NativeRideCapabilityResolver.hasNativePlayerControl(target)) {
            return new RideStatusSnapshot(
                    true,
                    "gui.tl_domesticate_more_creatures.riding.status.generic",
                    ""
            );
        }
        if (!PetOwnershipService.isTamed(target) || TamingRuleManager.ruleFor(target).isEmpty()) {
            return RideStatusSnapshot.NONE;
        }
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (entityId == null) {
            return disabledRideStatus();
        }
        EntityRideProfile profile = RidingConfigManager.profile(entityId);
        if (profile.mode() == RideMode.DISABLED
                || (!RidingConfigManager.settings().allows(entityId) && profile.mode() != RideMode.FORCE_GENERIC)) {
            return disabledRideStatus();
        }
        String movementKey = "gui.tl_domesticate_more_creatures.riding.movement.auto";
        if (target instanceof Mob mob) {
            RideCapabilityProfile capabilities = RideCapabilityResolver.profile(mob, profile);
            movementKey = movementKey(capabilities);
        }
        return new RideStatusSnapshot(
                true,
                "gui.tl_domesticate_more_creatures.riding.status.generic",
                movementKey
        );
    }

    private static RideStatusSnapshot disabledRideStatus() {
        return new RideStatusSnapshot(
                true,
                "gui.tl_domesticate_more_creatures.riding.status.disabled",
                "gui.tl_domesticate_more_creatures.riding.movement.auto"
        );
    }

    private static String movementKey(RideCapabilityProfile capabilities) {
        if (capabilities.flight() && capabilities.swim()) {
            return "gui.tl_domesticate_more_creatures.riding.movement.flight_swim";
        }
        if (capabilities.ground() && capabilities.swim()) {
            return "gui.tl_domesticate_more_creatures.riding.movement.ground_swim";
        }
        if (capabilities.flight()) {
            return "gui.tl_domesticate_more_creatures.riding.movement.flight";
        }
        if (capabilities.swim()) {
            return "gui.tl_domesticate_more_creatures.riding.movement.swim";
        }
        return "gui.tl_domesticate_more_creatures.riding.movement.ground";
    }

    private static List<TalentSnapshot> talents(ProgressData data) {
        return talents(data.talents(), data.specialTalents());
    }

    private static List<TalentSnapshot> talents(Map<String, Integer> stored) {
        return talents(stored, Set.of());
    }

    private static List<TalentSnapshot> talents(Map<String, Integer> stored, Set<String> specialTalents) {
        List<TalentSnapshot> result = new ArrayList<>();
        for (TalentDefinition definition : TalentConfigManager.all()) {
            Integer storedLevel = stored.get(definition.id());
            if (storedLevel == null) {
                continue;
            }
            int level = definition.clampLevel(storedLevel);
            List<TalentSnapshot.Effect> effects = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : definition.effectsFor(level).entrySet()) {
                StatDefinition stat = StatConfigManager.byId(entry.getKey());
                String nameKey = stat == null
                        ? "stat.tl_domesticate_more_creatures." + entry.getKey()
                        : stat.nameKey();
                effects.add(new TalentSnapshot.Effect(nameKey, entry.getValue()));
            }
            result.add(new TalentSnapshot(definition.id(), definition.nameKey(), level, false, List.copyOf(effects)));
        }
        Set<String> storedSpecial = specialTalents == null ? Set.of() : specialTalents;
        for (SpecialTalentDefinition definition : SpecialTalentConfigManager.all()) {
            if (storedSpecial.contains(definition.id())) {
                result.add(new TalentSnapshot(definition.id(), definition.nameKey(), 0, true, List.of()));
            }
        }
        return List.copyOf(result);
    }

    private static FurySnapshot fury(LivingEntity target) {
        if (!FuryService.hasFury(target) || !ProgressData.exists(target)) {
            return FurySnapshot.NONE;
        }
        ProgressData data = ProgressData.of(target);
        double max = FuryService.maxAnger(target);
        return new FurySnapshot(
                true,
                Math.max(0.0D, Math.min(max, data.furyAnger())),
                max,
                data.furyBerserk()
        );
    }

    private static String displayFormat(StatDefinition definition) {
        if (AttributeService.DAMAGE_MULTIPLIER_TARGET.equals(definition.target())) {
            return "MULTIPLIER";
        }
        if (AttributeService.RESISTANCE_TARGET.equals(definition.target())) {
            return "PERCENT";
        }
        return "NUMBER";
    }

    private static DynamicAttributeDisplayValue panelDisplayValue(
            ServerPlayer viewer,
            LivingEntity target,
            StatDefinition definition
    ) {
        ResourceLocation targetId = ResourceLocation.tryParse(definition.target());
        if (targetId != null) {
            DynamicAttributeDisplayValue external = DynamicAttributeDisplayRegistry.resolve(targetId, viewer, target).orElse(null);
            if (external != null) {
                return external;
            }
        }
        if (MAX_HEALTH_TARGET.equals(definition.target())) {
            return new DynamicAttributeDisplayValue(target.getHealth(), target.getMaxHealth());
        }
        if (AttributeService.TORPOR_TARGET.equals(definition.target())) {
            return new DynamicAttributeDisplayValue(TorporService.currentTorpor(target), TorporService.maxTorpor(target));
        }
        return null;
    }

    private static double currentValue(LivingEntity target, StatDefinition definition) {
        if (MAX_HEALTH_TARGET.equals(definition.target())) {
            return target.getHealth();
        }
        if (AttributeService.TORPOR_TARGET.equals(definition.target())) {
            return TorporService.currentTorpor(target);
        }
        return AttributeService.displayedValue(target, definition);
    }

    private static double maxValue(LivingEntity target, StatDefinition definition) {
        if (MAX_HEALTH_TARGET.equals(definition.target())) {
            return target.getMaxHealth();
        }
        if (AttributeService.TORPOR_TARGET.equals(definition.target())) {
            return TorporService.maxTorpor(target);
        }
        return 0.0D;
    }

    private record PanelAttributeEntry(int displayOrder, String sortId, StatSnapshot snapshot) {
    }
}
