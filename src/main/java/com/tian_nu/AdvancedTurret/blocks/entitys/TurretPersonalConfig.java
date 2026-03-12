package com.tian_nu.AdvancedTurret.blocks.entitys;

import net.minecraft.nbt.CompoundTag;

/**
 * 炮塔个性化配置类
 * 存储每个炮塔的独特设置
 */
public class TurretPersonalConfig {

    private float backgroundAlpha = 0.0F;
    private float energyBarAlpha = 1.0F;

    private int ammoSlots = 27;
    private int upgradeSlots = 2;
    private int pluginSlots = 1;
    private int redstoneSlots = 1;

    private boolean friendlyFire = false;
    private int fireRate = 20;

    public TurretPersonalConfig() {
    }

    public TurretPersonalConfig(float bgAlpha, float energyAlpha, int ammo, int upgrade, int plugin, int redstone) {
        this.backgroundAlpha = bgAlpha;
        this.energyBarAlpha = energyAlpha;
        this.ammoSlots = ammo;
        this.upgradeSlots = upgrade;
        this.pluginSlots = plugin;
        this.redstoneSlots = redstone;
    }

    public TurretPersonalConfig(TurretPersonalConfig other) {
        this.backgroundAlpha = other.backgroundAlpha;
        this.energyBarAlpha = other.energyBarAlpha;
        this.ammoSlots = other.ammoSlots;
        this.upgradeSlots = other.upgradeSlots;
        this.pluginSlots = other.pluginSlots;
        this.redstoneSlots = other.redstoneSlots;
        this.friendlyFire = other.friendlyFire;
        this.fireRate = other.fireRate;
    }

    public void serializeNBT(CompoundTag tag) {
        CompoundTag configTag = new CompoundTag();
        configTag.putFloat("BackgroundAlpha", backgroundAlpha);
        configTag.putFloat("EnergyBarAlpha", energyBarAlpha);
        configTag.putInt("AmmoSlots", ammoSlots);
        configTag.putInt("UpgradeSlots", upgradeSlots);
        configTag.putInt("PluginSlots", pluginSlots);
        configTag.putInt("RedstoneSlots", redstoneSlots);
        configTag.putBoolean("FriendlyFire", friendlyFire);
        configTag.putInt("FireRate", fireRate);
        tag.put("PersonalConfig", configTag);
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("PersonalConfig")) {
            CompoundTag configTag = tag.getCompound("PersonalConfig");
            backgroundAlpha = configTag.getFloat("BackgroundAlpha");
            energyBarAlpha = configTag.getFloat("EnergyBarAlpha");
            ammoSlots = configTag.getInt("AmmoSlots");
            upgradeSlots = configTag.getInt("UpgradeSlots");
            pluginSlots = configTag.getInt("PluginSlots");
            redstoneSlots = configTag.getInt("RedstoneSlots");
            friendlyFire = configTag.getBoolean("FriendlyFire");
            fireRate = configTag.getInt("FireRate");
        }
    }

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
        this.ammoSlots = Math.max(0, Math.min(54, ammoSlots));
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

    public int getTotalSlots() {
        return ammoSlots + upgradeSlots + pluginSlots + redstoneSlots;
    }

    public boolean isValid() {
        return ammoSlots >= 0 && ammoSlots <= 54 &&
               upgradeSlots >= 0 && upgradeSlots <= 9 &&
               pluginSlots >= 0 && pluginSlots <= 9 &&
               redstoneSlots >= 0 && redstoneSlots <= 9 &&
               getTotalSlots() <= 54;
    }

    public void resetToDefault() {
        backgroundAlpha = 0.0F;
        energyBarAlpha = 1.0F;
        ammoSlots = 27;
        upgradeSlots = 2;
        pluginSlots = 1;
        redstoneSlots = 1;
        friendlyFire = false;
        fireRate = 20;
    }
}