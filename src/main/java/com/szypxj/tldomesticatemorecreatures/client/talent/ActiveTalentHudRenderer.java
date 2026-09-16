package com.szypxj.tldomesticatemorecreatures.client.talent;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.client.ClientKeys;
import com.szypxj.tldomesticatemorecreatures.client.TargetHudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID, value = Dist.CLIENT)
public final class ActiveTalentHudRenderer {
    private ActiveTalentHudRenderer() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) return;
        ClientActiveTalentState.Snapshot state = ClientActiveTalentState.currentForMountedPet();
        if (state == null || state.skillId() == null || state.skillId().isBlank()) return;
        long now = minecraft.level.getGameTime();
        Component name = Component.translatable("talent.tl_domesticate_more_creatures.special." + state.skillId());
        Component key = ClientKeys.ACTIVE_TALENT.getTranslatedKeyMessage();
        Component line;
        if (state.cooldownEndGameTime() > now) {
            line = Component.translatable(
                    "gui.tl_domesticate_more_creatures.active_talent.hud_cooldown",
                    key,
                    name,
                    seconds(state.cooldownEndGameTime() - now)
            );
        } else if ("MARKING".equals(state.phase())) {
            line = Component.translatable(
                    "gui.tl_domesticate_more_creatures.active_talent.hud_marking",
                    key,
                    name,
                    state.marks(),
                    state.maxMarks(),
                    seconds(Math.max(0L, state.phaseEndGameTime() - now))
            );
        } else if ("CAMOUFLAGED".equals(state.phase())) {
            line = Component.translatable(
                    "gui.tl_domesticate_more_creatures.active_talent.hud_camouflaged",
                    key,
                    name,
                    clock(Math.max(0L, state.phaseEndGameTime() - now))
            );
        } else if (state.phase() == null || state.phase().isBlank()) {
            line = Component.translatable(
                    "gui.tl_domesticate_more_creatures.active_talent.hud_ready",
                    key,
                    name
            );
        } else {
            line = Component.translatable(
                    "gui.tl_domesticate_more_creatures.active_talent.hud_active",
                    key,
                    name
            );
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int anchorY = TargetHudRenderer.activeTalentAnchorY(minecraft);
        graphics.drawCenteredString(minecraft.font, line, centerX, anchorY, 0xFFFFFF);
    }

    private static String seconds(long ticks) {
        return String.format(Locale.ROOT, "%.1f", Math.max(0L, ticks) / 20.0D);
    }

    private static String clock(long ticks) {
        long totalSeconds = Math.max(0L, (ticks + 19L) / 20L);
        return String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60L, totalSeconds % 60L);
    }
}
