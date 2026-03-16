package com.tian_nu.AdvancedTurret.gui;

import com.tian_nu.AdvancedTurret.ConfigManager;
import com.tian_nu.AdvancedTurret.items.SmartChipItem;
import com.tian_nu.AdvancedTurret.items.SmartChipItem.TargetMode;
import com.tian_nu.AdvancedTurret.network.ModNetwork;
import com.tian_nu.AdvancedTurret.network.SmartChipConfigPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 智能芯片配置界面
 */
public class SmartChipConfigScreen extends Screen {
    private final ItemStack stack;
    private final BlockPos pos;

    private int targetFlags;
    private boolean friendlyFire;
    private boolean predictiveAiming;
    private boolean thriftyMode;

    private EditBox blacklistInput;
    private EditBox whitelistInput;

    private Checkbox friendlyFireCheckbox;
    private Checkbox predictiveAimingCheckbox;
    private Checkbox thriftyModeCheckbox;

    private Checkbox hostileCheckbox;
    private Checkbox neutralCheckbox;
    private Checkbox friendlyCheckbox;
    private Checkbox playersCheckbox;

    private int panelX;
    private int panelY;
    private static final int PANEL_W = 360;
    private static final int PANEL_H = 236;

    public SmartChipConfigScreen(ItemStack stack, BlockPos pos) {
        super(Component.translatable("gui.advanced_turret.smart_config"));
        this.stack = stack;
        this.pos = pos;

        this.targetFlags = SmartChipItem.getTargetFlags(stack);
        this.friendlyFire = SmartChipItem.isFriendlyFire(stack);
        this.predictiveAiming = SmartChipItem.isPredictiveAiming(stack);
        this.thriftyMode = SmartChipItem.isThriftyMode(stack);
    }

    @Override
    protected void init() {
        panelX = (this.width - PANEL_W) / 2;
        panelY = (this.height - PANEL_H) / 2;

        int flagsY = panelY + 44;
        this.hostileCheckbox = new Checkbox(panelX + 14, flagsY, 74, 20,
            Component.translatable("gui.advanced_turret.target_mode.hostile"), (targetFlags & SmartChipItem.FLAG_HOSTILE) != 0);
        this.neutralCheckbox = new Checkbox(panelX + 96, flagsY, 74, 20,
            Component.translatable("gui.advanced_turret.target_mode.neutral"), (targetFlags & SmartChipItem.FLAG_NEUTRAL) != 0);
        this.friendlyCheckbox = new Checkbox(panelX + 178, flagsY, 74, 20,
            Component.translatable("gui.advanced_turret.target_mode.friendly"), (targetFlags & SmartChipItem.FLAG_FRIENDLY) != 0);
        this.playersCheckbox = new Checkbox(panelX + 260, flagsY, 74, 20,
            Component.translatable("gui.advanced_turret.target_mode.players"), (targetFlags & SmartChipItem.FLAG_PLAYERS) != 0);
        addRenderableWidget(hostileCheckbox);
        addRenderableWidget(neutralCheckbox);
        addRenderableWidget(friendlyCheckbox);
        addRenderableWidget(playersCheckbox);

        int togglesY = panelY + 94;
        this.friendlyFireCheckbox = new Checkbox(panelX + 14, togglesY, 108, 20,
            Component.translatable("gui.advanced_turret.friendly_fire"), friendlyFire);
        this.predictiveAimingCheckbox = new Checkbox(panelX + 128, togglesY, 108, 20,
            Component.translatable("gui.advanced_turret.predictive_aiming"), predictiveAiming);
        this.thriftyModeCheckbox = new Checkbox(panelX + 242, togglesY, 104, 20,
            Component.translatable("gui.advanced_turret.thrifty_mode"), thriftyMode);
        addRenderableWidget(friendlyFireCheckbox);
        addRenderableWidget(predictiveAimingCheckbox);
        addRenderableWidget(thriftyModeCheckbox);

        this.blacklistInput = new EditBox(this.font, panelX + 14, panelY + 150, PANEL_W - 28, 18,
            Component.translatable("gui.advanced_turret.blacklist_label"));
        this.blacklistInput.setMaxLength(1024);
        this.blacklistInput.setValue(String.join(",", SmartChipItem.getBlacklist(stack)));
        addRenderableWidget(blacklistInput);

        this.whitelistInput = new EditBox(this.font, panelX + 14, panelY + 190, PANEL_W - 28, 18,
            Component.translatable("gui.advanced_turret.whitelist_label"));
        this.whitelistInput.setMaxLength(1024);
        this.whitelistInput.setValue(String.join(",", SmartChipItem.getWhitelist(stack)));
        addRenderableWidget(whitelistInput);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> saveAndClose())
            .bounds(panelX + PANEL_W - 80, panelY + PANEL_H - 24, 66, 18)
            .build());
    }

    private void saveAndClose() {
        List<String> blacklist = Arrays.stream(blacklistInput.getValue().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());

        List<String> whitelist = Arrays.stream(whitelistInput.getValue().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());

        int flags = 0;
        if (hostileCheckbox.selected()) flags |= SmartChipItem.FLAG_HOSTILE;
        if (neutralCheckbox.selected()) flags |= SmartChipItem.FLAG_NEUTRAL;
        if (friendlyCheckbox.selected()) flags |= SmartChipItem.FLAG_FRIENDLY;
        if (playersCheckbox.selected()) flags |= SmartChipItem.FLAG_PLAYERS;

        ModNetwork.CHANNEL.sendToServer(new SmartChipConfigPacket(
            pos,
            TargetMode.HOSTILE,
            friendlyFireCheckbox.selected(),
            predictiveAimingCheckbox.selected(),
            thriftyModeCheckbox.selected(),
            (byte) 0b111111,
            blacklist,
            whitelist,
            flags
        ));

        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        float panelAlpha = Math.max(0.15F, ConfigManager.getBackgroundAlpha());
        TurretUiTheme.drawPanel(guiGraphics, panelX, panelY, PANEL_W, PANEL_H, panelAlpha);
        TurretUiTheme.drawSection(guiGraphics, panelX + 10, panelY + 40, PANEL_W - 20, 28, panelAlpha);
        TurretUiTheme.drawSection(guiGraphics, panelX + 10, panelY + 90, PANEL_W - 20, 28, panelAlpha);
        TurretUiTheme.drawSection(guiGraphics, panelX + 10, panelY + 146, PANEL_W - 20, 26, panelAlpha);
        TurretUiTheme.drawSection(guiGraphics, panelX + 10, panelY + 186, PANEL_W - 20, 26, panelAlpha);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, panelY + 8, TurretUiTheme.COLOR_TEXT);
        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.target_type"), panelX + 14, panelY + 24, TurretUiTheme.COLOR_TEXT_SUB, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.behavior_toggles"), panelX + 14, panelY + 78, TurretUiTheme.COLOR_TEXT_SUB, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.blacklist_label"), panelX + 14, panelY + 136, TurretUiTheme.COLOR_TEXT_SUB, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.whitelist_label"), panelX + 14, panelY + 176, TurretUiTheme.COLOR_TEXT_SUB, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderStateTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderStateTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();

        if (friendlyFireCheckbox != null && friendlyFireCheckbox.isMouseOver(mouseX, mouseY)) {
            tooltip.add(TurretUiTheme.tipTitle(Component.translatable("gui.advanced_turret.friendly_fire").getString()));
            tooltip.add(friendlyFireCheckbox.selected() ? TurretUiTheme.tipWarn(Component.translatable("gui.turret_config.enabled").getString()) : TurretUiTheme.tipOk(Component.translatable("gui.turret_config.disabled").getString()));
        } else if (predictiveAimingCheckbox != null && predictiveAimingCheckbox.isMouseOver(mouseX, mouseY)) {
            tooltip.add(TurretUiTheme.tipTitle(Component.translatable("gui.advanced_turret.predictive_aiming").getString()));
            tooltip.add(predictiveAimingCheckbox.selected() ? TurretUiTheme.tipOk(Component.translatable("gui.turret_config.enabled").getString()) : TurretUiTheme.tipInfo(Component.translatable("gui.turret_config.disabled").getString()));
        } else if (thriftyModeCheckbox != null && thriftyModeCheckbox.isMouseOver(mouseX, mouseY)) {
            tooltip.add(TurretUiTheme.tipTitle(Component.translatable("gui.advanced_turret.thrifty_mode").getString()));
            tooltip.add(thriftyModeCheckbox.selected() ? TurretUiTheme.tipOk(Component.translatable("gui.turret_config.enabled").getString()) : TurretUiTheme.tipInfo(Component.translatable("gui.turret_config.disabled").getString()));
        }

        if (!tooltip.isEmpty()) {
            guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}