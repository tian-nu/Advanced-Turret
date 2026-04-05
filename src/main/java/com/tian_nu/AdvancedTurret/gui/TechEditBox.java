package com.tian_nu.AdvancedTurret.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * 科技风输入框。
 */
public class TechEditBox extends EditBox {

    private int boxX;
    private int boxY;
    private int boxWidth;
    private int boxHeight;

    public TechEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x + 4, y + (height - 8) / 2, width - 8, height, message);
        this.boxX = x;
        this.boxY = y;
        this.boxWidth = width;
        this.boxHeight = height;
        this.setBordered(false);
        this.setTextColor(TurretUiTheme.COLOR_TEXT);
        this.setTextColorUneditable(TurretUiTheme.COLOR_TEXT_SUB);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        TurretUiTheme.drawTechyInput(guiGraphics, this.boxX, this.boxY, this.boxWidth, this.boxHeight, this.isFocused(), this.isHoveredOrFocused());
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.active && this.visible && mouseX >= (double) this.boxX && mouseX < (double) (this.boxX + this.boxWidth) && mouseY >= (double) this.boxY && mouseY < (double) (this.boxY + this.boxHeight);
    }

    @Override
    public void setX(int x) {
        this.boxX = x;
        super.setX(x + 4);
    }

    @Override
    public void setY(int y) {
        this.boxY = y;
        super.setY(y + (this.boxHeight - 8) / 2);
    }
}

