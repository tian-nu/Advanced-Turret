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

    // 更新为科技感/玻璃态半透明色板
    public static final int COLOR_PANEL_BG = 0xD90D141C;
    public static final int COLOR_PANEL_BORDER = 0x882A898E;
    public static final int COLOR_SECTION_BG = 0x66111D29;
    public static final int COLOR_SECTION_BORDER = 0x5538BCC2;
    public static final int COLOR_SLOT_BG = 0x44050A10;
    public static final int COLOR_SLOT_BORDER = 0x661D7373;
    
    // 主题强调色
    public static final int COLOR_ACCENT = 0xFF00FFCC;
    public static final int COLOR_ACCENT_HOVER = 0xFF5CFFDD;
    public static final int COLOR_WARN = 0xFFFFC95A;
    public static final int COLOR_OK = 0xFF2BFF96;
    public static final int COLOR_DANGER = 0xFFFF4D4D;
    public static final int COLOR_TEXT = 0xFFE0F7FA;
    public static final int COLOR_TEXT_SUB = 0xFFA5D1D5;

    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        drawPanel(g, x, y, w, h, 1.0F);
    }

    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h, float alpha) {
        g.fill(x, y, x + w, y + h, withAlpha(COLOR_PANEL_BG, alpha));
        drawBorder(g, x, y, w, h, withAlpha(COLOR_PANEL_BORDER, Math.max(alpha, 0.4F)));
    }

    public static void drawSection(GuiGraphics g, int x, int y, int w, int h) {
        drawSection(g, x, y, w, h, 1.0F);
    }

    public static void drawSection(GuiGraphics g, int x, int y, int w, int h, float alpha) {
        g.fill(x, y, x + w, y + h, withAlpha(COLOR_SECTION_BG, alpha));
        drawBorder(g, x, y, w, h, withAlpha(COLOR_SECTION_BORDER, Math.max(alpha, 0.4F)));
        // 添加截断的角落高光感（科技感点缀）
        g.fill(x, y, x + 3, y + 1, COLOR_ACCENT);
        g.fill(x, y, x + 1, y + 3, COLOR_ACCENT);
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

    // 绘制科技风按钮背景
    public static void drawTechyButton(GuiGraphics g, int x, int y, int w, int h, boolean hovered, Component text) {
        int bgColor = hovered ? 0xAA113333 : 0x880A1A22;
        int borderColor = hovered ? COLOR_ACCENT_HOVER : 0xAA2A898E;
        int textColor = hovered ? 0xFFFFFFFF : COLOR_TEXT;

        g.fill(x, y, x + w, y + h, bgColor);
        drawBorder(g, x, y, w, h, borderColor);
        
        // 左上有高光标志
        g.fill(x, y, x + 4, y + 1, COLOR_ACCENT);
        
        g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, text, x + w / 2, y + (h - 8) / 2, textColor);
    }

    // 绘制科技风 Checkbox/Toggle
    public static void drawTechyCheckbox(GuiGraphics g, int x, int y, int w, int h, boolean checked, boolean hovered, Component text) {
        int boxSize = 12;
        int cy = y + (h - boxSize) / 2;
        
        int boxBgColor = checked ? 0x6600FFCC : (hovered ? 0x442A898E : 0x440D141C);
        int boxBorderColor = checked ? COLOR_ACCENT : (hovered ? 0xAA2A898E : 0x662A898E);
        
        // 画复选框方块
        g.fill(x, cy, x + boxSize, cy + boxSize, boxBgColor);
        drawBorder(g, x, cy, boxSize, boxSize, boxBorderColor);

        // 如果勾选，画内部选中标记
        if (checked) {
            g.fill(x + 3, cy + 3, x + boxSize - 3, cy + boxSize - 3, COLOR_ACCENT);
        }

        int textColor = checked ? COLOR_ACCENT : (hovered ? 0xFFFFFFFF : COLOR_TEXT_SUB);
        g.drawString(net.minecraft.client.Minecraft.getInstance().font, text, x + boxSize + 6, y + (h - 8) / 2, textColor, false);
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

