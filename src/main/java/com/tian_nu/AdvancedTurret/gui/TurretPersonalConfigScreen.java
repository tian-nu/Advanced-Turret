package com.tian_nu.AdvancedTurret.gui;

import com.tian_nu.AdvancedTurret.blocks.entitys.TurretBaseBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.TurretPersonalConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 炮塔个性化配置屏幕
 */
public class TurretPersonalConfigScreen extends Screen {

    private final TurretScreen parentScreen;
    private final TurretBaseBlockEntity blockEntity;
    private final TurretPersonalConfig config;

    private static final int GUI_WIDTH = 250;
    private static final int GUI_HEIGHT = 200;

    private EditBox backgroundAlphaField;
    private EditBox energyAlphaField;

    private boolean autoReload = true;

    public TurretPersonalConfigScreen(TurretScreen parentScreen, TurretBaseBlockEntity blockEntity) {
        super(Component.translatable("gui.turret_personal_config.title"));
        this.parentScreen = parentScreen;
        this.blockEntity = blockEntity;
        this.config = new TurretPersonalConfig(
            parentScreen.getBackgroundAlpha(),
            parentScreen.getEnergyBarAlpha(),
            27, 2, 1, 1
        );
    }

    @Override
    protected void init() {
        super.init();

        int guiX = (this.width - GUI_WIDTH) / 2;
        int guiY = (this.height - GUI_HEIGHT) / 2;

        createInputFields(guiX, guiY);
        createCheckboxButtons(guiX, guiY);
        createActionButtons(guiX, guiY);
        populateInitialValues();
    }

    private void createInputFields(int guiX, int guiY) {
        backgroundAlphaField = new EditBox(this.font, guiX + 130, guiY + 36, 48, 18, Component.empty());
        backgroundAlphaField.setMaxLength(4);
        backgroundAlphaField.setValue(String.format("%.2f", config.getBackgroundAlpha()));
        this.addRenderableWidget(backgroundAlphaField);

        energyAlphaField = new EditBox(this.font, guiX + 130, guiY + 60, 48, 18, Component.empty());
        energyAlphaField.setMaxLength(4);
        energyAlphaField.setValue(String.format("%.2f", config.getEnergyBarAlpha()));
        this.addRenderableWidget(energyAlphaField);
    }

    private void createCheckboxButtons(int guiX, int guiY) {
        this.addRenderableWidget(Button.builder(
            autoReloadLabel(),
            button -> {
                toggleAutoReload();
                button.setMessage(autoReloadLabel());
            }
        ).bounds(guiX + 22, guiY + 90, 206, 20).build());
    }

    private Component autoReloadLabel() {
        return Component.translatable("gui.turret_config.auto_reload")
            .append(": ")
            .append(autoReload
                ? Component.translatable("gui.turret_config.enabled")
                : Component.translatable("gui.turret_config.disabled"));
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

    private void populateInitialValues() {
        autoReload = config.isAutoReload();
    }

    private void toggleAutoReload() {
        autoReload = !autoReload;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int guiX = (this.width - GUI_WIDTH) / 2;
        int guiY = (this.height - GUI_HEIGHT) / 2;

        TurretUiTheme.drawPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT);
        TurretUiTheme.drawSection(guiGraphics, guiX + 12, guiY + 26, GUI_WIDTH - 24, 92);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, guiY + 10, TurretUiTheme.COLOR_TEXT);
        renderLabels(guiGraphics, guiX, guiY);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderLabels(GuiGraphics guiGraphics, int guiX, int guiY) {
        guiGraphics.drawString(this.font, Component.translatable("gui.turret_config.background_alpha"), guiX + 22, guiY + 40, TurretUiTheme.COLOR_TEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.turret_config.energy_alpha"), guiX + 22, guiY + 64, TurretUiTheme.COLOR_TEXT, false);
        guiGraphics.drawString(this.font, Component.literal("范围: 0.00 - 1.00"), guiX + 22, guiY + 122, TurretUiTheme.COLOR_TEXT_SUB, false);
    }

    private void applyChanges() {
        try {
            float bgAlpha = Math.max(0.0F, Math.min(1.0F, Float.parseFloat(backgroundAlphaField.getValue())));
            float energyAlpha = Math.max(0.0F, Math.min(1.0F, Float.parseFloat(energyAlphaField.getValue())));

            config.setBackgroundAlpha(bgAlpha);
            config.setEnergyBarAlpha(energyAlpha);
            config.setAutoReload(autoReload);

            // 暂未接入实体个性化配置持久化，仅同步当前GUI表现
            parentScreen.setBackgroundAlpha(bgAlpha);
            parentScreen.setEnergyBarAlpha(energyAlpha);

            onClose();
        } catch (NumberFormatException ignored) {
        }
    }

    private void resetToDefault() {
        config.resetToDefault();
        populateInitialValues();
        backgroundAlphaField.setValue("1.00");
        energyAlphaField.setValue("1.00");
        autoReload = true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
