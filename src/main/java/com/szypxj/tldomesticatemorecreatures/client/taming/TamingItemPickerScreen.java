package com.szypxj.tldomesticatemorecreatures.client.taming;

import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcButton;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcEditBox;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcUiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TamingItemPickerScreen extends Screen {
    private static final int ROW_HEIGHT = 34;
    private static final int LIST_TOP = 68;
    private static final int SCROLLBAR_WIDTH = 7;

    private final TamingEditorScreen parent;
    private final Set<ResourceLocation> blocked;
    private TdmcEditBox searchBox;
    private String searchText = "";
    private List<ResourceLocation> visibleItems = List.of();
    private int scroll;
    private int maxScroll;
    private boolean draggingScrollbar;

    public TamingItemPickerScreen(TamingEditorScreen parent, ResourceLocation entityId, Set<ResourceLocation> blocked) {
        super(Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.item_picker.title"));
        this.parent = parent;
        this.blocked = Set.copyOf(blocked == null ? Set.of() : blocked);
    }

    @Override
    protected void init() {
        int right = width - 8;
        addRenderableWidget(TdmcButton.create(right - 92, 8, 84, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.item_picker.back"),
                button -> returnToParent()));
        searchBox = new TdmcEditBox(font, 12, 38, Math.max(120, width - 24), 20,
                Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.item_picker.search"));
        searchBox.setValue(searchText);
        searchBox.setResponder(value -> {
            searchText = value;
            scroll = 0;
            rebuildVisibleItems();
        });
        addRenderableWidget(searchBox);
        rebuildVisibleItems();
    }

    private void rebuildVisibleItems() {
        String query = searchText.trim().toLowerCase(Locale.ROOT);
        List<ResourceLocation> result = new ArrayList<>();
        for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item == null || item == Items.AIR) {
                continue;
            }
            String name = new ItemStack(item).getHoverName().getString();
            if (!query.isEmpty()
                    && !id.toString().toLowerCase(Locale.ROOT).contains(query)
                    && !name.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            result.add(id);
        }
        result.sort(Comparator
                .comparing((ResourceLocation id) -> blocked.contains(id))
                .thenComparing(this::itemDisplayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ResourceLocation::toString));
        visibleItems = List.copyOf(result);
        maxScroll = Math.max(0, visibleItems.size() * ROW_HEIGHT - Math.max(1, height - LIST_TOP - 12));
        scroll = Mth.clamp(scroll, 0, maxScroll);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        TdmcUiTheme.fillPanel(graphics, 4, 4, width - 8, height - 8);
        TdmcUiTheme.fillHeader(graphics, 5, 5, width - 10, 28);
        graphics.drawString(font, title, 12, 14, TdmcUiTheme.TEXT_PRIMARY, false);

        int left = 12;
        int right = width - 12;
        int bottom = height - 10;
        TdmcUiTheme.fillSection(graphics, left, LIST_TOP, right - left, bottom - LIST_TOP);
        graphics.enableScissor(left + 1, LIST_TOP + 1, right - SCROLLBAR_WIDTH - 3, bottom - 1);
        int y = LIST_TOP + 4 - scroll;
        for (ResourceLocation id : visibleItems) {
            if (y + ROW_HEIGHT >= LIST_TOP && y <= bottom) {
                boolean disabled = blocked.contains(id);
                boolean hovered = mouseX >= left + 4 && mouseX < right - SCROLLBAR_WIDTH - 6
                        && mouseY >= y && mouseY < y + ROW_HEIGHT - 2;
                graphics.fill(left + 4, y, right - SCROLLBAR_WIDTH - 6, y + ROW_HEIGHT - 2,
                        hovered ? TdmcUiTheme.ROW_HOVERED : TdmcUiTheme.ROW_BACKGROUND);
                Item item = ForgeRegistries.ITEMS.getValue(id);
                if (item != null) {
                    ItemStack stack = new ItemStack(item);
                    graphics.renderItem(stack, left + 9, y + 8);
                    graphics.drawString(font, stack.getHoverName(), left + 31, y + 6,
                            disabled ? TdmcUiTheme.TEXT_MUTED : TdmcUiTheme.TEXT_PRIMARY, false);
                }
                graphics.drawString(font, id.toString(), left + 31, y + 18, TdmcUiTheme.TEXT_MUTED, false);
                if (disabled) {
                    graphics.drawString(font,
                            Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.item_picker.blocked"),
                            right - SCROLLBAR_WIDTH - 84, y + 12, 0xFFFFC060, false);
                }
            }
            y += ROW_HEIGHT;
        }
        graphics.disableScissor();
        renderScrollbar(graphics, right - SCROLLBAR_WIDTH - 1, LIST_TOP + 2, bottom - LIST_TOP - 4);

        super.render(graphics, mouseX, mouseY, partialTick);
        if (searchBox != null && searchBox.getValue().isEmpty() && !searchBox.isFocused()) {
            graphics.drawString(font,
                    Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.item_picker.search"),
                    searchBox.getX() + 4,
                    searchBox.getY() + (searchBox.getHeight() - 8) / 2,
                    TdmcUiTheme.TEXT_MUTED,
                    false);
        }
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y, int height) {
        graphics.fill(x, y, x + SCROLLBAR_WIDTH, y + height, TdmcUiTheme.SLIDER_TRACK);
        if (maxScroll <= 0) {
            return;
        }
        int thumbHeight = Math.max(18, height * height / (height + maxScroll));
        int travel = Math.max(1, height - thumbHeight);
        int thumbY = y + Math.round(travel * (scroll / (float) maxScroll));
        graphics.fill(x + 1, thumbY, x + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, TdmcUiTheme.SLIDER_THUMB);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= LIST_TOP) {
            scroll = Mth.clamp(scroll - (int) Math.signum(delta) * ROW_HEIGHT, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int barX = width - 12 - SCROLLBAR_WIDTH - 1;
            if (mouseX >= barX && mouseX < barX + SCROLLBAR_WIDTH && mouseY >= LIST_TOP && mouseY < height - 10) {
                draggingScrollbar = maxScroll > 0;
                if (draggingScrollbar) {
                    scroll = scrollFromMouse(mouseY);
                }
                return true;
            }
            if (mouseX >= 16 && mouseX < width - 26 && mouseY >= LIST_TOP && mouseY < height - 10) {
                int index = (int) ((mouseY - LIST_TOP - 4 + scroll) / ROW_HEIGHT);
                if (index >= 0 && index < visibleItems.size()) {
                    ResourceLocation id = visibleItems.get(index);
                    if (!blocked.contains(id)) {
                        parent.addExtraFood(id);
                        returnToParent();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingScrollbar) {
            scroll = scrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        returnToParent();
    }

    private void returnToParent() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private int scrollFromMouse(double mouseY) {
        int trackY = LIST_TOP + 2;
        int trackHeight = height - LIST_TOP - 14;
        if (maxScroll <= 0 || trackHeight <= 1) {
            return 0;
        }
        double ratio = Mth.clamp((mouseY - trackY) / Math.max(1.0D, trackHeight), 0.0D, 1.0D);
        return Mth.clamp((int) Math.round(ratio * maxScroll), 0, maxScroll);
    }

    private String itemDisplayName(ResourceLocation id) {
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item == null ? id.toString() : new ItemStack(item).getHoverName().getString();
    }
}
