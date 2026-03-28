package com.tian_nu.AdvancedTurret.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class TechButton extends Button {

    protected TechButton(int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration createNarration) {
        super(x, y, width, height, message, onPress, createNarration);
    }

    public static Button.Builder builder(Component message, Button.OnPress onPress) {
        return new Button.Builder(message, onPress) {
            @Override
            public Button build() {
                Button button = super.build();
                return new TechButton(button.getX(), button.getY(), button.getWidth(), button.getHeight(), button.getMessage(), onPress, DEFAULT_NARRATION);
            }
        };
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.isHoveredOrFocused();
        TurretUiTheme.drawTechyButton(guiGraphics, this.getX(), this.getY(), this.width, this.height, hovered, this.getMessage());
    }
}
