package com.szypxj.tldomesticatemorecreatures.client.riding;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionInfo;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionState;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID, value = Dist.CLIENT)
public final class RideAttackHudRenderer {
    private static final int CHIP_HEIGHT = 16;
    private static final int CHIP_GAP = 4;
    private static final int RIGHT_MARGIN = 8;
    private static final int TOP_MARGIN = 8;
    private static final int BOTTOM_MARGIN = 8;
    private static final float TOP_START_RATIO = 0.38F;
    private static final int HORIZONTAL_PADDING = 6;
    private static final int BACKGROUND = 0xBF0A2945;
    private static final int BORDER = 0xD05A9BC5;
    private static final int TEXT = 0xFFFFFFFF;

    private RideAttackHudRenderer() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || !ClientRideControlState.activeForCurrentMount(minecraft)) {
            return;
        }
        List<RideActionInfo> actions = ClientRideControlState.actions();
        if (actions.isEmpty()) return;

        List<Chip> chips = new ArrayList<>(actions.size());
        for (RideActionInfo info : actions) {
            Component label = actionLabel(minecraft, info);
            chips.add(new Chip(label, minecraft.font.width(label) + HORIZONTAL_PADDING * 2));
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int stackHeight = chips.size() * CHIP_HEIGHT + Math.max(0, chips.size() - 1) * CHIP_GAP;
        int preferredY = Math.max(TOP_MARGIN, Math.round(screenHeight * TOP_START_RATIO));
        int maxY = Math.max(TOP_MARGIN, screenHeight - stackHeight - BOTTOM_MARGIN);
        int y = Math.min(preferredY, maxY);
        for (Chip chip : chips) {
            int x = screenWidth - chip.width() - RIGHT_MARGIN;
            renderChip(graphics, minecraft, chip, x, y);
            y += CHIP_HEIGHT + CHIP_GAP;
        }
    }

    private static Component actionLabel(Minecraft minecraft, RideActionInfo info) {
        Component key = RideControlClientHandler.keyMessage(minecraft, info.action());
        Component name = info.nameTranslationKey().isBlank()
                ? Component.translatable("gui.tl_domesticate_more_creatures.riding.control.unnamed")
                : Component.translatable(info.nameTranslationKey());
        MutableComponent label = Component.translatable(
                "gui.tl_domesticate_more_creatures.riding.control.hud_action",
                key,
                name
        );
        RideActionStatus status = ClientRideControlState.status(info.action());
        if (status.state() == RideActionState.COOLDOWN && status.remainingTicks() > 0) {
            String seconds = String.format(Locale.ROOT, "%.1f", status.remainingTicks() / 20.0F);
            label.append(Component.literal(" ")).append(Component.translatable(
                    "gui.tl_domesticate_more_creatures.riding.control.hud_cooldown",
                    seconds
            ));
        } else if (status.state() == RideActionState.BLOCKED) {
            label.append(Component.literal(" ")).append(Component.translatable(
                    "gui.tl_domesticate_more_creatures.riding.control.hud_blocked"
            ));
        } else if (status.state() == RideActionState.CHARGING) {
            label.append(Component.literal(" ")).append(Component.translatable(
                    "gui.tl_domesticate_more_creatures.riding.control.hud_charging"
            ));
        } else if (status.state() == RideActionState.ACTIVE) {
            label.append(Component.literal(" ")).append(Component.translatable(
                    "gui.tl_domesticate_more_creatures.riding.control.hud_active"
            ));
        }
        return label;
    }

    private static void renderChip(GuiGraphics graphics, Minecraft minecraft, Chip chip, int x, int y) {
        int right = x + chip.width();
        int bottom = y + CHIP_HEIGHT;
        graphics.fill(x, y, right, bottom, BACKGROUND);
        graphics.fill(x, y, right, y + 1, BORDER);
        graphics.fill(x, bottom - 1, right, bottom, BORDER);
        graphics.fill(x, y, x + 1, bottom, BORDER);
        graphics.fill(right - 1, y, right, bottom, BORDER);
        graphics.drawString(minecraft.font, chip.label(), x + HORIZONTAL_PADDING, y + 4, TEXT, false);
    }

    private record Chip(Component label, int width) {
    }
}
