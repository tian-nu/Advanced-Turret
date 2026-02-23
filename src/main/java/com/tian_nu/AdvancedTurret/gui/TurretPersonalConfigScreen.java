package com.tian_nu.AdvancedTurret.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tian_nu.AdvancedTurret.blocks.entitys.TurretBaseBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.TurretPersonalConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 炮塔个性化配置屏幕
 * 用于为每个炮塔设置独特的配置
 */
public class TurretPersonalConfigScreen extends Screen {
    
    private final TurretScreen parentScreen;
    private final TurretBaseBlockEntity blockEntity;
    private final TurretPersonalConfig config;
    
    private static final ResourceLocation CONFIG_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("turret_mod", "textures/gui/container/personal_config.png");
    
    // GUI尺寸
    private static final int GUI_WIDTH = 250;
    private static final int GUI_HEIGHT = 200;
    
    // 输入框位置
    private EditBox backgroundAlphaField;
    private EditBox energyAlphaField;
    
    // 复选框状态
    private boolean autoReload = true;
    
    public TurretPersonalConfigScreen(TurretScreen parentScreen, TurretBaseBlockEntity blockEntity) {
        super(Component.translatable("gui.turret_personal_config.title"));
        this.parentScreen = parentScreen;
        this.blockEntity = blockEntity;
        // 从父屏幕获取当前透明度值
        this.config = new TurretPersonalConfig(
            parentScreen.getBackgroundAlpha(), 
            parentScreen.getEnergyBarAlpha(), 
            27, 2, 1, 1
        );
    }
    
    @Override
    protected void init() {
        super.init();
        
        int guiX = (this.width - GUI_WIDTH) / 2;
        int guiY = (this.height - GUI_HEIGHT) / 2;
        
        // 创建输入框
        createInputFields(guiX, guiY);
        
        // 创建复选框按钮
        createCheckboxButtons(guiX, guiY);
        
        // 创建操作按钮
        createActionButtons(guiX, guiY);
        
        // 填充初始值
        populateInitialValues();
    }
    
    private void createInputFields(int guiX, int guiY) {
        // 背景透明度输入框
        backgroundAlphaField = new EditBox(this.font, guiX + 100, guiY + 30, 40, 16, Component.empty());
        backgroundAlphaField.setMaxLength(4);
        backgroundAlphaField.setValue(String.format("%.2f", config.getBackgroundAlpha()));
        this.addRenderableWidget(backgroundAlphaField);
        
        // 能量条透明度输入框
        energyAlphaField = new EditBox(this.font, guiX + 100, guiY + 50, 40, 16, Component.empty());
        energyAlphaField.setMaxLength(4);
        energyAlphaField.setValue(String.format("%.2f", config.getEnergyBarAlpha()));
        this.addRenderableWidget(energyAlphaField);
    }
    
    private void createCheckboxButtons(int guiX, int guiY) {
        // 自动装填复选框
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.turret_config.auto_reload").append(": ").append(
                autoReload ? Component.translatable("gui.turret_config.enabled") : Component.translatable("gui.turret_config.disabled")),
            button -> toggleAutoReload()
        ).bounds(guiX + 150, guiY + 30, 90, 20).build());
    }
    
    private void createActionButtons(int guiX, int guiY) {
        // 确认按钮
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.done"),
            button -> applyChanges()
        ).bounds(guiX + 30, guiY + GUI_HEIGHT - 30, 60, 20).build());
        
        // 重置按钮
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.reset"),
            button -> resetToDefault()
        ).bounds(guiX + 100, guiY + GUI_HEIGHT - 30, 60, 20).build());
        
        // 取消按钮
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.cancel"),
            button -> onClose()
        ).bounds(guiX + 170, guiY + GUI_HEIGHT - 30, 60, 20).build());
    }
    
    private void populateInitialValues() {
        autoReload = config.isAutoReload();
    }
    
    private void toggleAutoReload() {
        autoReload = !autoReload;
        // 更新按钮文本
        this.children().stream()
            .filter(widget -> widget instanceof Button)
            .map(widget -> (Button) widget)
            .filter(button -> button.getMessage().getString().contains("auto_reload"))
            .findFirst()
            .ifPresent(button -> button.setMessage(
                Component.translatable("gui.turret_config.auto_reload").append(": ").append(
                    autoReload ? Component.translatable("gui.turret_config.enabled") : Component.translatable("gui.turret_config.disabled"))
            ));
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染背景暗化效果
        this.renderBackground(guiGraphics);
        
        // 计算GUI位置
        int guiX = (this.width - GUI_WIDTH) / 2;
        int guiY = (this.height - GUI_HEIGHT) / 2;
        
        // 渲染配置GUI背景
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, CONFIG_TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(CONFIG_TEXTURE, guiX, guiY, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        
        // 渲染标题
        guiGraphics.drawCenteredString(
            this.font, 
            this.title, 
            this.width / 2, 
            guiY + 10, 
            0xFFFFFF
        );
        
        // 渲染标签
        renderLabels(guiGraphics, guiX, guiY);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    private void renderLabels(GuiGraphics guiGraphics, int guiX, int guiY) {
        guiGraphics.drawString(this.font, Component.translatable("gui.turret_config.background_alpha"), guiX + 10, guiY + 34, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("gui.turret_config.energy_alpha"), guiX + 10, guiY + 54, 0xFFFFFF);
    }
    
    private void applyChanges() {
        try {
            // 验证并应用配置
            float bgAlpha = Math.max(0.0F, Math.min(1.0F, Float.parseFloat(backgroundAlphaField.getValue())));
            float energyAlpha = Math.max(0.0F, Math.min(1.0F, Float.parseFloat(energyAlphaField.getValue())));
            
            // 更新配置
            config.setBackgroundAlpha(bgAlpha);
            config.setEnergyBarAlpha(energyAlpha);
            config.setAutoReload(autoReload);
            
            // 应用到方块实体
            // 暂时禁用个性化配置功能
            // blockEntity.setPersonalConfig(config);
            
            // 更新父屏幕的透明度设置
            parentScreen.setBackgroundAlpha(bgAlpha);
            parentScreen.setEnergyBarAlpha(energyAlpha);
            
            onClose();
            
        } catch (NumberFormatException e) {
            // 输入格式错误，不执行任何操作
        }
    }
    
    private void resetToDefault() {
        config.resetToDefault();
        populateInitialValues();
        backgroundAlphaField.setValue("1.00");
        energyAlphaField.setValue("1.00");
        autoReload = true;
    }
    
    @Override
    public void onClose() {
        this.minecraft.setScreen(parentScreen);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false; // 不暂停游戏
    }
}