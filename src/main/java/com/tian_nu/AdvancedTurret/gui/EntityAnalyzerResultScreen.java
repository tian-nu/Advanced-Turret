package com.tian_nu.AdvancedTurret.gui;

import com.tian_nu.AdvancedTurret.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 生命体分析机结果界面。
 */
public class EntityAnalyzerResultScreen extends Screen {

    private final String scannedEntityId;
    private final List<String> recentScans;

    private int panelX;
    private int panelY;
    private static final int PANEL_W = 360;
    private static final int PANEL_H = 196;

    public EntityAnalyzerResultScreen(String scannedEntityId, List<String> recentScans) {
        super(Component.translatable("gui.advanced_turret.entity_analyzer.title"));
        this.scannedEntityId = scannedEntityId;
        this.recentScans = recentScans == null ? new ArrayList<>() : new ArrayList<>(recentScans);
    }

    @Override
    protected void init() {
        panelX = (this.width - PANEL_W) / 2;
        panelY = (this.height - PANEL_H) / 2;

        addRenderableWidget(TechButton.builder(Component.translatable("gui.advanced_turret.entity_analyzer.copy_again"), b -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.keyboardHandler != null) {
                minecraft.keyboardHandler.setClipboard(scannedEntityId);
            }
        }).bounds(panelX + PANEL_W - 170, panelY + PANEL_H - 24, 74, 18).build());

        addRenderableWidget(TechButton.builder(Component.translatable("gui.done"), b -> this.onClose())
            .bounds(panelX + PANEL_W - 88, panelY + PANEL_H - 24, 74, 18)
            .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        float panelAlpha = Math.max(0.15F, ConfigManager.getBackgroundAlpha());
        TurretUiTheme.drawPanel(guiGraphics, panelX, panelY, PANEL_W, PANEL_H, panelAlpha);
        TurretUiTheme.drawSection(guiGraphics, panelX + 10, panelY + 34, PANEL_W - 20, 42, panelAlpha);
        TurretUiTheme.drawSection(guiGraphics, panelX + 10, panelY + 86, PANEL_W - 20, 90, panelAlpha);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, panelY + 10, TurretUiTheme.COLOR_TEXT);
        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.entity_analyzer.current"), panelX + 16, panelY + 24, TurretUiTheme.COLOR_TEXT_SUB, false);
        guiGraphics.drawString(this.font, scannedEntityId, panelX + 16, panelY + 50, TurretUiTheme.COLOR_ACCENT, false);

        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.entity_analyzer.recent"), panelX + 16, panelY + 78, TurretUiTheme.COLOR_TEXT_SUB, false);

        int renderCount = Math.min(5, recentScans.size());
        if (renderCount == 0) {
            guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.entity_analyzer.recent.empty"), panelX + 16, panelY + 104, TurretUiTheme.COLOR_TEXT_SUB, false);
        } else {
            for (int i = 0; i < renderCount; i++) {
                String id = recentScans.get(i);
                int color = i == 0 ? TurretUiTheme.COLOR_OK : TurretUiTheme.COLOR_TEXT;
                guiGraphics.drawString(this.font, (i + 1) + ". " + id, panelX + 16, panelY + 94 + i * 14, color, false);
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
