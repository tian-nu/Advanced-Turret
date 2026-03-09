package com.tian_nu.AdvancedTurret;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 模组配置类
 * 
 * <p>使用Forge的配置API管理模组设置</p>
 * 
 * @author tian_nu
 */
@Mod.EventBusSubscriber(modid = TurretMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    
    // 通用配置
    public static final ForgeConfigSpec.IntValue TURRET_BASE_MAX_ENERGY_T1 = BUILDER
            .comment("T1炮塔基座最大能量存储")
            .defineInRange("turretBaseMaxEnergyT1", 10000, 1000, 100000);
    
    public static final ForgeConfigSpec.IntValue TURRET_BASE_MAX_ENERGY_T2 = BUILDER
            .comment("T2炮塔基座最大能量存储")
            .defineInRange("turretBaseMaxEnergyT2", 40000, 1000, 500000);
    
    public static final ForgeConfigSpec.IntValue TURRET_BASE_MAX_ENERGY_T3 = BUILDER
            .comment("T3炮塔基座最大能量存储")
            .defineInRange("turretBaseMaxEnergyT3", 100000, 1000, 1000000);
    
    public static final ForgeConfigSpec.IntValue MAX_TRANSFER_RATE = BUILDER
            .comment("能量最大传输速率 (FE/tick)")
            .defineInRange("maxTransferRate", 1000, 100, 10000);
    
    public static final ForgeConfigSpec.IntValue MACHINE_GUN_ENERGY_COST = BUILDER
            .comment("机枪炮塔每次射击能量消耗")
            .defineInRange("machineGunEnergyCost", 100, 10, 1000);
    
    public static final ForgeConfigSpec.IntValue RAILGUN_ENERGY_COST = BUILDER
            .comment("磁轨炮炮塔每次射击能量消耗")
            .defineInRange("railgunEnergyCost", 10000, 100, 100000);

    public static final ForgeConfigSpec.IntValue LASER_ENERGY_PER_TICK = BUILDER
            .comment("激光炮塔每tick能量消耗")
            .defineInRange("laserEnergyPerTick", 500, 1, 10000);
    
    public static final ForgeConfigSpec.IntValue ROCKET_ENERGY_COST = BUILDER
            .comment("火箭炮塔每次射击能量消耗")
            .defineInRange("rocketEnergyCost", 5000, 100, 50000);
    
    public static final ForgeConfigSpec.IntValue MISSILE_ENERGY_COST = BUILDER
            .comment("导弹炮塔每次射击能量消耗")
            .defineInRange("missileEnergyCost", 10000, 100, 100000);
    
    public static final ForgeConfigSpec.IntValue GRENADE_LAUNCHER_ENERGY_COST = BUILDER
            .comment("榴弹发射器炮塔每次射击能量消耗")
            .defineInRange("grenadeLauncherEnergyCost", 3000, 100, 30000);
    
    public static final ForgeConfigSpec.IntValue JUNK_TURRET_ENERGY_COST = BUILDER
            .comment("垃圾炮塔每次射击能量消耗")
            .defineInRange("junkTurretEnergyCost", 20, 1, 500);
    
    // 插件配置
    public static final ForgeConfigSpec.IntValue SOLAR_ENERGY_GENERATION = BUILDER
            .comment("太阳能插件发电量 (FE/tick)")
            .defineInRange("solarEnergyGeneration", 10, 1, 500);
    
    public static final ForgeConfigSpec.DoubleValue AMMO_RECYCLE_CHANCE = BUILDER
            .comment("弹药回收插件：不消耗弹药的概率 (0.0-1.0)")
            .defineInRange("ammoRecycleChance", 0.2, 0.0, 1.0);
    
    public static final ForgeConfigSpec.IntValue REDSTONE_TO_ENERGY_RATIO = BUILDER
            .comment("红石转化插件：每个红石转化能量 (FE/个)")
            .defineInRange("redstoneToEnergyRatio", 2000, 100, 10000);
    
    static final ForgeConfigSpec SPEC = BUILDER.build();
    
    // 运行时配置值
    public static int turretBaseMaxEnergyT1;
    public static int turretBaseMaxEnergyT2;
    public static int turretBaseMaxEnergyT3;
    public static int maxTransferRate;
    public static int machineGunEnergyCost;
    public static int railgunEnergyCost;
    public static int laserEnergyPerTick;
    public static int rocketEnergyCost;
    public static int missileEnergyCost;
    public static int grenadeLauncherEnergyCost;
    public static int junkTurretEnergyCost;
    public static int solarEnergyGeneration;
    public static double ammoRecycleChance;
    public static int redstoneToEnergyRatio;
    
    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        turretBaseMaxEnergyT1 = TURRET_BASE_MAX_ENERGY_T1.get();
        turretBaseMaxEnergyT2 = TURRET_BASE_MAX_ENERGY_T2.get();
        turretBaseMaxEnergyT3 = TURRET_BASE_MAX_ENERGY_T3.get();
        maxTransferRate = MAX_TRANSFER_RATE.get();
        machineGunEnergyCost = MACHINE_GUN_ENERGY_COST.get();
        railgunEnergyCost = RAILGUN_ENERGY_COST.get();
        laserEnergyPerTick = LASER_ENERGY_PER_TICK.get();
        rocketEnergyCost = ROCKET_ENERGY_COST.get();
        missileEnergyCost = MISSILE_ENERGY_COST.get();
        grenadeLauncherEnergyCost = GRENADE_LAUNCHER_ENERGY_COST.get();
        junkTurretEnergyCost = JUNK_TURRET_ENERGY_COST.get();
        solarEnergyGeneration = SOLAR_ENERGY_GENERATION.get();
        ammoRecycleChance = AMMO_RECYCLE_CHANCE.get();
        redstoneToEnergyRatio = REDSTONE_TO_ENERGY_RATIO.get();
    }
}
