package com.szypxj.tldomesticatemorecreatures.client.pet;

import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommand;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandSummary;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.BooleanSupplier;

public final class PetCommandRadialScreen extends Screen {
    private static final List<PetCommand> BASE_COMMANDS = List.of(
            PetCommand.ATTACK,
            PetCommand.MOVE,
            PetCommand.FOLLOW,
            PetCommand.DEFEND,
            PetCommand.RETREAT
    );
    private static final List<PetCommand> FLIGHT_COMMANDS = List.of(
            PetCommand.ATTACK,
            PetCommand.MOVE,
            PetCommand.FOLLOW,
            PetCommand.DEFEND,
            PetCommand.RETREAT,
            PetCommand.LAND
    );

    private final PetCommandTargetSnapshot target;
    private final boolean persistentSelection;
    private final BooleanSupplier holdKeyDown;
    private int highlightedIndex = -1;
    private boolean centerSelected;
    private int centerX;
    private int centerY;
    private boolean closed;
    private boolean cancelled;

    public PetCommandRadialScreen(PetCommandTargetSnapshot target, BooleanSupplier holdKeyDown) {
        this(target, false, holdKeyDown);
    }

    public PetCommandRadialScreen(PetCommandTargetSnapshot target, boolean persistentSelection) {
        this(target, persistentSelection, null);
    }

    private PetCommandRadialScreen(PetCommandTargetSnapshot target, boolean persistentSelection, BooleanSupplier holdKeyDown) {
        super(Component.translatable("gui.tl_domesticate_more_creatures.pet_command.title"));
        this.target = target;
        this.persistentSelection = persistentSelection;
        this.holdKeyDown = holdKeyDown;
    }

    @Override
    protected void init() {
        super.init();
        centerX = width / 2;
        centerY = height / 2;
        NetworkHandler.requestPetCommandSummary();
        centerCursor();
    }

    @Override
    public void tick() {
        if (!persistentSelection && !closed && (holdKeyDown == null || !holdKeyDown.getAsBoolean())) {
            finishSelection();
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (closed) {
            return;
        }
        boolean activeCommand = hasActiveCommand();
        List<PetCommand> commands = commands();
        updateSelection(mouseX, mouseY, activeCommand, commands.size());
        PetCommandRadialRenderer.draw(
                graphics.pose(), centerX, centerY,
                PetCommandRadialRenderer.DEFAULT_RADIUS,
                PetCommandRadialRenderer.DEFAULT_DEAD_ZONE,
                commands.size(), highlightedIndex, activeCommand, centerSelected
        );
        renderLabels(graphics, activeCommand, commands);
    }

    private void updateSelection(int mouseX, int mouseY, boolean activeCommand, int commandCount) {
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.hypot(dx, dy);
        float cancelRadius = PetCommandRadialRenderer.cancelRadius(
                PetCommandRadialRenderer.DEFAULT_RADIUS,
                PetCommandRadialRenderer.DEFAULT_DEAD_ZONE
        );
        if (activeCommand && distance <= cancelRadius) {
            centerSelected = true;
            highlightedIndex = -1;
            return;
        }

        centerSelected = false;
        float innerRadius = PetCommandRadialRenderer.innerRadius(
                PetCommandRadialRenderer.DEFAULT_RADIUS,
                PetCommandRadialRenderer.DEFAULT_DEAD_ZONE
        );
        if (distance < innerRadius) {
            highlightedIndex = -1;
            return;
        }

        double angle = Math.atan2(dy, dx) + Math.PI / 2.0D;
        if (angle < 0.0D) {
            angle += Math.PI * 2.0D;
        }
        if (angle >= Math.PI * 2.0D) {
            angle -= Math.PI * 2.0D;
        }
        highlightedIndex = Math.min(
                (int) (angle / (Math.PI * 2.0D / commandCount)),
                commandCount - 1
        );
    }

    private void renderLabels(GuiGraphics graphics, boolean activeCommand, List<PetCommand> commands) {
        int count = commands.size();
        double sectorAngle = Math.PI * 2.0D / count;
        float textRadius = PetCommandRadialRenderer.labelRadius(
                PetCommandRadialRenderer.DEFAULT_RADIUS,
                PetCommandRadialRenderer.DEFAULT_DEAD_ZONE
        );
        for (int i = 0; i < count; i++) {
            double middle = (i + 0.5D) * sectorAngle - Math.PI / 2.0D;
            int x = (int) (centerX + textRadius * Math.cos(middle));
            int y = (int) (centerY + textRadius * Math.sin(middle));
            graphics.drawCenteredString(
                    font,
                    commandName(commands.get(i)),
                    x,
                    y - font.lineHeight / 2,
                    0xFFFFFFFF
            );
        }

        if (activeCommand) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.tl_domesticate_more_creatures.pet_command.cancel"),
                    centerX,
                    centerY - font.lineHeight / 2,
                    0xFFFFFFFF
            );
        }
        graphics.drawCenteredString(
                font,
                title,
                centerX,
                centerY - PetCommandRadialRenderer.DEFAULT_RADIUS - 18,
                0xFFFFFFFF
        );
    }

    private static boolean hasActiveCommand() {
        return ClientState.petCommandSummary() != PetCommandSummary.NONE;
    }

    private static List<PetCommand> commands() {
        return ClientState.petCommandHasLandCapablePets() ? FLIGHT_COMMANDS : BASE_COMMANDS;
    }

    private static Component commandName(PetCommand command) {
        return Component.translatable("gui.tl_domesticate_more_creatures.pet_command." + command.name().toLowerCase());
    }

    private void finishSelection() {
        if (closed) {
            return;
        }
        closed = true;
        if (cancelled) {
            return;
        }
        if (centerSelected && hasActiveCommand()) {
            NetworkHandler.clearPetCommand();
            return;
        }
        if (highlightedIndex < 0) {
            return;
        }
        List<PetCommand> commands = commands();
        if (highlightedIndex >= commands.size()) {
            return;
        }
        PetCommand command = commands.get(highlightedIndex);
        if (command == PetCommand.ATTACK && target.entityId() < 0) {
            return;
        }
        NetworkHandler.issuePetCommand(command, target.entityId(), target.position());
    }

    @Override
    public void removed() {
        if (!persistentSelection) {
            finishSelection();
        }
        super.removed();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (persistentSelection && button == 0 && !closed) {
            finishSelection();
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancelled = true;
            closed = true;
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void centerCursor() {
        if (minecraft == null) {
            return;
        }
        GLFW.glfwSetCursorPos(
                minecraft.getWindow().getWindow(),
                minecraft.getWindow().getScreenWidth() / 2.0D,
                minecraft.getWindow().getScreenHeight() / 2.0D
        );
    }
}
