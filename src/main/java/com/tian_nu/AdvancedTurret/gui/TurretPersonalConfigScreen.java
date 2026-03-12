package com.tian_nu.AdvancedTurret.gui;

import com.tian_nu.AdvancedTurret.ConfigManager;
import com.tian_nu.AdvancedTurret.blocks.entitys.TurretBaseBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.TurretPersonalConfig;
import com.tian_nu.AdvancedTurret.network.ModNetwork;
import com.tian_nu.AdvancedTurret.network.TurretFaceEnableConfigPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 炮塔个性化配置屏幕
 */
public class TurretPersonalConfigScreen extends Screen {

    private final TurretScreen parentScreen;
    private final TurretBaseBlockEntity blockEntity;
    private final TurretPersonalConfig config;
    private final Map<Direction, FaceToggleButton> faceButtons = new LinkedHashMap<>();

    private static final int GUI_WIDTH = 250;
    private static final int GUI_HEIGHT = 220;

    private EditBox backgroundAlphaField;
    private EditBox energyAlphaField;
    private byte enabledFacesMask;

    public TurretPersonalConfigScreen(TurretScreen parentScreen, TurretBaseBlockEntity blockEntity) {
        super(Component.translatable("gui.turret_personal_config.title"));
        this.parentScreen = parentScreen;
        this.blockEntity = blockEntity;
        this.config = new TurretPersonalConfig(
            parentScreen.getBackgroundAlpha(),
            parentScreen.getEnergyBarAlpha(),
            27, 2, 1, 1
        );
        this.enabledFacesMask = blockEntity.getEnabledFacesMask();
    }

    @Override
    protected void init() {
        super.init();
        int guiX = (this.width - GUI_WIDTH) / 2;
        int guiY = (this.height - GUI_HEIGHT) / 2;
        createInputFields(guiX, guiY);
        createFaceButtons(guiX, guiY);
        createActionButtons(guiX, guiY);
    }

    private void createInputFields(int guiX, int guiY) {
        backgroundAlphaField = new EditBox(this.font, guiX + 148, guiY + 36, 48, 18, Component.empty());
        backgroundAlphaField.setMaxLength(4);
        backgroundAlphaField.setValue(formatAlpha(config.getBackgroundAlpha()));
        this.addRenderableWidget(backgroundAlphaField);

        energyAlphaField = new EditBox(this.font, guiX + 148, guiY + 60, 48, 18, Component.empty());
        energyAlphaField.setMaxLength(4);
        energyAlphaField.setValue(formatAlpha(config.getEnergyBarAlpha()));
        this.addRenderableWidget(energyAlphaField);
    }

    private void createFaceButtons(int guiX, int guiY) {
        Direction[] directions = {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        String[] labels = {"U", "D", "N", "S", "W", "E"};
        int startX = guiX + 22;
        int y = guiY + 118;

        for (int i = 0; i < directions.length; i++) {
            Direction direction = directions[i];
            FaceToggleButton button = new FaceToggleButton(startX + i * 34, y, labels[i], direction);
            faceButtons.put(direction, button);
            addRenderableWidget(button);
        }
    }

    private void createActionButtons(int guiX, int guiY) {
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.done"),
            button -> applyChanges()
        ).bounds(guiX + 28, guiY + GUI_HEIGHT - 30, 60, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.reset"),
            button -> resetToDefault()
        ).bounds(guiX + 96, guiY + GUI_HEIGHT - 30, 60, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.cancel"),
            button -> onClose()
        ).bounds(guiX + 164, guiY + GUI_HEIGHT - 30, 60, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int guiX = (this.width - GUI_WIDTH) / 2;
        int guiY = (this.height - GUI_HEIGHT) / 2;
        float panelAlpha = Math.max(0.15F, ConfigManager.getBackgroundAlpha());

        TurretUiTheme.drawPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, panelAlpha);
        TurretUiTheme.drawSection(guiGraphics, guiX + 12, guiY + 26, GUI_WIDTH - 24, 64, panelAlpha);
        TurretUiTheme.drawSection(guiGraphics, guiX + 12, guiY + 102, GUI_WIDTH - 24, 46, panelAlpha);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, guiY + 10, TurretUiTheme.COLOR_TEXT);
        renderLabels(guiGraphics, guiX, guiY);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderFaceTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderLabels(GuiGraphics guiGraphics, int guiX, int guiY) {
        guiGraphics.drawString(this.font, Component.translatable("gui.turret_config.background_alpha"), guiX + 22, guiY + 40, TurretUiTheme.COLOR_TEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.turret_config.energy_alpha"), guiX + 22, guiY + 64, TurretUiTheme.COLOR_TEXT, false);
        guiGraphics.drawString(this.font, Component.literal("范围: 0.00 - 1.00"), guiX + 22, guiY + 92, TurretUiTheme.COLOR_TEXT_SUB, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.enabled_faces"), guiX + 22, guiY + 108, TurretUiTheme.COLOR_TEXT_SUB, false);
    }

    private void renderFaceTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (FaceToggleButton button : faceButtons.values()) {
            if (!button.isMouseOver(mouseX, mouseY)) {
                continue;
            }
            guiGraphics.renderTooltip(
                this.font,
                java.util.List.of(
                    TurretUiTheme.tipTitle(button.getFaceName()),
                    button.isFaceEnabled()
                        ? TurretUiTheme.tipOk(Component.translatable("gui.turret_config.enabled").getString())
                        : TurretUiTheme.tipDanger(Component.translatable("gui.turret_config.disabled").getString())
                ),
                java.util.Optional.empty(),
                mouseX,
                mouseY
            );
            return;
        }
    }

    private void applyChanges() {
        try {
            float bgAlpha = Math.max(0.0F, Math.min(1.0F, Float.parseFloat(backgroundAlphaField.getValue())));
            float energyAlpha = Math.max(0.0F, Math.min(1.0F, Float.parseFloat(energyAlphaField.getValue())));

            config.setBackgroundAlpha(bgAlpha);
            config.setEnergyBarAlpha(energyAlpha);

            parentScreen.setBackgroundAlpha(bgAlpha);
            parentScreen.setEnergyBarAlpha(energyAlpha);
            syncEnabledFaces();
            onClose();
        } catch (NumberFormatException ignored) {
        }
    }

    private void syncEnabledFaces() {
        ModNetwork.CHANNEL.sendToServer(new TurretFaceEnableConfigPacket(blockEntity.getBlockPos(), enabledFacesMask));
    }

    private void resetToDefault() {
        config.resetToDefault();
        backgroundAlphaField.setValue(formatAlpha(config.getBackgroundAlpha()));
        energyAlphaField.setValue(formatAlpha(config.getEnergyBarAlpha()));
        enabledFacesMask = 0b111111;
    }

    private String formatAlpha(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class FaceToggleButton extends AbstractButton {
        private final Direction direction;

        private FaceToggleButton(int x, int y, String label, Direction direction) {
            super(x, y, 24, 20, Component.literal(label));
            this.direction = direction;
        }

        @Override
        public void onPress() {
            int bit = 1 << direction.get3DDataValue();
            if ((enabledFacesMask & bit) != 0) {
                enabledFacesMask &= (byte) ~bit;
            } else {
                enabledFacesMask |= (byte) bit;
            }
        }

        private boolean isFaceEnabled() {
            return (enabledFacesMask & (1 << direction.get3DDataValue())) != 0;
        }

        private String getFaceName() {
            return switch (direction) {
                case UP -> "上面";
                case DOWN -> "下面";
                case NORTH -> "北面";
                case SOUTH -> "南面";
                case WEST -> "西面";
                case EAST -> "东面";
            };
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean enabled = isFaceEnabled();
            int baseColor = enabled ? 0xFF245F48 : 0xFF2A2A2A;
            int borderColor = enabled ? 0xFF6BE08D : 0xFF666666;
            int hoverOverlay = this.isHoveredOrFocused() ? 0x22FFFFFF : 0x00000000;
            int textColor = enabled ? TurretUiTheme.COLOR_TEXT : 0xFF9A9A9A;

            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, baseColor);
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, hoverOverlay);
            TurretUiTheme.drawBorder(guiGraphics, this.getX(), this.getY(), this.width, this.height, borderColor);
            guiGraphics.drawCenteredString(TurretPersonalConfigScreen.this.font, this.getMessage(), this.getX() + this.width / 2, this.getY() + 6, textColor);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
    }
}