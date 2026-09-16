package com.szypxj.tldomesticatemorecreatures.compat.arsnouveau;

import com.mojang.logging.LogUtils;
import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.api.attribute.DynamicAttributeDisplayRegistry;
import com.szypxj.tldomesticatemorecreatures.api.attribute.DynamicAttributeDisplayValue;
import com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeDefinition;
import com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeFlags;
import com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeRegistry;
import com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeValue;
import com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeValueFormat;
import com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeValueStore;
import com.szypxj.tldomesticatemorecreatures.config.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;

public final class ArsNouveauManaCompat {
    public static final String ARS_MOD_ID = "ars_nouveau";
    public static final ResourceLocation MANA_ID = Objects.requireNonNull(ResourceLocation.tryBuild(TlDomesticateMoreCreatures.MOD_ID, "mana"));

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final UUID TDMC_MANA_MODIFIER_ID = UUID.fromString("d1889f6b-d9be-4b2b-902c-8b2b123a26b7");
    private static final String ARS_MAX_MANA_DESCRIPTION_ID = "ars_nouveau.perk.max_mana";
    private static final double MAX_STORED_BONUS = Double.MAX_VALUE;

    private static boolean registered;
    private static boolean reflectionResolved;
    private static boolean reflectionAvailable;
    private static Method getCurrentManaMethod;
    private static Method getMaxManaMethod;
    private static Attribute maxManaAttribute;

    private ArsNouveauManaCompat() {
    }

    public static synchronized void register() {
        if (registered || !ModList.get().isLoaded(ARS_MOD_ID)) {
            return;
        }
        registered = true;

        TdmcAttributeFlags flags = new TdmcAttributeFlags(
                true,
                true,
                false,
                true,
                false,
                false,
                true
        );
        TdmcAttributeDefinition definition = TdmcAttributeDefinition.builder(
                        MANA_ID,
                        "stat.tl_domesticate_more_creatures.mana"
                )
                .descriptionKey("tip.tl_domesticate_more_creatures.mana_base")
                .icon("ars_nouveau:source_gem")
                .displayOrder(450)
                .bounds(0.0D, MAX_STORED_BONUS)
                .defaultValue(0.0D)
                .valueFormat(TdmcAttributeValueFormat.NUMBER)
                .flags(flags)
                .pointRule(Config.ARS_NOUVEAU_MANA_PER_POINT.get(), 1)
                .applicability(entity -> entity instanceof Player)
                .valueProvider(entity -> TdmcAttributeValue.of(storedBonus(entity), MAX_STORED_BONUS))
                .valueWriter(ArsNouveauManaCompat::writeBonus)
                .build();
        TdmcAttributeRegistry.registerIfAbsent(TlDomesticateMoreCreatures.MOD_ID, definition);

        DynamicAttributeDisplayRegistry.register(MANA_ID, (viewer, target) -> {
            if (!(target instanceof Player player)) {
                return null;
            }
            sync(player);
            double current = currentMana(player);
            double max = maxMana(player);
            return max > 0.0D ? new DynamicAttributeDisplayValue(current, max) : null;
        });

        MinecraftForge.EVENT_BUS.addListener(ArsNouveauManaCompat::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(ArsNouveauManaCompat::onPlayerRespawn);
        MinecraftForge.EVENT_BUS.addListener(ArsNouveauManaCompat::onPlayerChangedDimension);
    }

    public static void sync(Player player) {
        if (player == null || player.level().isClientSide || !ModList.get().isLoaded(ARS_MOD_ID)) {
            return;
        }
        Attribute attribute = maxManaAttribute();
        if (attribute == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        AttributeModifier existing = instance.getModifier(TDMC_MANA_MODIFIER_ID);
        double bonus = storedBonus(player);
        if (existing != null && Math.abs(existing.getAmount() - bonus) < 0.000001D) {
            return;
        }
        if (existing != null) {
            instance.removeModifier(existing);
        }
        if (bonus > 0.0D) {
            instance.addTransientModifier(new AttributeModifier(
                    TDMC_MANA_MODIFIER_ID,
                    "TDMC base mana",
                    bonus,
                    AttributeModifier.Operation.ADDITION
            ));
        }
    }

    public static double currentMana(Player player) {
        if (player == null || !resolveReflection()) {
            return 0.0D;
        }
        try {
            Object value = getCurrentManaMethod.invoke(null, player);
            return value instanceof Number number ? Math.max(0.0D, number.doubleValue()) : 0.0D;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            disableReflection(exception);
            return 0.0D;
        }
    }

    public static double maxMana(Player player) {
        if (player == null || !resolveReflection()) {
            return 0.0D;
        }
        try {
            Object value = getMaxManaMethod.invoke(null, player);
            return value instanceof Number number ? Math.max(0.0D, number.doubleValue()) : 0.0D;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            disableReflection(exception);
            return 0.0D;
        }
    }

    public static int refundAllocatedPoints(LivingEntity entity) {
        if (!(entity instanceof Player player) || !registered) {
            return 0;
        }
        int points = TdmcAttributeValueStore.allocatedPoints(player, MANA_ID);
        if (points <= 0) {
            return 0;
        }
        if (!com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeOperations.remove(player, MANA_ID)) {
            return 0;
        }
        return points;
    }

    private static boolean writeBonus(LivingEntity entity, double value) {
        if (!(entity instanceof Player player) || !Double.isFinite(value)) {
            return false;
        }
        double clamped = Math.max(0.0D, Math.min(MAX_STORED_BONUS, value));
        TdmcAttributeValueStore.set(player, MANA_ID, clamped);
        sync(player);
        return true;
    }

    private static double storedBonus(LivingEntity entity) {
        return entity == null ? 0.0D : Math.max(0.0D, TdmcAttributeValueStore.get(entity, MANA_ID, 0.0D));
    }

    private static Attribute maxManaAttribute() {
        Attribute cached = maxManaAttribute;
        if (cached != null) {
            return cached;
        }
        for (Attribute attribute : ForgeRegistries.ATTRIBUTES.getValues()) {
            if (ARS_MAX_MANA_DESCRIPTION_ID.equals(attribute.getDescriptionId())) {
                maxManaAttribute = attribute;
                return attribute;
            }
        }
        return null;
    }

    private static synchronized boolean resolveReflection() {
        if (reflectionResolved) {
            return reflectionAvailable;
        }
        reflectionResolved = true;
        if (!ModList.get().isLoaded(ARS_MOD_ID)) {
            return false;
        }
        try {
            Class<?> manaUtil = Class.forName("com.hollingsworth.arsnouveau.api.util.ManaUtil");
            getCurrentManaMethod = manaUtil.getMethod("getCurrentMana", LivingEntity.class);
            getMaxManaMethod = manaUtil.getMethod("getMaxMana", Player.class);
            reflectionAvailable = true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Ars Nouveau mana API could not be resolved. TDMC mana display compatibility is disabled.", exception);
            reflectionAvailable = false;
        }
        return reflectionAvailable;
    }

    private static synchronized void disableReflection(Exception exception) {
        if (reflectionAvailable) {
            LOGGER.warn("Ars Nouveau mana API invocation failed. TDMC mana display compatibility is disabled.", exception);
        }
        reflectionAvailable = false;
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        sync(event.getEntity());
    }

    private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sync(event.getEntity());
    }

    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sync(event.getEntity());
    }
}
