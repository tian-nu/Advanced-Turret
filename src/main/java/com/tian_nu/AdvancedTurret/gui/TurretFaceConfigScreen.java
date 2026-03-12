package com.tian_nu.AdvancedTurret.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tian_nu.AdvancedTurret.ConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * 炮塔按面配置界面
 */
public class TurretFaceConfigScreen extends AbstractContainerScreen<TurretFaceConfigMenu> {

    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("advanced_turret", "textures/gui/container/turret_base.png");

    private static final int TEXTURE_WIDTH = 194;
    private static final int TEXTURE_HEIGHT = 166;
    private static final int UPGRADE_SLOT_COUNT = 18;
    private static final int PLAYER_SLOT_COUNT = 36;

    private final float backgroundAlpha = ConfigManager.getBackgroundAlpha();

    public TurretFaceConfigScreen(TurretFaceConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = TEXTURE_WIDTH;
        this.imageHeight = TEXTURE_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 2;
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
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, backgroundAlpha);
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        Rect activeRect = calcSlotRect(0, UPGRADE_SLOT_COUNT, 3, true);
        if (activeRect != null) {
            TurretUiTheme.drawSection(guiGraphics, x + activeRect.x, y + activeRect.y, activeRect.w, activeRect.h, backgroundAlpha);
        }

        for (int i = 0; i < UPGRADE_SLOT_COUNT && i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.isActive()) {
                continue;
            }
            TurretUiTheme.drawSlotFrame(guiGraphics, x + slot.x - 1, y + slot.y - 1, backgroundAlpha);
        }

        int playerStart = menu.slots.size() - PLAYER_SLOT_COUNT;
        if (playerStart >= 0) {
            Rect invRect = calcSlotRect(playerStart, 27, 3, false);
            if (invRect != null) {
                TurretUiTheme.drawSection(guiGraphics, x + invRect.x, y + invRect.y, invRect.w, invRect.h, backgroundAlpha);
            }

            Rect hotbarRect = calcSlotRect(playerStart + 27, 9, 3, false);
            if (hotbarRect != null) {
                TurretUiTheme.drawSection(guiGraphics, x + hotbarRect.x, y + hotbarRect.y, hotbarRect.w, hotbarRect.h, backgroundAlpha);
            }
        }

        drawFaceLabels(guiGraphics, x, y);
        RenderSystem.disableBlend();
    }

    private void drawFaceLabels(GuiGraphics guiGraphics, int guiX, int guiY) {
        String[] labels = new String[] {"D", "U", "N", "S", "W", "E"};
        for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
            int base = faceIndex * 3;
            if (base >= menu.slots.size()) {
                continue;
            }
            Slot topSlot = menu.slots.get(base);
            if (!topSlot.isActive()) {
                continue;
            }
            int labelX = guiX + topSlot.x + 5;
            int labelY = guiY + topSlot.y - 10;
            guiGraphics.drawString(this.font, labels[faceIndex], labelX, labelY, TurretUiTheme.COLOR_ACCENT, false);
        }
    }

    private Rect calcSlotRect(int startIndex, int count, int pad, boolean onlyActive) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int i = 0; i < count && startIndex + i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(startIndex + i);
            if (onlyActive && !slot.isActive()) {
                continue;
            }
            minX = Math.min(minX, slot.x - 1);
            minY = Math.min(minY, slot.y - 1);
            maxX = Math.max(maxX, slot.x + 16);
            maxY = Math.max(maxY, slot.y + 16);
        }

        if (minX == Integer.MAX_VALUE) {
            return null;
        }
        return new Rect(minX - pad, minY - pad, (maxX - minX + 1) + pad * 2, (maxY - minY + 1) + pad * 2);
    }

    private record Rect(int x, int y, int w, int h) {
    }
}