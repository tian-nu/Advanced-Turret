package com.tian_nu.AdvancedTurret.gui;

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
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private byte enabledFacesMask;

    private EditBox blacklistInput;
    private EditBox whitelistInput;

    private Checkbox friendlyFireCheckbox;
    private Checkbox predictiveAimingCheckbox;
    private Checkbox thriftyModeCheckbox;

    private Checkbox hostileCheckbox;
    private Checkbox neutralCheckbox;
    private Checkbox friendlyCheckbox;
    private Checkbox playersCheckbox;

    private final Map<Direction, Button> faceButtons = new HashMap<>();

    private int panelX;
    private int panelY;
    private static final int PANEL_W = 360;
    private static final int PANEL_H = 238;

    public SmartChipConfigScreen(ItemStack stack, BlockPos pos) {
        super(Component.translatable("gui.advanced_turret.smart_config"));
        this.stack = stack;
        this.pos = pos;

        this.targetFlags = SmartChipItem.getTargetFlags(stack);
        this.friendlyFire = SmartChipItem.isFriendlyFire(stack);
        this.predictiveAiming = SmartChipItem.isPredictiveAiming(stack);
        this.thriftyMode = SmartChipItem.isThriftyMode(stack);
        this.enabledFacesMask = SmartChipItem.getEnabledFaces(stack);
    }

    @Override
    protected void init() {
        panelX = (this.width - PANEL_W) / 2;
        panelY = (this.height - PANEL_H) / 2;

        int flagsY = panelY + 32;
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

        int togglesY = panelY + 62;
        this.friendlyFireCheckbox = new Checkbox(panelX + 14, togglesY, 108, 20,
            Component.translatable("gui.advanced_turret.friendly_fire"), friendlyFire);
        this.predictiveAimingCheckbox = new Checkbox(panelX + 128, togglesY, 108, 20,
            Component.translatable("gui.advanced_turret.predictive_aiming"), predictiveAiming);
        this.thriftyModeCheckbox = new Checkbox(panelX + 242, togglesY, 104, 20,
            Component.translatable("gui.advanced_turret.thrifty_mode"), thriftyMode);
        addRenderableWidget(friendlyFireCheckbox);
        addRenderableWidget(predictiveAimingCheckbox);
        addRenderableWidget(thriftyModeCheckbox);

        int faceY = panelY + 98;
        int btnSize = 20;
        int faceStartX = panelX + 14;
        Direction[] directions = {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        String[] labels = {"U", "D", "N", "S", "W", "E"};

        for (int i = 0; i < directions.length; i++) {
            Direction dir = directions[i];
            int x = faceStartX + i * (btnSize + 6);
            int y = faceY + 14;

            Button btn = Button.builder(Component.literal(labels[i]), b -> {
                boolean enabled = (enabledFacesMask & (1 << dir.get3DDataValue())) != 0;
                if (enabled) {
                    enabledFacesMask &= (byte) ~(1 << dir.get3DDataValue());
                } else {
                    enabledFacesMask |= (byte) (1 << dir.get3DDataValue());
                }
            }).bounds(x, y, btnSize, btnSize).build();

            faceButtons.put(dir, btn);
            addRenderableWidget(btn);
        }

        int listY = panelY + 140;
        this.blacklistInput = new EditBox(this.font, panelX + 14, listY + 12, PANEL_W - 28, 18,
            Component.translatable("gui.advanced_turret.blacklist_label"));
        this.blacklistInput.setMaxLength(1024);
        this.blacklistInput.setValue(String.join(",", SmartChipItem.getBlacklist(stack)));
        addRenderableWidget(blacklistInput);

        this.whitelistInput = new EditBox(this.font, panelX + 14, listY + 44, PANEL_W - 28, 18,
            Component.translatable("gui.advanced_turret.whitelist_label"));
        this.whitelistInput.setMaxLength(1024);
        this.whitelistInput.setValue(String.join(",", SmartChipItem.getWhitelist(stack)));
        addRenderableWidget(whitelistInput);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> saveAndClose())
            .bounds(panelX + PANEL_W / 2 - 42, panelY + PANEL_H - 24, 84, 18)
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
            enabledFacesMask,
            blacklist,
            whitelist,
            flags
        ));

        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        TurretUiTheme.drawPanel(guiGraphics, panelX, panelY, PANEL_W, PANEL_H);
        TurretUiTheme.drawSection(guiGraphics, panelX + 10, panelY + 24, PANEL_W - 20, 60);
        TurretUiTheme.drawSection(guiGraphics, panelX + 10, panelY + 90, PANEL_W - 20, 44);
        TurretUiTheme.drawSection(guiGraphics, panelX + 10, panelY + 138, PANEL_W - 20, 72);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, panelY + 8, TurretUiTheme.COLOR_TEXT);
        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.target_type"), panelX + 14, panelY + 28, TurretUiTheme.COLOR_TEXT_SUB, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.behavior_toggles"), panelX + 14, panelY + 58, TurretUiTheme.COLOR_TEXT_SUB, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.enabled_faces"), panelX + 14, panelY + 94, TurretUiTheme.COLOR_TEXT_SUB, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.blacklist_label"), panelX + 14, panelY + 142, TurretUiTheme.COLOR_TEXT_SUB, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.whitelist_label"), panelX + 14, panelY + 174, TurretUiTheme.COLOR_TEXT_SUB, false);

        faceButtons.forEach((dir, btn) -> {
            boolean enabled = (enabledFacesMask & (1 << dir.get3DDataValue())) != 0;
            int color = enabled ? 0x4000C86D : 0x40A33A3A;
            guiGraphics.fill(btn.getX(), btn.getY(), btn.getX() + btn.getWidth(), btn.getY() + btn.getHeight(), color);
        });

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
        } else {
            for (Map.Entry<Direction, Button> entry : faceButtons.entrySet()) {
                Button btn = entry.getValue();
                if (!btn.isMouseOver(mouseX, mouseY)) {
                    continue;
                }
                boolean enabled = (enabledFacesMask & (1 << entry.getKey().get3DDataValue())) != 0;
                tooltip.add(TurretUiTheme.tipTitle(Component.translatable("gui.advanced_turret.face_prefix", btn.getMessage()).getString()));
                tooltip.add(enabled ? TurretUiTheme.tipOk(Component.translatable("gui.turret_config.enabled").getString()) : TurretUiTheme.tipDanger(Component.translatable("gui.turret_config.disabled").getString()));
                break;
            }
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