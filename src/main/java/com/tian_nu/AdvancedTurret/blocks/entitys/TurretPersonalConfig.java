package com.tian_nu.AdvancedTurret.blocks.entitys;

import net.minecraft.nbt.CompoundTag;

/**
 * 炮塔个性化配置类
 * 存储每个炮塔的独特设置
 */
public class TurretPersonalConfig {
    
    // GUI透明度设置
    private float backgroundAlpha = 1.0F;
    private float energyBarAlpha = 1.0F;
    
    // 库存格子配置
    private int ammoSlots = 27;      // 弹药槽位数量
    private int upgradeSlots = 2;    // 升级槽位数量
    private int pluginSlots = 1;     // 插件槽位数量
    private int redstoneSlots = 1;   // 红石槽位数量
    
    // 炮塔行为配置
    private boolean autoReload = true;     // 自动装填
    private boolean friendlyFire = false;  // 友军伤害保护
    private int fireRate = 20;             // 射击间隔(tick)
    
    public TurretPersonalConfig() {
        // 默认配置
    }
    
    public TurretPersonalConfig(float bgAlpha, float energyAlpha, int ammo, int upgrade, int plugin, int redstone) {
        this.backgroundAlpha = bgAlpha;
        this.energyBarAlpha = energyAlpha;
        this.ammoSlots = ammo;
        this.upgradeSlots = upgrade;
        this.pluginSlots = plugin;
        this.redstoneSlots = redstone;
    }
    
    /**
     * 复制构造函数
     */
    public TurretPersonalConfig(TurretPersonalConfig other) {
        this.backgroundAlpha = other.backgroundAlpha;
        this.energyBarAlpha = other.energyBarAlpha;
        this.ammoSlots = other.ammoSlots;
        this.upgradeSlots = other.upgradeSlots;
        this.pluginSlots = other.pluginSlots;
        this.redstoneSlots = other.redstoneSlots;
        this.autoReload = other.autoReload;
        this.friendlyFire = other.friendlyFire;
        this.fireRate = other.fireRate;
    }
    
    // ==================== NBT序列化 ====================
    
    public void serializeNBT(CompoundTag tag) {
        CompoundTag configTag = new CompoundTag();
        
        // 透明度设置
        configTag.putFloat("BackgroundAlpha", backgroundAlpha);
        configTag.putFloat("EnergyBarAlpha", energyBarAlpha);
        
        // 库存配置
        configTag.putInt("AmmoSlots", ammoSlots);
        configTag.putInt("UpgradeSlots", upgradeSlots);
        configTag.putInt("PluginSlots", pluginSlots);
        configTag.putInt("RedstoneSlots", redstoneSlots);
        
        // 行为配置
        configTag.putBoolean("AutoReload", autoReload);
        configTag.putBoolean("FriendlyFire", friendlyFire);
        configTag.putInt("FireRate", fireRate);
        
        tag.put("PersonalConfig", configTag);
    }
    
    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("PersonalConfig")) {
            CompoundTag configTag = tag.getCompound("PersonalConfig");
            
            // 透明度设置
            backgroundAlpha = configTag.getFloat("BackgroundAlpha");
            energyBarAlpha = configTag.getFloat("EnergyBarAlpha");
            
            // 库存配置
            ammoSlots = configTag.getInt("AmmoSlots");
            upgradeSlots = configTag.getInt("UpgradeSlots");
            pluginSlots = configTag.getInt("PluginSlots");
            redstoneSlots = configTag.getInt("RedstoneSlots");
            
            // 行为配置
            autoReload = configTag.getBoolean("AutoReload");
            friendlyFire = configTag.getBoolean("FriendlyFire");
            fireRate = configTag.getInt("FireRate");
        }
    }
    
    // ==================== Getters and Setters ====================
    
    public float getBackgroundAlpha() {
        return backgroundAlpha;
    }
    
    public void setBackgroundAlpha(float backgroundAlpha) {
        this.backgroundAlpha = Math.max(0.0F, Math.min(1.0F, backgroundAlpha));
    }
    
    public float getEnergyBarAlpha() {
        return energyBarAlpha;
    }
    
    public void setEnergyBarAlpha(float energyBarAlpha) {
        this.energyBarAlpha = Math.max(0.0F, Math.min(1.0F, energyBarAlpha));
    }
    
    public int getAmmoSlots() {
        return ammoSlots;
    }
    
    public void setAmmoSlots(int ammoSlots) {
        this.ammoSlots = Math.max(0, Math.min(54, ammoSlots)); // 限制在0-54之间
    }
    
    public int getUpgradeSlots() {
        return upgradeSlots;
    }
    
    public void setUpgradeSlots(int upgradeSlots) {
        this.upgradeSlots = Math.max(0, Math.min(9, upgradeSlots));
    }
    
    public int getPluginSlots() {
        return pluginSlots;
    }
    
    public void setPluginSlots(int pluginSlots) {
        this.pluginSlots = Math.max(0, Math.min(9, pluginSlots));
    }
    
    public int getRedstoneSlots() {
        return redstoneSlots;
    }
    
    public void setRedstoneSlots(int redstoneSlots) {
        this.redstoneSlots = Math.max(0, Math.min(9, redstoneSlots));
    }
    
    public boolean isAutoReload() {
        return autoReload;
    }
    
    public void setAutoReload(boolean autoReload) {
        this.autoReload = autoReload;
    }
    
    public boolean isFriendlyFire() {
        return friendlyFire;
    }
    
    public void setFriendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }
    
    public int getFireRate() {
        return fireRate;
    }
    
    public void setFireRate(int fireRate) {
        this.fireRate = Math.max(1, Math.min(100, fireRate));
    }
    
    // ==================== 实用方法 ====================
    
    /**
     * 获取总槽位数量
     */
    public int getTotalSlots() {
        return ammoSlots + upgradeSlots + pluginSlots + redstoneSlots;
    }
    
    /**
     * 验证配置是否有效
     */
    public boolean isValid() {
        return ammoSlots >= 0 && ammoSlots <= 54 &&
               upgradeSlots >= 0 && upgradeSlots <= 9 &&
               pluginSlots >= 0 && pluginSlots <= 9 &&
               redstoneSlots >= 0 && redstoneSlots <= 9 &&
               getTotalSlots() <= 54; // 总槽数不能超过54
    }
    
    /**
     * 重置为默认配置
     */
    public void resetToDefault() {
        backgroundAlpha = 1.0F;
        energyBarAlpha = 1.0F;
        ammoSlots = 27;
        upgradeSlots = 2;
        pluginSlots = 1;
        redstoneSlots = 1;
        autoReload = true;
        friendlyFire = false;
        fireRate = 20;
    }
}