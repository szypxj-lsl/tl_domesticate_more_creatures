package com.szypxj.tldomesticatemorecreatures.compat.saintsdragons;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideAction;
import com.szypxj.tldomesticatemorecreatures.client.ClientKeys;
import com.szypxj.tldomesticatemorecreatures.client.riding.ClientRideControlState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/** Remaps TDMC unified keys onto Saint's Dragons' native KeyMappings before its input tick. */
@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID, value = Dist.CLIENT)
public final class SaintsDragonsUnifiedInputClient {
    private static final String PRIMARY = "key.saintsdragons.ability_primary";
    private static final String SECONDARY = "key.saintsdragons.ability_secondary";
    private static final String TERTIARY = "key.saintsdragons.ability_tertiary";
    private static final String TOGGLE_MELEE = "key.saintsdragons.toggle_melee";
    private static final String DESCEND = "key.saintsdragons.descend";
    private static final String ASCEND = "key.saintsdragons.ascend";
    private static final Map<String, KeyMapping> CACHE = new HashMap<>();
    private static boolean wasActive;

    private SaintsDragonsUnifiedInputClient() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        boolean active = minecraft.player != null
                && minecraft.player.getVehicle() instanceof LivingEntity mount
                && SaintsDragonsRideControlProvider.isRideableDragon(mount)
                && ClientRideControlState.activeForCurrentMount(minecraft);
        if (!active) {
            if (wasActive) releaseNativeMappings(minecraft);
            wasActive = false;
            return;
        }
        wasActive = true;

        boolean primaryAbilityDown = ClientRideControlState.supports(RideAction.SKILL_1) && ClientKeys.RIDE_SKILL_1.isDown();
        if (ClientRideControlState.supports(RideAction.ROAR) && ClientKeys.RIDE_ROAR.isDown()) {
            primaryAbilityDown = true;
        }
        setDown(minecraft, PRIMARY, primaryAbilityDown);
        setDown(minecraft, TOGGLE_MELEE, ClientRideControlState.supports(RideAction.SECONDARY_ATTACK)
                && minecraft.options.keyUse.isDown());
        setDown(minecraft, SECONDARY, ClientRideControlState.supports(RideAction.SKILL_2) && ClientKeys.RIDE_SKILL_2.isDown());
        setDown(minecraft, TERTIARY, ClientRideControlState.supports(RideAction.SKILL_3) && ClientKeys.RIDE_SKILL_3.isDown());
        setDown(minecraft, DESCEND, ClientRideControlState.supports(RideAction.UTILITY) && ClientKeys.RIDE_UTILITY.isDown());
        // Space is already Minecraft's jump key and Saint's handler reads it natively. Keep the legacy ascend bind suppressed.
        setDown(minecraft, ASCEND, false);
    }

    private static void releaseNativeMappings(Minecraft minecraft) {
        setDown(minecraft, PRIMARY, false);
        setDown(minecraft, SECONDARY, false);
        setDown(minecraft, TERTIARY, false);
        setDown(minecraft, TOGGLE_MELEE, false);
        setDown(minecraft, DESCEND, false);
        setDown(minecraft, ASCEND, false);
    }

    private static void setDown(Minecraft minecraft, String translationKey, boolean down) {
        KeyMapping mapping = find(minecraft, translationKey);
        if (mapping != null) mapping.setDown(down);
    }

    private static KeyMapping find(Minecraft minecraft, String translationKey) {
        if (minecraft == null || minecraft.options == null) return null;
        KeyMapping cached = CACHE.get(translationKey);
        if (cached != null) return cached;
        for (KeyMapping mapping : minecraft.options.keyMappings) {
            if (translationKey.equals(mapping.getName())) {
                CACHE.put(translationKey, mapping);
                return mapping;
            }
        }
        return null;
    }
}
