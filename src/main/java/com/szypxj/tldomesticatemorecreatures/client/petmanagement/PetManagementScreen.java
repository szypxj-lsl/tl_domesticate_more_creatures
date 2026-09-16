package com.szypxj.tldomesticatemorecreatures.client.petmanagement;

import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcButton;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcEditBox;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcConfirmScreen;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcUiTheme;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.network.PetManagementDetail;
import com.szypxj.tldomesticatemorecreatures.network.PetManagementSummary;
import com.szypxj.tldomesticatemorecreatures.petmanagement.PetRecordState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class PetManagementScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private final Screen parent;
    private Filter filter = Filter.ALL;
    private TdmcEditBox searchBox;
    private TdmcButton summonButton;
    private TdmcButton storeButton;
    private TdmcButton viewButton;
    private TdmcButton removeDeadButton;
    private UUID selectedUuid;
    private UUID previewUuid;
    private LivingEntity previewEntity;
    private PetShortcutPopup shortcutPopup;
    private int listScroll;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int leftWidth;

    public PetManagementScreen(Screen parent) {
        super(Component.translatable("gui.tl_domesticate_more_creatures.pet_management.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(680, Math.max(1, width - 16));
        panelHeight = Math.min(410, Math.max(1, height - 16));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        leftWidth = Math.max(120, Math.min(panelWidth - 92, (int) (panelWidth * 0.56F)));

        int y = panelY + 28;
        int innerX = panelX + 10;
        int innerWidth = Math.max(1, panelWidth - 20);
        int filterAreaWidth = Math.min(244, Math.max(140, innerWidth - 82));
        int searchWidth = Math.min(180, innerWidth - filterAreaWidth - 6);
        if (searchWidth < 70) {
            searchWidth = Math.max(1, Math.min(70, innerWidth / 3));
            filterAreaWidth = Math.max(1, innerWidth - searchWidth - 6);
        }
        int filterGap = 4;
        int filterWidth = Math.max(1, (filterAreaWidth - filterGap * 3) / 4);
        addFilterButton(Filter.ALL, innerX, y, filterWidth);
        addFilterButton(Filter.WORLD, innerX + filterWidth + filterGap, y, filterWidth);
        addFilterButton(Filter.STORED, innerX + (filterWidth + filterGap) * 2, y, filterWidth);
        addFilterButton(Filter.DEAD, innerX + (filterWidth + filterGap) * 3, y, filterWidth);

        int searchX = innerX + filterAreaWidth + 6;
        searchBox = addRenderableWidget(new TdmcEditBox(font, searchX, y, searchWidth, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.pet_management.search")));

        int actionY = panelY + panelHeight - 54;
        int rightX = panelX + leftWidth + 10;
        int rightWidth = Math.max(1, panelWidth - leftWidth - 20);
        int buttonWidth = Math.max(1, (rightWidth - 6) / 2);
        summonButton = addRenderableWidget(TdmcButton.create(rightX, actionY, buttonWidth, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.pet_management.action.summon"),
                button -> actSummon()));
        storeButton = addRenderableWidget(TdmcButton.create(rightX + buttonWidth + 6, actionY, buttonWidth, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.pet_management.action.store"),
                button -> actStore()));
        viewButton = addRenderableWidget(TdmcButton.create(rightX, actionY + 24, rightWidth, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.pet_management.action.view"),
                button -> actView()));
        removeDeadButton = addRenderableWidget(TdmcButton.create(rightX, actionY, rightWidth, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.pet_management.action.remove_dead"),
                button -> actRemoveDead()));
        NetworkHandler.requestPetManagementList();
        updateButtons();
    }

    private void addFilterButton(Filter value, int x, int y, int width) {
        addRenderableWidget(TdmcButton.create(x, y, width, 20,
                Component.translatable(value.translationKey), button -> {
                    filter = value;
                    listScroll = 0;
                }));
    }

    @Override
    public void tick() {
        super.tick();
        if (searchBox != null) searchBox.tick();
        updatePreview();
        updateButtons();
    }

    private void updatePreview() {
        PetManagementDetail detail = ClientState.petManagementDetail();
        if (detail == null || selectedUuid == null || !selectedUuid.equals(detail.summary().petUuid())) return;
        if (selectedUuid.equals(previewUuid)) return;
        previewUuid = selectedUuid;
        previewEntity = null;
        CompoundTag tag = detail.previewTag();
        if (minecraft == null || minecraft.level == null || tag.isEmpty()) return;
        Entity loaded = EntityType.loadEntityRecursive(tag, minecraft.level, entity -> entity);
        if (loaded instanceof LivingEntity living) previewEntity = living;
    }

    private void updateButtons() {
        PetManagementSummary summary = selectedSummary();
        boolean selected = summary != null;
        summonButton.visible = selected && summary.state() != PetRecordState.DEAD;
        summonButton.active = summonButton.visible && summary.state() != PetRecordState.SUMMONING;
        summonButton.setMessage(Component.translatable(summary != null && summary.state() == PetRecordState.UNLOCATED
                ? "gui.tl_domesticate_more_creatures.pet_management.action.locate"
                : "gui.tl_domesticate_more_creatures.pet_management.action.summon"));
        storeButton.visible = selected && summary.state() == PetRecordState.WORLD;
        storeButton.active = storeButton.visible;
        viewButton.visible = selected && summary.state() == PetRecordState.WORLD;
        viewButton.active = viewButton.visible;
        removeDeadButton.visible = selected && summary.state() == PetRecordState.DEAD;
        removeDeadButton.active = removeDeadButton.visible;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        TdmcUiTheme.fillPanel(graphics, panelX, panelY, panelWidth, panelHeight);
        TdmcUiTheme.fillHeader(graphics, panelX + 2, panelY + 2, panelWidth - 4, 26);
        graphics.drawString(font, title, panelX + 10, panelY + 9, TdmcUiTheme.TEXT_PRIMARY, true);
        Component capacity = Component.translatable("gui.tl_domesticate_more_creatures.pet_management.capacity",
                ClientState.petManagementStoredCount(), ClientState.petManagementCapacity());
        graphics.drawString(font, capacity, panelX + panelWidth - 10 - font.width(capacity), panelY + 9, TdmcUiTheme.TEXT_PRIMARY, true);
        renderList(graphics, mouseX, mouseY);
        renderPreview(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderSearchPlaceholder(graphics);
        if (shortcutPopup != null) shortcutPopup.render(graphics, font, mouseX, mouseY);
    }

    private void renderSearchPlaceholder(GuiGraphics graphics) {
        if (searchBox == null || searchBox.isFocused() || !searchBox.getValue().isEmpty()) {
            return;
        }
        graphics.drawString(
                font,
                Component.translatable("gui.tl_domesticate_more_creatures.pet_management.search"),
                searchBox.getX() + 6,
                searchBox.getY() + (searchBox.getHeight() - 8) / 2,
                TdmcUiTheme.TEXT_MUTED,
                false
        );
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = panelX + 10;
        int y = panelY + 54;
        int width = leftWidth - 20;
        int height = panelHeight - 66;
        TdmcUiTheme.fillSection(graphics, x, y, width, height);
        List<PetManagementSummary> pets = filteredPets();
        int maxRows = Math.max(1, height / ROW_HEIGHT);
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, pets.size() - maxRows)));
        graphics.enableScissor(x, y, x + width, y + height);
        for (int i = listScroll; i < pets.size() && i < listScroll + maxRows + 1; i++) {
            PetManagementSummary pet = pets.get(i);
            int rowY = y + (i - listScroll) * ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            boolean selected = pet.petUuid().equals(selectedUuid);
            int background = selected ? TdmcUiTheme.ROW_SELECTED : hovered ? TdmcUiTheme.ROW_HOVERED : TdmcUiTheme.ROW_BACKGROUND;
            graphics.fill(x + 1, rowY + 1, x + width - 1, rowY + ROW_HEIGHT - 1, background);
            Component name = Component.literal(pet.displayName());
            graphics.drawString(font, name, x + 6, rowY + 4, TdmcUiTheme.TEXT_PRIMARY, true);
            Component level = Component.translatable("gui.tl_domesticate_more_creatures.pet_management.level", pet.level());
            graphics.drawString(font, level, x + 6, rowY + 14, TdmcUiTheme.TEXT_MUTED, false);
            Component status = statusText(pet.state());
            int statusX = x + width - 6 - font.width(status);
            graphics.drawString(font, status, statusX, rowY + 4, statusColor(pet.state()), true);
            if (pet.shortcutSlot() > 0) {
                Component slot = Component.translatable("gui.tl_domesticate_more_creatures.pet_management.shortcut", pet.shortcutSlot());
                graphics.drawString(font, slot, x + width - 6 - font.width(slot), rowY + 14, TdmcUiTheme.TEXT_MUTED, false);
            }
        }
        graphics.disableScissor();
    }

    private void renderPreview(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = panelX + leftWidth + 8;
        int y = panelY + 28;
        int width = panelWidth - leftWidth - 18;
        int height = Math.max(1, panelHeight - 92);
        TdmcUiTheme.fillSection(graphics, x, y, width, height);
        PetManagementSummary summary = selectedSummary();
        if (summary == null) {
            Component empty = Component.translatable("gui.tl_domesticate_more_creatures.pet_management.preview.empty");
            graphics.drawCenteredString(font, empty, x + width / 2, y + height / 2, TdmcUiTheme.TEXT_MUTED);
            return;
        }
        graphics.drawCenteredString(font, Component.literal(summary.displayName()), x + width / 2, y + 8, TdmcUiTheme.TEXT_PRIMARY);
        graphics.drawCenteredString(font, Component.translatable("gui.tl_domesticate_more_creatures.pet_management.level", summary.level()),
                x + width / 2, y + 20, TdmcUiTheme.TEXT_MUTED);
        graphics.drawCenteredString(font, statusText(summary.state()), x + width / 2, y + 32, statusColor(summary.state()));
        if (previewEntity != null) {
            float size = Math.max(previewEntity.getBbWidth(), previewEntity.getBbHeight());
            int scale = Math.max(18, Math.min(62, (int) (60.0F / Math.max(1.0F, size))));
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    x + width / 2,
                    y + height - 28,
                    scale,
                    (float) (x + width / 2 - mouseX),
                    (float) (y + height / 2 - mouseY),
                    previewEntity
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (shortcutPopup != null) {
            int slot = shortcutPopup.slotAt(mouseX, mouseY);
            if (slot > 0) {
                NetworkHandler.setPetShortcut(shortcutPopup.petUuid(), slot);
                shortcutPopup = null;
                return true;
            }
            if (shortcutPopup.isClearArea(mouseX, mouseY)) {
                NetworkHandler.setPetShortcut(shortcutPopup.petUuid(), 0);
                shortcutPopup = null;
                return true;
            }
            if (!shortcutPopup.contains(mouseX, mouseY)) shortcutPopup = null;
        }
        PetManagementSummary row = rowAt(mouseX, mouseY);
        if (row != null) {
            if (button == 1 && row.rideable() && row.state() != PetRecordState.DEAD) {
                int popupX = Math.min(width - 86, (int) mouseX);
                int popupY = Math.min(height - 102, (int) mouseY);
                shortcutPopup = new PetShortcutPopup(row.petUuid(), popupX, popupY);
                return true;
            }
            if (button == 0) {
                selectedUuid = row.petUuid();
                previewUuid = null;
                previewEntity = null;
                ClientState.setPetManagementDetail(null);
                NetworkHandler.requestPetManagementDetail(selectedUuid);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int listX = panelX + 10;
        int listY = panelY + 54;
        int listWidth = leftWidth - 20;
        int listHeight = panelHeight - 66;
        if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= listY && mouseY < listY + listHeight) {
            listScroll = Math.max(0, listScroll + (delta < 0 ? 1 : -1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private PetManagementSummary rowAt(double mouseX, double mouseY) {
        int x = panelX + 10;
        int y = panelY + 54;
        int width = leftWidth - 20;
        int height = panelHeight - 66;
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) return null;
        int index = listScroll + ((int) mouseY - y) / ROW_HEIGHT;
        List<PetManagementSummary> pets = filteredPets();
        return index >= 0 && index < pets.size() ? pets.get(index) : null;
    }

    private List<PetManagementSummary> filteredPets() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<PetManagementSummary> result = new ArrayList<>();
        for (PetManagementSummary pet : ClientState.petManagementList()) {
            if (!filter.matches(pet.state())) continue;
            if (!query.isEmpty() && !pet.displayName().toLowerCase(Locale.ROOT).contains(query)
                    && !pet.entityTypeId().toString().toLowerCase(Locale.ROOT).contains(query)) continue;
            result.add(pet);
        }
        return result;
    }

    private PetManagementSummary selectedSummary() {
        if (selectedUuid == null) return null;
        for (PetManagementSummary pet : ClientState.petManagementList()) {
            if (selectedUuid.equals(pet.petUuid())) return pet;
        }
        return null;
    }

    private void actSummon() {
        if (selectedUuid == null) return;
        PetManagementSummary summary = selectedSummary();
        if (summary != null && summary.state() == PetRecordState.UNLOCATED) {
            NetworkHandler.locateManagedPet(selectedUuid);
        } else {
            NetworkHandler.summonManagedPet(selectedUuid);
        }
    }

    private void actStore() {
        if (selectedUuid != null) NetworkHandler.storeManagedPet(selectedUuid);
    }

    private void actView() {
        if (selectedUuid == null || minecraft == null || minecraft.level == null) return;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (selectedUuid.equals(entity.getUUID()) && entity instanceof LivingEntity living) {
                NetworkHandler.openPanel(living.getId());
                return;
            }
        }
    }

    private void actRemoveDead() {
        if (selectedUuid == null || minecraft == null) return;
        UUID petUuid = selectedUuid;
        minecraft.setScreen(new TdmcConfirmScreen(accepted -> {
            if (minecraft == null) return;
            minecraft.setScreen(this);
            if (accepted) NetworkHandler.removeDeadPetRecord(petUuid);
        },
                Component.translatable("gui.tl_domesticate_more_creatures.pet_management.confirm_remove_dead.title"),
                Component.translatable("gui.tl_domesticate_more_creatures.pet_management.confirm_remove_dead.message"),
                Component.translatable("gui.tl_domesticate_more_creatures.pet_management.confirm_remove_dead.confirm"),
                Component.translatable("gui.tl_domesticate_more_creatures.pet_management.confirm_remove_dead.cancel")));
    }

    private static Component statusText(PetRecordState state) {
        String suffix = switch (state) {
            case WORLD -> "world";
            case STORED -> "stored";
            case SUMMONING -> "summoning";
            case DEAD -> "dead";
            case UNLOCATED -> "unlocated";
        };
        return Component.translatable("gui.tl_domesticate_more_creatures.pet_management.status." + suffix);
    }

    private static int statusColor(PetRecordState state) {
        return switch (state) {
            case WORLD -> 0xFF8FE88F;
            case STORED -> 0xFF8FC8FF;
            case SUMMONING -> 0xFFFFD37A;
            case DEAD -> 0xFFFF7C7C;
            case UNLOCATED -> 0xFFFFB36B;
        };
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    public enum Filter {
        ALL("gui.tl_domesticate_more_creatures.pet_management.filter.all"),
        WORLD("gui.tl_domesticate_more_creatures.pet_management.filter.world"),
        STORED("gui.tl_domesticate_more_creatures.pet_management.filter.stored"),
        DEAD("gui.tl_domesticate_more_creatures.pet_management.filter.dead");

        private final String translationKey;

        Filter(String translationKey) {
            this.translationKey = translationKey;
        }

        boolean matches(PetRecordState state) {
            return this == ALL || (this == WORLD && (state == PetRecordState.WORLD || state == PetRecordState.SUMMONING || state == PetRecordState.UNLOCATED))
                    || (this == STORED && state == PetRecordState.STORED)
                    || (this == DEAD && state == PetRecordState.DEAD);
        }
    }
}
