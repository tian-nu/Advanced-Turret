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
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 炮塔基座GUI屏幕类
 * 负责渲染炮塔基座的图形用户界面
 */
public class TurretScreen extends AbstractContainerScreen<TurretMenu> {
    
    // GUI纹理资源路径
    private static final ResourceLocation TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("advanced_turret", "textures/gui/container/turret_base.png");
    
    // GUI尺寸常量
    private static final int TEXTURE_WIDTH = 194;   // 纹理宽度
    private static final int TEXTURE_HEIGHT = 166;  // 纹理高度
    
    // 透明度控制变量
    private float backgroundAlpha = ConfigManager.getBackgroundAlpha();
    private float energyBarAlpha = ConfigManager.getEnergyBarAlpha();
    
    // 配置按钮相关
    private static final ResourceLocation PERSONAL_CONFIG_BUTTON_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("advanced_turret", "textures/gui/container/personal_config_button.png");
    private static final int CONFIG_BUTTON_SIZE = 20;
    
    // 能量条相关常量 (移到右侧)
    private static final int ENERGY_BAR_X = 170;
    private static final int ENERGY_BAR_Y = 18;
    private static final int ENERGY_BAR_WIDTH = 14;
    private static final int ENERGY_BAR_HEIGHT = 50;
    
    // 智能插件按钮
    private Button smartConfigButton;
    private Button faceConfigButton;
    private boolean hasSmartChip = false;
    
    public TurretScreen(TurretMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = TEXTURE_WIDTH;
        this.imageHeight = TEXTURE_HEIGHT;
    }
    
    @Override
    protected void init() {
        super.init();
        // 居中标题
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 94;
        
        // 添加原有配置按钮
        addConfigButton();
        
        // 添加智能配置按钮
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;
        
        this.smartConfigButton = Button.builder(Component.translatable("gui.advanced_turret.smart_config"), b -> {
            ItemStack stack = getPluginStack();
            if (!stack.isEmpty()) {
                Minecraft.getInstance().setScreen(new SmartChipConfigScreen(stack, menu.getBlockEntity().getBlockPos()));
            }
        }).bounds(guiLeft + this.imageWidth + 5, guiTop + 20, 80, 20).build();
        this.smartConfigButton.visible = false;
        addRenderableWidget(this.smartConfigButton);

        this.faceConfigButton = Button.builder(Component.literal("面配置"), b -> {
            ModNetwork.CHANNEL.sendToServer(new TurretOpenFaceConfigPacket(menu.getBlockEntity().getBlockPos()));
        }).bounds(guiLeft + this.imageWidth + 5, guiTop + 45, 80, 20).build();
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
            0, 0, // 移除悬停偏移，如果用户不需要
            PERSONAL_CONFIG_BUTTON_TEXTURE,
            button -> openConfigScreen()
        ) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                RenderSystem.setShaderTexture(0, PERSONAL_CONFIG_BUTTON_TEXTURE);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                // 静态渲染，不根据 hover 改变 v 坐标
                guiGraphics.blit(PERSONAL_CONFIG_BUTTON_TEXTURE, this.getX(), this.getY(), 0, 0, 
                               this.width, this.height, CONFIG_BUTTON_SIZE, CONFIG_BUTTON_SIZE);
                
                // 发光效果
                if (this.isHovered) {
                    renderGlowingBorder(guiGraphics, this.getX(), this.getY(), this.width, this.height, 0xFFFFFF00);
                }
            }
        });
    }
    
    private ItemStack getPluginStack() {
        return menu.getBlockEntity().getPluginStack();
    }
    
    private void updateControlsVisibility() {
        // 检查插件槽位
        ItemStack stack = getPluginStack();
        hasSmartChip = !stack.isEmpty();
        
        if (smartConfigButton != null) {
            smartConfigButton.visible = hasSmartChip;
        }
    }
    
    private void openConfigScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new TurretPersonalConfigScreen(this, menu.getBlockEntity()));
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 更新控件可见性
        updateControlsVisibility();
        
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        
        // 渲染能量条 Tooltip
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;
        int barX = guiLeft + ENERGY_BAR_X;
        int barY = guiTop + ENERGY_BAR_Y;
        
        if (mouseX >= barX && mouseX < barX + ENERGY_BAR_WIDTH && 
            mouseY >= barY && mouseY < barY + ENERGY_BAR_HEIGHT) {
            List<Component> tooltip = new ArrayList<>();
            int energy = menu.getEnergy();
            int maxEnergy = menu.getMaxEnergy();
            tooltip.add(Component.literal(energy + " / " + maxEnergy + " FE"));
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

        int ammoBorder = 0x80909090;
        int ammoFill = 0x30000000;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = x + 8 + col * 18 - 1;
                int sy = y + 18 + row * 18 - 1;
                guiGraphics.fill(sx, sy, sx + 18, sy + 18, ammoFill);
                guiGraphics.fill(sx, sy, sx + 18, sy + 1, ammoBorder);
                guiGraphics.fill(sx, sy + 17, sx + 18, sy + 18, ammoBorder);
                guiGraphics.fill(sx, sy, sx + 1, sy + 18, ammoBorder);
                guiGraphics.fill(sx + 17, sy, sx + 18, sy + 18, ammoBorder);
            }
        }

        // 渲染插件槽边框
        // 使用实际槽数量兼容旧存档
        int pluginSlotCount = Math.min(
            menu.getBlockEntity().getPluginSlotCount(), 
            menu.getBlockEntity().getBasePluginSlot().getSlots()
        );
        if (pluginSlotCount > 0) {
            int pluginBorder = 0x80FFFF00;
            for (int i = 0; i < pluginSlotCount; i++) {
                // 与TurretMenu中的布局保持一致
                int slotX = x + 79 + (i % 2) * 18;  // 第一列79，第二列97
                int slotY = y + 59 + (i / 2) * 18;  // 第一行59，第二行77
                guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, ammoFill);
                guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 1, pluginBorder);
                guiGraphics.fill(slotX, slotY + 17, slotX + 18, slotY + 18, pluginBorder);
                guiGraphics.fill(slotX, slotY, slotX + 1, slotY + 18, pluginBorder);
                guiGraphics.fill(slotX + 17, slotY, slotX + 18, slotY + 18, pluginBorder);
            }
        }
        
        renderEnergyBar(guiGraphics, x, y);
        RenderSystem.disableBlend();
    }
    
    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y) {
        int energy = menu.getEnergy();
        int maxEnergy = menu.getMaxEnergy();
        int fillHeight = maxEnergy > 0 ? (int) (((float) energy / maxEnergy) * ENERGY_BAR_HEIGHT) : 0;
        
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, energyBarAlpha);
        // 纹理中能量条位置假设在 (194, 0)
        // 目标位置: x + ENERGY_BAR_X, y + ENERGY_BAR_Y + (HEIGHT - fill)
        guiGraphics.blit(TEXTURE, x + ENERGY_BAR_X, y + ENERGY_BAR_Y + ENERGY_BAR_HEIGHT - fillHeight, 
                       194, ENERGY_BAR_HEIGHT - fillHeight, ENERGY_BAR_WIDTH, fillHeight);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
    
    // 发光边框绘制辅助方法
    private void renderGlowingBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        // 绘制外边框
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y, color); // Top
        guiGraphics.fill(x - 1, y + height, x + width + 1, y + height + 1, color); // Bottom
        guiGraphics.fill(x - 1, y, x, y + height, color); // Left
        guiGraphics.fill(x + width, y, x + width + 1, y + height, color); // Right
    }

    /**
     * 设置背景透明度
     */
    public void setBackgroundAlpha(float alpha) {
        this.backgroundAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
        ConfigManager.setBackgroundAlpha(alpha);
        ConfigManager.saveConfig();
    }
    
    /**
     * 设置能量条透明度
     */
    public void setEnergyBarAlpha(float alpha) {
        this.energyBarAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
        ConfigManager.setEnergyBarAlpha(alpha);
        ConfigManager.saveConfig();
    }
    
    /**
     * 获取当前背景透明度
     */
    public float getBackgroundAlpha() {
        return this.backgroundAlpha;
    }
    
    /**
     * 获取当前能量条透明度
     */
    public float getEnergyBarAlpha() {
        return this.energyBarAlpha;
    }
}
