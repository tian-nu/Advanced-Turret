package com.tian_nu.AdvancedTurret.gui;

import com.tian_nu.AdvancedTurret.ConfigManager;
import com.tian_nu.AdvancedTurret.items.SmartChipItem;
import com.tian_nu.AdvancedTurret.items.SmartChipItem.TargetMode;
import com.tian_nu.AdvancedTurret.network.ModNetwork;
import com.tian_nu.AdvancedTurret.network.SmartChipConfigPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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

    private TechEditBox blacklistInput;
    private TechEditBox whitelistInput;

    private TechCheckbox friendlyFireCheckbox;
    private TechCheckbox predictiveAimingCheckbox;
    private TechCheckbox thriftyModeCheckbox;

    private TechCheckbox hostileCheckbox;
    private TechCheckbox neutralCheckbox;
    private TechCheckbox friendlyCheckbox;
    private TechCheckbox playersCheckbox;

    private List<String> recentScans = new ArrayList<>();
    private int recentScanIndex = 0;
    private Button recentPrevButton;
    private Button recentNextButton;
    private Button addRecentToBlacklistButton;
    private Button addRecentToWhitelistButton;

    private int panelX;
    private int panelY;
    private static final int PANEL_W = 360;
    private static final int PANEL_H = 272;

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

        int flagsY = panelY + 42;
        this.hostileCheckbox = new TechCheckbox(panelX + 14, flagsY, 74, 20,
            Component.translatable("gui.advanced_turret.target_mode.hostile"), (targetFlags & SmartChipItem.FLAG_HOSTILE) != 0);
        this.neutralCheckbox = new TechCheckbox(panelX + 96, flagsY, 74, 20,
            Component.translatable("gui.advanced_turret.target_mode.neutral"), (targetFlags & SmartChipItem.FLAG_NEUTRAL) != 0);
        this.friendlyCheckbox = new TechCheckbox(panelX + 178, flagsY, 74, 20,
            Component.translatable("gui.advanced_turret.target_mode.friendly"), (targetFlags & SmartChipItem.FLAG_FRIENDLY) != 0);
        this.playersCheckbox = new TechCheckbox(panelX + 260, flagsY, 74, 20,
            Component.translatable("gui.advanced_turret.target_mode.players"), (targetFlags & SmartChipItem.FLAG_PLAYERS) != 0);
        addRenderableWidget(hostileCheckbox);
        addRenderableWidget(neutralCheckbox);
        addRenderableWidget(friendlyCheckbox);
        addRenderableWidget(playersCheckbox);

        int togglesY = panelY + 94;
        this.friendlyFireCheckbox = new TechCheckbox(panelX + 14, togglesY, 108, 20,
            Component.translatable("gui.advanced_turret.friendly_fire"), friendlyFire);
        this.predictiveAimingCheckbox = new TechCheckbox(panelX + 128, togglesY, 108, 20,
            Component.translatable("gui.advanced_turret.predictive_aiming"), predictiveAiming);
        this.thriftyModeCheckbox = new TechCheckbox(panelX + 242, togglesY, 104, 20,
            Component.translatable("gui.advanced_turret.thrifty_mode"), thriftyMode);
        addRenderableWidget(friendlyFireCheckbox);
        addRenderableWidget(predictiveAimingCheckbox);
        addRenderableWidget(thriftyModeCheckbox);

        this.recentScans = ConfigManager.getRecentEntityScans();
        if (!recentScans.isEmpty()) {
            recentScanIndex = Math.max(0, Math.min(recentScanIndex, recentScans.size() - 1));
        }

        int recentY = panelY + 146;
        this.recentPrevButton = addRenderableWidget(TechButton.builder(Component.translatable("gui.advanced_turret.entity_analyzer.prev"), b -> {
            if (!recentScans.isEmpty()) {
                recentScanIndex = (recentScanIndex - 1 + recentScans.size()) % recentScans.size();
                refreshRecentButtons();
            }
        }).bounds(panelX + 150, recentY, 44, 18).build());

        this.recentNextButton = addRenderableWidget(TechButton.builder(Component.translatable("gui.advanced_turret.entity_analyzer.next"), b -> {
            if (!recentScans.isEmpty()) {
                recentScanIndex = (recentScanIndex + 1) % recentScans.size();
                refreshRecentButtons();
            }
        }).bounds(panelX + 198, recentY, 44, 18).build());

        this.addRecentToBlacklistButton = addRenderableWidget(TechButton.builder(Component.translatable("gui.advanced_turret.add_to_blacklist"), b -> addSelectedRecentToInput(blacklistInput))
            .bounds(panelX + 246, recentY, 48, 18)
            .build());

        this.addRecentToWhitelistButton = addRenderableWidget(TechButton.builder(Component.translatable("gui.advanced_turret.add_to_whitelist"), b -> addSelectedRecentToInput(whitelistInput))
            .bounds(panelX + 298, recentY, 48, 18)
            .build());

        this.blacklistInput = new TechEditBox(this.font, panelX + 14, panelY + 204, 156, 18,
            Component.translatable("gui.advanced_turret.blacklist_label"));
        this.blacklistInput.setMaxLength(1024);
        this.blacklistInput.setValue(String.join(",", SmartChipItem.getBlacklist(stack)));
        addRenderableWidget(blacklistInput);

        this.whitelistInput = new TechEditBox(this.font, panelX + 190, panelY + 204, 156, 18,
            Component.translatable("gui.advanced_turret.whitelist_label"));
        this.whitelistInput.setMaxLength(1024);
        this.whitelistInput.setValue(String.join(",", SmartChipItem.getWhitelist(stack)));
        addRenderableWidget(whitelistInput);

        addRenderableWidget(TechButton.builder(Component.translatable("gui.done"), b -> saveAndClose())
            .bounds(panelX + PANEL_W - 80, panelY + PANEL_H - 24, 66, 18)
            .build());

        refreshRecentButtons();
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
        TurretUiTheme.drawSection(guiGraphics, panelX + 10, panelY + 36, PANEL_W - 20, 32, panelAlpha);
        TurretUiTheme.drawSection(guiGraphics, panelX + 10, panelY + 88, PANEL_W - 20, 32, panelAlpha);
        TurretUiTheme.drawSection(guiGraphics, panelX + 10, panelY + 140, PANEL_W - 20, 32, panelAlpha);
        TurretUiTheme.drawSection(guiGraphics, panelX + 10, panelY + 196, 164, 40, panelAlpha);
        TurretUiTheme.drawSection(guiGraphics, panelX + 186, panelY + 196, 164, 40, panelAlpha);

        guiGraphics.drawCenteredString(this.font, this.title, panelX + PANEL_W / 2, panelY + 10, TurretUiTheme.COLOR_TEXT);
        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.target_type"), panelX + 14, panelY + 26, TurretUiTheme.COLOR_TEXT_SUB, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.behavior_toggles"), panelX + 14, panelY + 76, TurretUiTheme.COLOR_TEXT_SUB, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.entity_analyzer.recent"), panelX + 14, panelY + 128, TurretUiTheme.COLOR_TEXT_SUB, false);

        String currentRecent = getCurrentRecentScan();
        int idColor = currentRecent == null ? TurretUiTheme.COLOR_TEXT_SUB : TurretUiTheme.COLOR_ACCENT;
        Component idText = currentRecent == null
            ? Component.translatable("gui.advanced_turret.entity_analyzer.recent.empty")
            : Component.literal(currentRecent);
        guiGraphics.drawString(this.font, idText, panelX + 14, panelY + 152, idColor, false);

        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.blacklist_label"), panelX + 14, panelY + 184, TurretUiTheme.COLOR_TEXT_SUB, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.whitelist_label"), panelX + 190, panelY + 184, TurretUiTheme.COLOR_TEXT_SUB, false);

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
        } else if (addRecentToBlacklistButton != null && addRecentToBlacklistButton.isMouseOver(mouseX, mouseY)) {
            tooltip.add(TurretUiTheme.tipTitle(Component.translatable("gui.advanced_turret.add_to_blacklist").getString()));
            tooltip.add(TurretUiTheme.tipInfo(Component.translatable("gui.advanced_turret.entity_analyzer.recent.tooltip").getString()));
        } else if (addRecentToWhitelistButton != null && addRecentToWhitelistButton.isMouseOver(mouseX, mouseY)) {
            tooltip.add(TurretUiTheme.tipTitle(Component.translatable("gui.advanced_turret.add_to_whitelist").getString()));
            tooltip.add(TurretUiTheme.tipInfo(Component.translatable("gui.advanced_turret.entity_analyzer.recent.tooltip").getString()));
        }

        if (!tooltip.isEmpty()) {
            guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    private void addSelectedRecentToInput(EditBox input) {
        if (input == null) {
            return;
        }
        String selected = getCurrentRecentScan();
        if (selected == null || selected.isBlank()) {
            return;
        }
        input.setValue(appendCommaSeparatedUnique(input.getValue(), selected));
    }

    private String appendCommaSeparatedUnique(String original, String entityId) {
        List<String> entries = Arrays.stream(original.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toCollection(ArrayList::new));

        if (!entries.contains(entityId)) {
            entries.add(entityId);
        }
        return String.join(",", entries);
    }

    private String getCurrentRecentScan() {
        if (recentScans == null || recentScans.isEmpty()) {
            return null;
        }
        if (recentScanIndex < 0 || recentScanIndex >= recentScans.size()) {
            recentScanIndex = 0;
        }
        return recentScans.get(recentScanIndex);
    }

    private void refreshRecentButtons() {
        boolean hasData = recentScans != null && !recentScans.isEmpty();
        if (recentPrevButton != null) {
            recentPrevButton.active = hasData;
        }
        if (recentNextButton != null) {
            recentNextButton.active = hasData;
        }
        if (addRecentToBlacklistButton != null) {
            addRecentToBlacklistButton.active = hasData;
        }
        if (addRecentToWhitelistButton != null) {
            addRecentToWhitelistButton.active = hasData;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}