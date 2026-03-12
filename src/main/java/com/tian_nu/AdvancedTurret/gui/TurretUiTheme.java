package com.tian_nu.AdvancedTurret.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 炮塔 GUI 统一视觉工具
 */
public final class TurretUiTheme {

    private TurretUiTheme() {
    }

    public static final int COLOR_PANEL_BG = 0xB0141822;
    public static final int COLOR_PANEL_BORDER = 0xFF5E6D87;
    public static final int COLOR_SECTION_BG = 0x8020293A;
    public static final int COLOR_SECTION_BORDER = 0xFF7D8CAA;
    public static final int COLOR_SLOT_BG = 0x50303030;
    public static final int COLOR_SLOT_BORDER = 0xB0AAB4C8;
    public static final int COLOR_ACCENT = 0xFF65D6FF;
    public static final int COLOR_WARN = 0xFFFFC95A;
    public static final int COLOR_OK = 0xFF6BE08D;
    public static final int COLOR_DANGER = 0xFFFF7A7A;
    public static final int COLOR_TEXT = 0xFFE7ECF8;
    public static final int COLOR_TEXT_SUB = 0xFFB7C0D4;

    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        drawPanel(g, x, y, w, h, 1.0F);
    }

    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h, float alpha) {
        g.fill(x, y, x + w, y + h, withAlpha(COLOR_PANEL_BG, alpha));
        drawBorder(g, x, y, w, h, withAlpha(COLOR_PANEL_BORDER, Math.max(alpha, 0.35F)));
    }

    public static void drawSection(GuiGraphics g, int x, int y, int w, int h) {
        drawSection(g, x, y, w, h, 1.0F);
    }

    public static void drawSection(GuiGraphics g, int x, int y, int w, int h, float alpha) {
        g.fill(x, y, x + w, y + h, withAlpha(COLOR_SECTION_BG, alpha));
        drawBorder(g, x, y, w, h, withAlpha(COLOR_SECTION_BORDER, Math.max(alpha, 0.35F)));
    }

    public static void drawSlotFrame(GuiGraphics g, int x, int y) {
        drawSlotFrame(g, x, y, 1.0F);
    }

    public static void drawSlotFrame(GuiGraphics g, int x, int y, float alpha) {
        g.fill(x, y, x + 18, y + 18, withAlpha(COLOR_SLOT_BG, alpha));
        drawBorder(g, x, y, 18, 18, withAlpha(COLOR_SLOT_BORDER, Math.max(alpha, 0.35F)));
    }

    public static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static int withAlpha(int color, float alphaMultiplier) {
        float clamped = Math.max(0.0F, Math.min(1.0F, alphaMultiplier));
        int alpha = (color >>> 24) & 0xFF;
        int adjusted = Math.max(0, Math.min(255, Math.round(alpha * clamped)));
        return (color & 0x00FFFFFF) | (adjusted << 24);
    }

    public static MutableComponent tipTitle(String text) {
        return Component.literal(text).withStyle(style -> style.withColor(COLOR_TEXT));
    }

    public static MutableComponent tipInfo(String text) {
        return Component.literal(text).withStyle(style -> style.withColor(COLOR_TEXT_SUB));
    }

    public static MutableComponent tipWarn(String text) {
        return Component.literal(text).withStyle(style -> style.withColor(COLOR_WARN));
    }

    public static MutableComponent tipOk(String text) {
        return Component.literal(text).withStyle(style -> style.withColor(COLOR_OK));
    }

    public static MutableComponent tipDanger(String text) {
        return Component.literal(text).withStyle(style -> style.withColor(COLOR_DANGER));
    }
}
