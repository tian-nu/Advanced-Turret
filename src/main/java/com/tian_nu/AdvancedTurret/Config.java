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
            .defineInRange("railgunEnergyCost", 1000, 100, 10000);
    
    static final ForgeConfigSpec SPEC = BUILDER.build();
    
    // 运行时配置值
    public static int turretBaseMaxEnergyT1;
    public static int turretBaseMaxEnergyT2;
    public static int turretBaseMaxEnergyT3;
    public static int maxTransferRate;
    public static int machineGunEnergyCost;
    public static int railgunEnergyCost;
    
    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        turretBaseMaxEnergyT1 = TURRET_BASE_MAX_ENERGY_T1.get();
        turretBaseMaxEnergyT2 = TURRET_BASE_MAX_ENERGY_T2.get();
        turretBaseMaxEnergyT3 = TURRET_BASE_MAX_ENERGY_T3.get();
        maxTransferRate = MAX_TRANSFER_RATE.get();
        machineGunEnergyCost = MACHINE_GUN_ENERGY_COST.get();
        railgunEnergyCost = RAILGUN_ENERGY_COST.get();
    }
}
