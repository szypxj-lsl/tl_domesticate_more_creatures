package com.szypxj.tldomesticatemorecreatures.mixin.client.inventory;

import com.szypxj.tldomesticatemorecreatures.client.AttributePanelScreen;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcUiTheme;
import com.szypxj.tldomesticatemorecreatures.client.inventory.ExtraInventorySidecarLayout;
import com.szypxj.tldomesticatemorecreatures.client.inventory.ExtraInventorySidecarScreenAccess;
import com.szypxj.tldomesticatemorecreatures.menu.ExtraInventoryMenuAccess;
import com.szypxj.tldomesticatemorecreatures.menu.ExtraInventoryMenuBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenExtraInventoryMixin implements ExtraInventorySidecarScreenAccess {
    @Unique
    private static final int SIDE_COLUMNS = 3;
    @Unique
    private static final int SIDE_ROWS = 9;
    @Unique
    private static final int SLOT_SPACING = 18;
    @Unique
    private static final int SIDE_WIDTH = SIDE_COLUMNS * SLOT_SPACING;
    @Unique
    private static final int SIDE_HEIGHT = SIDE_ROWS * SLOT_SPACING;
    @Unique
    private static final int SIDE_GAP = 8;
    @Unique
    private static final int SCREEN_MARGIN = 6;

    @Shadow
    protected int imageWidth;
    @Shadow
    protected int imageHeight;
    @Shadow
    protected int leftPos;
    @Shadow
    protected int topPos;
    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    @Unique
    private int tdmc$sideX;
    @Unique
    private int tdmc$sideY;
    @Unique
    private boolean tdmc$renderSidecar;

    @Inject(method = "init", at = @At("TAIL"))
    private void tdmc$layoutExtraInventorySlots(CallbackInfo ci) {
        tdmc$updateSidecarLayout();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void tdmc$refreshExtraInventoryLayout(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        tdmc$updateSidecarLayout();
    }

    @Unique
    private void tdmc$updateSidecarLayout() {
        Object self = this;
        if (self instanceof AttributePanelScreen || self instanceof CreativeModeInventoryScreen) {
            tdmc$renderSidecar = false;
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            tdmc$renderSidecar = false;
            return;
        }

        ExtraInventoryMenuBridge.attachClient(menu, minecraft.player, -10000, -10000, SLOT_SPACING, SLOT_SPACING);
        if (!(menu instanceof ExtraInventoryMenuAccess access) || !access.tdmc$extraInventoryAttached()) {
            tdmc$renderSidecar = false;
            return;
        }

        int hostLeft = leftPos;
        int hostRight = leftPos + imageWidth;
        int extraStart = access.tdmc$extraInventoryStart();
        for (int index = 0; index < extraStart && index < menu.slots.size(); index++) {
            Slot slot = menu.slots.get(index);
            hostLeft = Math.min(hostLeft, leftPos + slot.x - 8);
            hostRight = Math.max(hostRight, leftPos + slot.x + 24);
        }

        ExtraInventorySidecarLayout.Position position = ExtraInventorySidecarLayout.resolve(
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(),
                hostLeft,
                topPos,
                Math.max(1, hostRight - hostLeft),
                imageHeight,
                SIDE_WIDTH,
                SIDE_HEIGHT,
                SIDE_GAP,
                SCREEN_MARGIN
        );
        tdmc$sideX = position.x() - leftPos;
        tdmc$sideY = position.y() - topPos;
        ExtraInventoryMenuBridge.positionClient(menu, tdmc$sideX, tdmc$sideY, SLOT_SPACING, SLOT_SPACING);
        tdmc$renderSidecar = true;
    }

    @Override
    public void tdmc$renderExtraInventorySidecar(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!tdmc$renderSidecar || !(menu instanceof ExtraInventoryMenuAccess access)) {
            return;
        }
        int start = access.tdmc$extraInventoryStart();
        int end = access.tdmc$extraInventoryEnd();
        if (start < 0 || end <= start || end > menu.slots.size()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int x0 = leftPos + tdmc$sideX - 4;
        int y0 = topPos + tdmc$sideY - 4;
        TdmcUiTheme.fillPanel(graphics, x0, y0, SIDE_WIDTH + 8, SIDE_HEIGHT + 8);

        Slot hoveredSlot = null;
        for (int menuIndex = start; menuIndex < end; menuIndex++) {
            Slot slot = menu.slots.get(menuIndex);
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            boolean hovered = slot.isActive() && mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
            TdmcUiTheme.fillSlot(graphics, x - 1, y - 1, 18, 18, hovered);
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, x, y);
                graphics.renderItemDecorations(minecraft.font, stack, x, y);
            }
            if (hovered) {
                hoveredSlot = slot;
            }
        }

        if (hoveredSlot != null && !hoveredSlot.getItem().isEmpty()) {
            graphics.renderTooltip(minecraft.font, hoveredSlot.getItem(), mouseX, mouseY);
        }
    }
}
