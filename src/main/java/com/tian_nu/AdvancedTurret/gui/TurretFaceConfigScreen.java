package com.tian_nu.AdvancedTurret.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class TurretFaceConfigScreen extends AbstractContainerScreen<TurretFaceConfigMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("advanced_turret", "textures/gui/container/turret_base.png");

    private static final int TEXTURE_WIDTH = 194;
    private static final int TEXTURE_HEIGHT = 166;

    public TurretFaceConfigScreen(TurretFaceConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = TEXTURE_WIDTH;
        this.imageHeight = TEXTURE_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        int border = 0x80FF0000;
        int fill = 0x30000000;
        for (int i = 0; i < 18 && i < menu.slots.size(); i++) {
            if (!menu.slots.get(i).isActive()) continue;
            int sx = x + menu.slots.get(i).x - 1;
            int sy = y + menu.slots.get(i).y - 1;
            guiGraphics.fill(sx, sy, sx + 18, sy + 18, fill);
            guiGraphics.fill(sx, sy, sx + 18, sy + 1, border);
            guiGraphics.fill(sx, sy + 17, sx + 18, sy + 18, border);
            guiGraphics.fill(sx, sy, sx + 1, sy + 18, border);
            guiGraphics.fill(sx + 17, sy, sx + 18, sy + 18, border);
        }

        int labelY = y + 10;
        String[] labels = new String[] {"D", "U", "N", "S", "W", "E"};
        for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
            boolean visible = false;
            int base = faceIndex * 3;
            for (int row = 0; row < 3; row++) {
                int slotIndex = base + row;
                if (slotIndex >= menu.slots.size()) continue;
                if (menu.slots.get(slotIndex).isActive()) {
                    visible = true;
                    break;
                }
            }
            if (visible) {
                guiGraphics.drawString(this.font, labels[faceIndex], x + 8 + faceIndex * 18 + 6, labelY, 0xFFAA0000, false);
            }
        }
    }
}
