package com.tian_nu.AdvancedTurret.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;

public class TechCheckbox extends Checkbox {

    public TechCheckbox(int x, int y, int width, int height, Component message, boolean selected) {
        super(x, y, width, height, message, selected);
    }

    public void setChecked(boolean checked) {
        if (this.selected() != checked) {
            this.onPress();
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.isHoveredOrFocused();
        TurretUiTheme.drawTechyCheckbox(guiGraphics, this.getX(), this.getY(), this.width, this.height, this.selected(), hovered, this.getMessage());
    }
}
