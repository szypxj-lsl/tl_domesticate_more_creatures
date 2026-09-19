package com.szypxj.tldomesticatemorecreatures.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientKeys {
    public static final KeyMapping RIDE = new KeyMapping(
            "key.tl_domesticate_more_creatures.ride",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            "key.categories.tl_domesticate_more_creatures"
    );

    public static final KeyMapping RIDE_ROAR = new KeyMapping(
            "key.tl_domesticate_more_creatures.ride_roar",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.tl_domesticate_more_creatures"
    );

    public static final KeyMapping RIDE_SKILL_1 = new KeyMapping(
            "key.tl_domesticate_more_creatures.ride_skill_slot_1",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "key.categories.tl_domesticate_more_creatures"
    );

    public static final KeyMapping RIDE_SKILL_2 = new KeyMapping(
            "key.tl_domesticate_more_creatures.ride_skill_slot_2",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            "key.categories.tl_domesticate_more_creatures"
    );

    public static final KeyMapping RIDE_SKILL_3 = new KeyMapping(
            "key.tl_domesticate_more_creatures.ride_skill_slot_3",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.tl_domesticate_more_creatures"
    );

    public static final KeyMapping RIDE_UTILITY = new KeyMapping(
            "key.tl_domesticate_more_creatures.ride_descend",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            "key.categories.tl_domesticate_more_creatures"
    );

    public static final KeyMapping ACTIVE_TALENT = new KeyMapping(
            "key.tl_domesticate_more_creatures.active_talent_v2",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.tl_domesticate_more_creatures"
    );

    public static final KeyMapping EDIT_SPYGLASS_PANEL = new KeyMapping(
            "key.tl_domesticate_more_creatures.edit_spyglass_panel",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.tl_domesticate_more_creatures"
    );

    private ClientKeys() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(RIDE);
        event.register(RIDE_ROAR);
        event.register(RIDE_SKILL_1);
        event.register(RIDE_SKILL_2);
        event.register(RIDE_SKILL_3);
        event.register(RIDE_UTILITY);
        event.register(ACTIVE_TALENT);
        event.register(EDIT_SPYGLASS_PANEL);
    }
}
