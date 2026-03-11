package com.tian_nu.AdvancedTurret.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tian_nu.AdvancedTurret.ConfigManager;
import com.tian_nu.AdvancedTurret.network.ModNetwork;
import com.tian_nu.AdvancedTurret.network.TurretOpenFaceConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 炮塔基座主界面
 */
public class TurretScreen extends AbstractContainerScreen<TurretMenu> {

    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("advanced_turret", "textures/gui/container/turret_base.png");

    private static final int TEXTURE_WIDTH = 194;
    private static final int TEXTURE_HEIGHT = 166;

    private static final int AMMO_SLOT_COUNT = 9;
    private static final int PLAYER_SLOT_COUNT = 36;

    private float backgroundAlpha = ConfigManager.getBackgroundAlpha();
    private float energyBarAlpha = ConfigManager.getEnergyBarAlpha();

    private static final ResourceLocation PERSONAL_CONFIG_BUTTON_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("advanced_turret", "textures/gui/container/personal_config_button.png");
    private static final int CONFIG_BUTTON_SIZE = 20;
    private static final int CONFIG_BUTTON_TEXTURE_SIZE = 474;

    private static final int ENERGY_BAR_X = 169;
    private static final int ENERGY_BAR_Y = 19;
    private static final int ENERGY_BAR_WIDTH = 14;
    private static final int ENERGY_BAR_HEIGHT = 50;

    private Button smartConfigButton;
    private Button faceConfigButton;

    public TurretScreen(TurretMenu menu, Inventory playerInventory, Component title) {
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

        addConfigButton();

        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        this.smartConfigButton = Button.builder(Component.translatable("gui.advanced_turret.smart_config"), b -> {
            ItemStack stack = getPluginStack();
            if (!stack.isEmpty()) {
                Minecraft.getInstance().setScreen(new SmartChipConfigScreen(stack, menu.getBlockEntity().getBlockPos()));
            }
        }).bounds(guiLeft + this.imageWidth + 8, guiTop + 22, 84, 20).build();
        addRenderableWidget(this.smartConfigButton);

        this.faceConfigButton = Button.builder(Component.translatable("gui.advanced_turret.face_config"), b ->
            ModNetwork.CHANNEL.sendToServer(new TurretOpenFaceConfigPacket(menu.getBlockEntity().getBlockPos()))
        ).bounds(guiLeft + this.imageWidth + 8, guiTop + 47, 84, 20).build();
        addRenderableWidget(this.faceConfigButton);
    }

    private void addConfigButton() {
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        this.addRenderableWidget(new ImageButton(
            guiLeft + this.imageWidth - CONFIG_BUTTON_SIZE - 5,
            guiTop + 5,
            CONFIG_BUTTON_SIZE,
            CONFIG_BUTTON_SIZE,
            0,
            0,
            PERSONAL_CONFIG_BUTTON_TEXTURE,
            button -> openConfigScreen()
        ) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                RenderSystem.setShaderTexture(0, PERSONAL_CONFIG_BUTTON_TEXTURE);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                guiGraphics.blit(PERSONAL_CONFIG_BUTTON_TEXTURE, this.getX(), this.getY(), 0, 0,
                    this.width, this.height, CONFIG_BUTTON_TEXTURE_SIZE, CONFIG_BUTTON_TEXTURE_SIZE);

                if (this.isHovered) {
                    TurretUiTheme.drawBorder(guiGraphics, this.getX() - 1, this.getY() - 1, this.width + 2, this.height + 2, TurretUiTheme.COLOR_WARN);
                }
            }
        });
    }

    private ItemStack getPluginStack() {
        return menu.getBlockEntity().getPluginStack();
    }

    private void updateControlsVisibility() {
        ItemStack stack = getPluginStack();
        if (smartConfigButton != null) {
            smartConfigButton.visible = !stack.isEmpty();
        }
    }

    private void openConfigScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new TurretPersonalConfigScreen(this, menu.getBlockEntity()));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateControlsVisibility();

        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;
        int barX = guiLeft + ENERGY_BAR_X;
        int barY = guiTop + ENERGY_BAR_Y;

        if (mouseX >= barX && mouseX < barX + ENERGY_BAR_WIDTH &&
            mouseY >= barY && mouseY < barY + ENERGY_BAR_HEIGHT) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(TurretUiTheme.tipTitle(Component.translatable("gui.advanced_turret.energy").getString()));
            tooltip.add(TurretUiTheme.tipInfo(menu.getEnergy() + " / " + menu.getMaxEnergy() + " FE"));
            guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }
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

        Rect ammoRect = calcSlotRect(0, AMMO_SLOT_COUNT, 3);
        if (ammoRect != null) {
            TurretUiTheme.drawSection(guiGraphics, x + ammoRect.x, y + ammoRect.y, ammoRect.w, ammoRect.h);
            guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.ammo"), x + ammoRect.x, y + ammoRect.y - 9, TurretUiTheme.COLOR_TEXT_SUB, false);
            drawSlotFrames(guiGraphics, x, y, 0, AMMO_SLOT_COUNT);
        }

        int pluginSlotCount = Math.min(menu.getBlockEntity().getPluginSlotCount(), menu.getBlockEntity().getBasePluginSlot().getSlots());
        if (pluginSlotCount > 0) {
            Rect pluginRect = calcSlotRect(AMMO_SLOT_COUNT, pluginSlotCount, 2);
            if (pluginRect != null) {
                TurretUiTheme.drawSection(guiGraphics, x + pluginRect.x, y + pluginRect.y, pluginRect.w, pluginRect.h);
                guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.plugin"), x + pluginRect.x, y + pluginRect.y - 9, TurretUiTheme.COLOR_TEXT_SUB, false);
                drawSlotFrames(guiGraphics, x, y, AMMO_SLOT_COUNT, pluginSlotCount);
            }
        }

        // 玩家背包和快捷栏区域按真实 slot 坐标计算，避免手调像素误差。
        int playerStart = menu.slots.size() - PLAYER_SLOT_COUNT;
        if (playerStart >= 0) {
            Rect invRect = calcSlotRect(playerStart, 27, 3);
            if (invRect != null) {
                TurretUiTheme.drawSection(guiGraphics, x + invRect.x, y + invRect.y, invRect.w, invRect.h);
            }

            Rect hotbarRect = calcSlotRect(playerStart + 27, 9, 3);
            if (hotbarRect != null) {
                TurretUiTheme.drawSection(guiGraphics, x + hotbarRect.x, y + hotbarRect.y, hotbarRect.w, hotbarRect.h);
            }
        }

        TurretUiTheme.drawSection(guiGraphics, x + ENERGY_BAR_X - 2, y + ENERGY_BAR_Y - 2, ENERGY_BAR_WIDTH + 4, ENERGY_BAR_HEIGHT + 4);
        renderEnergyBar(guiGraphics, x, y);
        RenderSystem.disableBlend();
    }

    private void drawSlotFrames(GuiGraphics guiGraphics, int guiX, int guiY, int startIndex, int count) {
        for (int i = 0; i < count; i++) {
            int idx = startIndex + i;
            if (idx < 0 || idx >= menu.slots.size()) {
                continue;
            }
            Slot slot = menu.slots.get(idx);
            TurretUiTheme.drawSlotFrame(guiGraphics, guiX + slot.x - 1, guiY + slot.y - 1);
        }
    }

    private Rect calcSlotRect(int startIndex, int count, int pad) {
        if (count <= 0) {
            return null;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int i = 0; i < count; i++) {
            int idx = startIndex + i;
            if (idx < 0 || idx >= menu.slots.size()) {
                continue;
            }
            Slot slot = menu.slots.get(idx);
            minX = Math.min(minX, slot.x - 1);
            minY = Math.min(minY, slot.y - 1);
            // 视觉边界取 slot 外框，避免比 18x18 再多 1px。
            maxX = Math.max(maxX, slot.x + 16);
            maxY = Math.max(maxY, slot.y + 16);
        }

        if (minX == Integer.MAX_VALUE) {
            return null;
        }

        return new Rect(minX - pad, minY - pad, (maxX - minX + 1) + pad * 2, (maxY - minY + 1) + pad * 2);
    }

    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y) {
        int maxEnergy = menu.getMaxEnergy();
        int fillHeight = maxEnergy > 0 ? (int) (((float) menu.getEnergy() / maxEnergy) * ENERGY_BAR_HEIGHT) : 0;
        int barLeft = x + ENERGY_BAR_X;
        int barTop = y + ENERGY_BAR_Y;
        int barBottom = barTop + ENERGY_BAR_HEIGHT;

        guiGraphics.fill(barLeft, barTop, barLeft + ENERGY_BAR_WIDTH, barBottom, 0xAA2A1010);

        if (fillHeight > 0) {
            int alpha = Math.max(0, Math.min(255, (int) (energyBarAlpha * 255.0F)));
            int fillColor = (alpha << 24) | 0x00D63838;
            int glowColor = (alpha << 24) | 0x00FF6B6B;
            guiGraphics.fill(barLeft, barBottom - fillHeight, barLeft + ENERGY_BAR_WIDTH, barBottom, fillColor);
            guiGraphics.fill(barLeft, barBottom - fillHeight, barLeft + ENERGY_BAR_WIDTH, barBottom - fillHeight + 1, glowColor);
        }

        TurretUiTheme.drawBorder(guiGraphics, barLeft, barTop, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, 0xFF8A2B2B);
    }

    public void setBackgroundAlpha(float alpha) {
        this.backgroundAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
        ConfigManager.setBackgroundAlpha(alpha);
        ConfigManager.saveConfig();
    }

    public void setEnergyBarAlpha(float alpha) {
        this.energyBarAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
        ConfigManager.setEnergyBarAlpha(alpha);
        ConfigManager.saveConfig();
    }

    public float getBackgroundAlpha() {
        return this.backgroundAlpha;
    }

    public float getEnergyBarAlpha() {
        return this.energyBarAlpha;
    }

    private record Rect(int x, int y, int w, int h) {
    }
}