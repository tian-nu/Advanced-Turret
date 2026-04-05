package com.tian_nu.AdvancedTurret.gui;

import com.tian_nu.AdvancedTurret.ConfigManager;
import com.tian_nu.AdvancedTurret.network.RemoteTerminalBaseInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 远程终端黑白名单详细配置子界面。
 */
public class RemoteTerminalListConfigScreen extends Screen {

    private static final int PANEL_W = 460;
    private static final int PANEL_H = 300;
    private static final int RECENT_ROWS = 5;

    private final RemoteTerminalScreen parent;
    private final RemoteTerminalBaseInfo baseInfo;
    private final String initialBlacklist;
    private final String initialWhitelist;

    private final List<Button> recentRowButtons = new ArrayList<>();
    private List<String> recentScans = new ArrayList<>();
    private int recentScanIndex = -1;
    private int recentPageIndex = 0;
    private Button recentPrevButton;
    private Button recentNextButton;

    private MultiLineEditBox blacklistInput;
    private MultiLineEditBox whitelistInput;

    private Button addRecentToBlacklistButton;
    private Button addRecentToWhitelistButton;

    private int panelX;
    private int panelY;

    public RemoteTerminalListConfigScreen(RemoteTerminalScreen parent, RemoteTerminalBaseInfo baseInfo, String initialBlacklist, String initialWhitelist) {
        super(Component.translatable("gui.advanced_turret.remote_terminal.list_config"));
        this.parent = parent;
        this.baseInfo = baseInfo;
        this.initialBlacklist = initialBlacklist == null ? "" : initialBlacklist;
        this.initialWhitelist = initialWhitelist == null ? "" : initialWhitelist;
    }

    @Override
    protected void init() {
        this.recentRowButtons.clear();

        panelX = (this.width - PANEL_W) / 2;
        panelY = (this.height - PANEL_H) / 2;

        this.recentScans = ConfigManager.getRecentEntityScans();
        this.recentScanIndex = recentScans.isEmpty() ? -1 : 0;

        int recentX = panelX + 16;
        int recentY = panelY + 52;
        for (int i = 0; i < RECENT_ROWS; i++) {
            final int row = i;
            Button rowButton = addRenderableWidget(TechButton.builder(Component.literal("-"), b -> selectRecentRow(row))
                    .bounds(recentX, recentY + i * 20, 160, 18)
                    .build());
            recentRowButtons.add(rowButton);
        }

        this.recentPrevButton = addRenderableWidget(TechButton.builder(Component.literal("<"), b -> switchRecentPage(-1))
                .bounds(recentX, panelY + 158, 40, 18)
                .build());

        this.recentNextButton = addRenderableWidget(TechButton.builder(Component.literal(">"), b -> switchRecentPage(1))
                .bounds(recentX + 120, panelY + 158, 40, 18)
                .build());

        this.addRecentToBlacklistButton = addRenderableWidget(TechButton.builder(Component.literal("加入黑名单"), b -> addSelectedRecentToInput(blacklistInput))
                .bounds(recentX, panelY + 196, 160, 18)
                .build());

        this.addRecentToWhitelistButton = addRenderableWidget(TechButton.builder(Component.literal("加入白名单"), b -> addSelectedRecentToInput(whitelistInput))
                .bounds(recentX, panelY + 218, 160, 18)
                .build());

        int textX = panelX + 186;
        this.blacklistInput = addRenderableWidget(new MultiLineEditBox(this.font, textX, panelY + 52, 258, 84,
                Component.translatable("gui.advanced_turret.blacklist_label"), Component.empty()));
        this.blacklistInput.setCharacterLimit(2048);
        this.blacklistInput.setValue(normalizeToMultiline(initialBlacklist));

        this.whitelistInput = addRenderableWidget(new MultiLineEditBox(this.font, textX, panelY + 162, 258, 84,
                Component.translatable("gui.advanced_turret.whitelist_label"), Component.empty()));
        this.whitelistInput.setCharacterLimit(2048);
        this.whitelistInput.setValue(normalizeToMultiline(initialWhitelist));

        addRenderableWidget(TechButton.builder(Component.literal("提前保存名单"), b -> saveAndApply())
                .bounds(panelX + PANEL_W - 212, panelY + PANEL_H - 24, 102, 18)
                .build());

        addRenderableWidget(TechButton.builder(Component.translatable("gui.cancel"), b -> this.onClose())
                .bounds(panelX + PANEL_W - 106, panelY + PANEL_H - 24, 44, 18)
                .build());

        addRenderableWidget(TechButton.builder(Component.translatable("gui.done"), b -> this.onClose())
                .bounds(panelX + PANEL_W - 58, panelY + PANEL_H - 24, 44, 18)
                .build());

        refreshRecentButtons();
    }

    private void saveAndApply() {
        String blacklist = normalizeToMultiline(blacklistInput.getValue());
        String whitelist = normalizeToMultiline(whitelistInput.getValue());
        parent.saveListDraftAndApply(baseInfo, blacklist, whitelist);
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        TurretUiTheme.drawPanel(guiGraphics, panelX, panelY, PANEL_W, PANEL_H);
        TurretUiTheme.drawSection(guiGraphics, panelX + 10, panelY + 30, 170, 224);
        TurretUiTheme.drawSection(guiGraphics, panelX + 182, panelY + 30, 268, 110);
        TurretUiTheme.drawSection(guiGraphics, panelX + 182, panelY + 144, 268, 110);

        guiGraphics.drawCenteredString(this.font, this.title, panelX + PANEL_W / 2, panelY + 10, TurretUiTheme.COLOR_TEXT);
        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.entity_analyzer.recent"), panelX + 16, panelY + 38, TurretUiTheme.COLOR_TEXT_SUB, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.blacklist_label"), panelX + 186, panelY + 38, TurretUiTheme.COLOR_TEXT_SUB, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.whitelist_label"), panelX + 186, panelY + 150, TurretUiTheme.COLOR_TEXT_SUB, false);
        
        int totalPages = Math.max(1, (recentScans.size() + RECENT_ROWS - 1) / RECENT_ROWS);
        guiGraphics.drawCenteredString(this.font, Component.literal((recentPageIndex + 1) + " / " + totalPages), panelX + 96, panelY + 163, TurretUiTheme.COLOR_TEXT_SUB);
        
        guiGraphics.drawCenteredString(this.font, Component.literal("=== 添加选中的生物ID ==="), panelX + 96, panelY + 182, TurretUiTheme.COLOR_TEXT_SUB);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void selectRecentRow(int row) {
        if (recentScans == null || recentScans.isEmpty()) {
            return;
        }
        int idx = recentPageIndex * RECENT_ROWS + row;
        if (idx < 0 || idx >= recentScans.size()) {
            return;
        }
        recentScanIndex = idx;
        refreshRecentButtons();
    }

    private void switchRecentPage(int step) {
        int totalPages = (recentScans.size() + RECENT_ROWS - 1) / RECENT_ROWS;
        if (totalPages <= 1) {
            recentPageIndex = 0;
            return;
        }
        recentPageIndex = Math.max(0, Math.min(recentPageIndex + step, totalPages - 1));
        refreshRecentButtons();
    }

    private void addSelectedRecentToInput(MultiLineEditBox input) {
        if (input == null) {
            return;
        }
        String selected = getCurrentRecentScan();
        if (selected == null || selected.isBlank()) {
            return;
        }
        input.setValue(appendLineUnique(input.getValue(), selected));
    }

    private String appendLineUnique(String original, String entityId) {
        List<String> entries = Arrays.stream(original.split("[,\\r\\n]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
        if (!entries.contains(entityId)) {
            entries.add(entityId);
        }
        return String.join("\n", entries);
    }

    private String normalizeToMultiline(String text) {
        return Arrays.stream(text.split("[,\\r\\n]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining("\n"));
    }

    private String getCurrentRecentScan() {
        if (recentScans == null || recentScans.isEmpty() || recentScanIndex < 0 || recentScanIndex >= recentScans.size()) {
            return null;
        }
        return recentScans.get(recentScanIndex);
    }

    private void refreshRecentButtons() {
        int start = recentPageIndex * RECENT_ROWS;
        for (int i = 0; i < recentRowButtons.size(); i++) {
            Button button = recentRowButtons.get(i);
            int idx = start + i;
            if (recentScans != null && idx < recentScans.size()) {
                String id = recentScans.get(idx);
                boolean isSelected = idx == recentScanIndex;
                String prefix = isSelected ? "▶ " : "  ";
                
                if (button instanceof TechButton techBtn) {
                    techBtn.setSelected(isSelected);
                }
                
                button.visible = true;
                button.active = true;
                if (isSelected) {
                    button.setMessage(Component.literal(prefix + id).withStyle(style -> style.withColor(TurretUiTheme.COLOR_ACCENT)));
                } else {
                    button.setMessage(Component.literal(prefix + id));
                }
            } else {
                button.visible = false;
                button.active = false;
            }
        }

        boolean hasData = recentScans != null && !recentScans.isEmpty();
        if (recentPrevButton != null) recentPrevButton.active = recentPageIndex > 0;
        if (recentNextButton != null) recentNextButton.active = recentPageIndex < ((recentScans.size() + RECENT_ROWS - 1) / RECENT_ROWS) - 1;
        
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
