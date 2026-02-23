package com.tian_nu.AdvancedTurret.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tian_nu.AdvancedTurret.ConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 炮塔基座GUI屏幕类
 * 负责渲染炮塔基座的图形用户界面
 */
public class TurretScreen extends AbstractContainerScreen<TurretMenu> {
    
    // GUI纹理资源路径
    private static final ResourceLocation TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("turret_mod", "textures/gui/container/turret_base.png");
    
    // GUI尺寸常量
    private static final int TEXTURE_WIDTH = 194;   // 纹理宽度
    private static final int TEXTURE_HEIGHT = 166;  // 纹理高度
    
    // 透明度控制变量
    private float backgroundAlpha = ConfigManager.getBackgroundAlpha();  // 背景透明度 (0.0-1.0)
    private float energyBarAlpha = ConfigManager.getEnergyBarAlpha();   // 能量条透明度 (0.0-1.0)
    
    // 配置按钮相关
    private static final ResourceLocation PERSONAL_CONFIG_BUTTON_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("turret_mod", "textures/gui/container/personal_config_button.png");
    private static final int CONFIG_BUTTON_SIZE = 20;
    
    // 能量条相关常量
    private static final int ENERGY_BAR_X = 180;    // 能量条X坐标
    private static final int ENERGY_BAR_Y = 18;     // 能量条Y坐标
    private static final int ENERGY_BAR_WIDTH = 14; // 能量条宽度
    private static final int ENERGY_BAR_HEIGHT = 50; // 能量条高度
    
    /**
     * 构造函数
     * @param menu 炮塔菜单
     * @param playerInventory 玩家物品栏
     * @param title 屏幕标题
     */
    public TurretScreen(TurretMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = TEXTURE_WIDTH;   // 设置GUI宽度
        this.imageHeight = TEXTURE_HEIGHT; // 设置GUI高度
    }
    
    /**
     * 设置背景透明度
     * @param alpha 透明度值 (0.0 = 完全透明, 1.0 = 完全不透明)
     */
    public void setBackgroundAlpha(float alpha) {
        this.backgroundAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
        ConfigManager.setBackgroundAlpha(alpha);
        ConfigManager.saveConfig();
    }
    
    /**
     * 设置能量条透明度
     * @param alpha 透明度值 (0.0 = 完全透明, 1.0 = 完全不透明)
     */
    public void setEnergyBarAlpha(float alpha) {
        this.energyBarAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
        ConfigManager.setEnergyBarAlpha(alpha);
        ConfigManager.saveConfig();
    }
    
    /**
     * 获取当前背景透明度
     * @return 当前透明度值
     */
    public float getBackgroundAlpha() {
        return this.backgroundAlpha;
    }
    
    /**
     * 获取当前能量条透明度
     * @return 当前透明度值
     */
    public float getEnergyBarAlpha() {
        return this.energyBarAlpha;
    }
    
    /**
     * 添加配置按钮
     */
    private void addConfigButton() {
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;
        
        // 添加个人配置按钮（右上角）
        this.addRenderableWidget(new ImageButton(
            guiLeft + this.imageWidth - CONFIG_BUTTON_SIZE - 5,  // X位置
            guiTop + 5,                                           // Y位置
            CONFIG_BUTTON_SIZE,                                   // 宽度
            CONFIG_BUTTON_SIZE,                                   // 高度
            0, 0,                                                 // 纹理UV坐标
            PERSONAL_CONFIG_BUTTON_TEXTURE,                       // 纹理路径
            button -> openConfigScreen()                          // 点击回调
        ) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                // 自定义渲染逻辑
                RenderSystem.setShaderTexture(0, PERSONAL_CONFIG_BUTTON_TEXTURE);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                int v = this.isHovered ? CONFIG_BUTTON_SIZE : 0;
                guiGraphics.blit(PERSONAL_CONFIG_BUTTON_TEXTURE, this.getX(), this.getY(), 0, v, 
                               this.width, this.height, CONFIG_BUTTON_SIZE, CONFIG_BUTTON_SIZE * 2);
            }
        });
    }
    
    /**
     * 打开个人配置屏幕
     */
    private void openConfigScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new TurretPersonalConfigScreen(this, menu.getBlockEntity()));
        }
    }
    

    
    /**
     * 根据能量百分比动态调整透明度
     * 能量越低，透明度越高
     */
    public void updateAlphaBasedOnEnergy() {
        float energyPercent = (float) menu.getBlockEntity().getEnergyStored() / 
                             menu.getBlockEntity().getMaxEnergyStored();
        
        // 背景透明度：能量低时更透明
        setBackgroundAlpha(0.3F + 0.7F * energyPercent);
        
        // 能量条透明度：始终保持可见
        setEnergyBarAlpha(0.5F + 0.5F * energyPercent);
    }
    
    /**
     * 脉冲效果 - 透明度周期性变化
     * @param partialTick 部分tick值
     */
    public void applyPulseEffect(float partialTick) {
        // 使用sin函数创建平滑的脉冲效果
        float pulse = (float) (0.5F + 0.5F * Math.sin(System.currentTimeMillis() * 0.005F));
        setBackgroundAlpha(0.7F + 0.3F * pulse);
    }
    
    /**
     * 根据鼠标位置调整透明度
     * @param mouseX 鼠标X坐标
     * @param mouseY 鼠标Y坐标
     */
    public void updateAlphaBasedOnMouse(int mouseX, int mouseY) {
        // 计算鼠标到GUI中心的距离
        int guiCenterX = (this.width - this.imageWidth) / 2 + this.imageWidth / 2;
        int guiCenterY = (this.height - this.imageHeight) / 2 + this.imageHeight / 2;
        
        double distance = Math.sqrt(Math.pow(mouseX - guiCenterX, 2) + Math.pow(mouseY - guiCenterY, 2));
        double maxDistance = Math.sqrt(Math.pow(this.width / 2, 2) + Math.pow(this.height / 2, 2));
        
        // 距离越远，透明度越高
        float alpha = (float) (0.4F + 0.6F * (1.0F - distance / maxDistance));
        setBackgroundAlpha(alpha);
    }
    
    /**
     * 初始化屏幕
     * 设置标题和标签的位置
     */
    @Override
    protected void init() {
        super.init();
        // 居中标题
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        // 设置标签Y坐标
        this.titleLabelY = 6;
        // 设置物品栏标签Y坐标
        this.inventoryLabelY = this.imageHeight - 94;
        
        // 添加配置按钮
        addConfigButton();
    }
    
    /**
     * 渲染背景
     * 绘制GUI背景纹理
     * @param guiGraphics GUI图形对象
     * @param partialTick 部分tick值（用于平滑动画）
     * @param mouseX 鼠标X坐标
     * @param mouseY 鼠标Y坐标
     */
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 启用混合模式以支持透明度
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // 清除颜色缓冲区
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        // 绑定GUI纹理
        RenderSystem.setShaderTexture(0, TEXTURE);
        
        // 计算GUI居中位置
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // 绘制GUI背景（带透明度）
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, backgroundAlpha);
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        
        // 重置着色器颜色为完全不透明，准备绘制其他元素
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        // 渲染能量条
        renderEnergyBar(guiGraphics, x, y);
        
        // 禁用混合模式
        RenderSystem.disableBlend();
    }
    
    /**
     * 渲染能量条
     * 根据当前能量值绘制能量条
     * @param guiGraphics GUI图形对象
     * @param x GUI左上角X坐标
     * @param y GUI左上角Y坐标
     */
    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y) {
        // 获取方块实体的能量信息
        int energy = menu.getBlockEntity().getEnergyStored();
        int maxEnergy = menu.getBlockEntity().getMaxEnergyStored();
        
        // 计算能量条填充高度
        int fillHeight = (int) (((float) energy / maxEnergy) * ENERGY_BAR_HEIGHT);
        
        // 启用混合模式
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // 设置能量条透明度
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, energyBarAlpha);
        
        // 绘制能量条填充部分（从底部向上绘制）
        guiGraphics.blit(TEXTURE, 
            x + ENERGY_BAR_X, 
            y + ENERGY_BAR_Y + ENERGY_BAR_HEIGHT - fillHeight, 
            194,  // 纹理中的X坐标
            ENERGY_BAR_HEIGHT - fillHeight,  // 纹理中的Y坐标
            ENERGY_BAR_WIDTH, 
            fillHeight);  // 绘制的高度
        
        // 重置着色器颜色
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        // 禁用混合模式
        RenderSystem.disableBlend();
    }
    
    /**
     * 渲染前景元素
     * 包括文本、工具提示等
     * @param guiGraphics GUI图形对象
     * @param mouseX 鼠标X坐标
     * @param mouseY 鼠标Y坐标
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 根据官方文档建议，先更新动态数据
        updateDynamicData();
        
        // 应用动态透明度效果（可选）
        // updateAlphaBasedOnEnergy();  // 根据能量调整透明度
        // applyPulseEffect(partialTick);  // 脉冲效果
        // updateAlphaBasedOnMouse(mouseX, mouseY);  // 根据鼠标位置调整
        
        // 渲染背景
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 渲染工具提示
        renderTooltip(guiGraphics, mouseX, mouseY);
        
        // 渲染能量数值提示
        renderEnergyTooltip(guiGraphics, mouseX, mouseY);
    }
    
    /**
     * 根据官方文档最佳实践，更新动态渲染数据
     * 确保在渲染前获取最新数据
     */
    private void updateDynamicData() {
        // 请求方块实体更新客户端数据
        if (menu != null && menu.getBlockEntity() != null) {
            // 触发服务端数据同步到客户端
            menu.getBlockEntity().requestClientUpdate();
        }
    }
    
    /**
     * 渲染能量条工具提示
     * 当鼠标悬停在能量条上时显示详细能量信息
     * @param guiGraphics GUI图形对象
     * @param mouseX 鼠标X坐标
     * @param mouseY 鼠标Y坐标
     */
    private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 计算GUI位置
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // 检查鼠标是否在能量条区域内
        if (mouseX >= x + ENERGY_BAR_X && mouseX < x + ENERGY_BAR_X + ENERGY_BAR_WIDTH &&
            mouseY >= y + ENERGY_BAR_Y && mouseY < y + ENERGY_BAR_Y + ENERGY_BAR_HEIGHT) {
            
            // 获取能量信息
            int energy = menu.getBlockEntity().getEnergyStored();
            int maxEnergy = menu.getBlockEntity().getMaxEnergyStored();
            
            // 创建工具提示文本
            Component tooltip = Component.literal(energy + "/" + maxEnergy + " FE");
            
            // 渲染工具提示
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }
}