package com.szypxj.tldomesticatemorecreatures.client.riding;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.client.TargetHudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID, value = Dist.CLIENT)
public final class RideAttackHudRenderer {
    private RideAttackHudRenderer() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!RideAttackClientHandler.isControlledPetRide(minecraft)) return;
        Component line = Component.translatable(
                "gui.tl_domesticate_more_creatures.riding.attack_hint",
                minecraft.options.keyAttack.getTranslatedKeyMessage()
        );
        GuiGraphics graphics = event.getGuiGraphics();
        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int anchorY = TargetHudRenderer.activeTalentAnchorY(minecraft) + 12;
        graphics.drawCenteredString(minecraft.font, line, centerX, anchorY, 0xFFFFFF);
    }
}
