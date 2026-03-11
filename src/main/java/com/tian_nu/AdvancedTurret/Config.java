package com.tian_nu.AdvancedTurret;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.slf4j.Logger;

/**
 * 模组配置类
 *
 * <p>使用 Forge 配置 API 管理炮塔平衡与插件参数</p>
 */
@Mod.EventBusSubscriber(modid = TurretMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final Logger LOGGER = LogUtils.getLogger();
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

    // 机枪炮塔
    public static final ForgeConfigSpec.DoubleValue MACHINE_GUN_DAMAGE = BUILDER
            .comment("机枪炮塔子弹伤害")
            .defineInRange("machineGunDamage", 4.0, 0.1, 1000.0);

    public static final ForgeConfigSpec.DoubleValue MACHINE_GUN_RANGE = BUILDER
            .comment("机枪炮塔搜索范围")
            .defineInRange("machineGunRange", 32.0, 1.0, 256.0);

    public static final ForgeConfigSpec.IntValue MACHINE_GUN_FIRE_RATE = BUILDER
            .comment("机枪炮塔射击间隔 (tick/发)")
            .defineInRange("machineGunFireRate", 5, 1, 1200);

    public static final ForgeConfigSpec.DoubleValue MACHINE_GUN_BULLET_SPEED = BUILDER
            .comment("机枪炮塔子弹速度")
            .defineInRange("machineGunBulletSpeed", 3.0, 0.1, 64.0);

    public static final ForgeConfigSpec.IntValue MACHINE_GUN_ENERGY_COST = BUILDER
            .comment("机枪炮塔每次射击能量消耗")
            .defineInRange("machineGunEnergyCost", 100, 1, 100000);

    // 磁轨炮
    public static final ForgeConfigSpec.DoubleValue RAILGUN_DAMAGE = BUILDER
            .comment("磁轨炮炮塔子弹伤害")
            .defineInRange("railgunDamage", 60.0, 0.1, 10000.0);

    public static final ForgeConfigSpec.DoubleValue RAILGUN_RANGE = BUILDER
            .comment("磁轨炮炮塔搜索范围")
            .defineInRange("railgunRange", 64.0, 1.0, 256.0);

    public static final ForgeConfigSpec.IntValue RAILGUN_FIRE_RATE = BUILDER
            .comment("磁轨炮炮塔射击间隔 (tick/发)")
            .defineInRange("railgunFireRate", 100, 1, 2400);

    public static final ForgeConfigSpec.DoubleValue RAILGUN_BULLET_SPEED = BUILDER
            .comment("磁轨炮炮塔子弹速度")
            .defineInRange("railgunBulletSpeed", 6.0, 0.1, 128.0);

    public static final ForgeConfigSpec.IntValue RAILGUN_ENERGY_COST = BUILDER
            .comment("磁轨炮炮塔每次射击能量消耗")
            .defineInRange("railgunEnergyCost", 10000, 1, 1000000);

    public static final ForgeConfigSpec.IntValue RAILGUN_PENETRATION = BUILDER
            .comment("磁轨炮炮塔穿透目标数量")
            .defineInRange("railgunPenetrationCount", 3, 1, 64);

    // 激光炮塔
    public static final ForgeConfigSpec.DoubleValue LASER_DAMAGE_PER_TICK = BUILDER
            .comment("激光炮塔每tick伤害")
            .defineInRange("laserDamagePerTick", 2.0, 0.1, 1000.0);

    public static final ForgeConfigSpec.DoubleValue LASER_RANGE = BUILDER
            .comment("激光炮塔搜索范围")
            .defineInRange("laserRange", 32.0, 1.0, 256.0);

    public static final ForgeConfigSpec.IntValue LASER_ENERGY_PER_TICK = BUILDER
            .comment("激光炮塔每tick能量消耗")
            .defineInRange("laserEnergyPerTick", 500, 1, 100000);

    public static final ForgeConfigSpec.IntValue LASER_FIRE_SECONDS = BUILDER
            .comment("激光炮塔点燃时间 (秒)")
            .defineInRange("laserFireSeconds", 3, 0, 60);

    public static final ForgeConfigSpec.DoubleValue LASER_AIM_THRESHOLD = BUILDER
            .comment("激光炮塔瞄准判定阈值")
            .defineInRange("laserAimThreshold", 0.26, 0.01, 3.14);

    public static final ForgeConfigSpec.DoubleValue LASER_TURN_SPEED = BUILDER
            .comment("激光炮塔转向速度")
            .defineInRange("laserTurnSpeed", 0.18, 0.01, 3.14);

    // 火箭炮塔
    public static final ForgeConfigSpec.DoubleValue ROCKET_DIRECT_DAMAGE = BUILDER
            .comment("火箭炮塔直击伤害")
            .defineInRange("rocketDirectDamage", 10.0, 0.1, 10000.0);

    public static final ForgeConfigSpec.DoubleValue ROCKET_EXPLOSION_DAMAGE = BUILDER
            .comment("火箭炮塔爆炸伤害")
            .defineInRange("rocketExplosionDamage", 10.0, 0.0, 10000.0);

    public static final ForgeConfigSpec.DoubleValue ROCKET_EXPLOSION_RADIUS = BUILDER
            .comment("火箭炮塔爆炸半径")
            .defineInRange("rocketExplosionRadius", 4.0, 0.1, 64.0);

    public static final ForgeConfigSpec.DoubleValue ROCKET_RANGE = BUILDER
            .comment("火箭炮塔搜索范围")
            .defineInRange("rocketRange", 48.0, 1.0, 256.0);

    public static final ForgeConfigSpec.IntValue ROCKET_FIRE_RATE = BUILDER
            .comment("火箭炮塔射击间隔 (tick/发)")
            .defineInRange("rocketFireRate", 100, 1, 2400);

    public static final ForgeConfigSpec.DoubleValue ROCKET_BULLET_SPEED = BUILDER
            .comment("火箭炮塔火箭初速度")
            .defineInRange("rocketBulletSpeed", 2.0, 0.1, 64.0);

    public static final ForgeConfigSpec.DoubleValue ROCKET_ACCELERATION = BUILDER
            .comment("火箭炮塔火箭加速度")
            .defineInRange("rocketAcceleration", 0.047, 0.0, 4.0);

    public static final ForgeConfigSpec.IntValue ROCKET_ENERGY_COST = BUILDER
            .comment("火箭炮塔每次射击能量消耗")
            .defineInRange("rocketEnergyCost", 5000, 1, 1000000);

    // 导弹炮塔
    public static final ForgeConfigSpec.DoubleValue MISSILE_DIRECT_DAMAGE = BUILDER
            .comment("导弹炮塔直击伤害")
            .defineInRange("missileDirectDamage", 10.0, 0.1, 10000.0);

    public static final ForgeConfigSpec.DoubleValue MISSILE_EXPLOSION_DAMAGE = BUILDER
            .comment("导弹炮塔爆炸伤害")
            .defineInRange("missileExplosionDamage", 15.0, 0.0, 10000.0);

    public static final ForgeConfigSpec.DoubleValue MISSILE_EXPLOSION_RADIUS = BUILDER
            .comment("导弹炮塔爆炸半径")
            .defineInRange("missileExplosionRadius", 4.0, 0.1, 64.0);

    public static final ForgeConfigSpec.DoubleValue MISSILE_RANGE = BUILDER
            .comment("导弹炮塔搜索范围")
            .defineInRange("missileRange", 64.0, 1.0, 256.0);

    public static final ForgeConfigSpec.IntValue MISSILE_FIRE_RATE = BUILDER
            .comment("导弹炮塔射击间隔 (tick/发)")
            .defineInRange("missileFireRate", 133, 1, 2400);

    public static final ForgeConfigSpec.DoubleValue MISSILE_BULLET_SPEED = BUILDER
            .comment("导弹炮塔导弹初速度")
            .defineInRange("missileBulletSpeed", 1.5, 0.1, 64.0);

    public static final ForgeConfigSpec.DoubleValue MISSILE_ACCELERATION = BUILDER
            .comment("导弹炮塔导弹加速度")
            .defineInRange("missileAcceleration", 0.03, 0.0, 4.0);

    public static final ForgeConfigSpec.DoubleValue MISSILE_TURN_RATE = BUILDER
            .comment("导弹炮塔导弹转向速率")
            .defineInRange("missileTurnRate", 0.3, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue MISSILE_ENERGY_COST = BUILDER
            .comment("导弹炮塔每次射击能量消耗")
            .defineInRange("missileEnergyCost", 10000, 1, 1000000);

    // 榴弹炮塔
    public static final ForgeConfigSpec.DoubleValue GRENADE_LAUNCHER_DIRECT_DAMAGE = BUILDER
            .comment("榴弹炮塔直击伤害")
            .defineInRange("grenadeLauncherDirectDamage", 5.0, 0.1, 10000.0);

    public static final ForgeConfigSpec.DoubleValue GRENADE_LAUNCHER_EXPLOSION_DAMAGE = BUILDER
            .comment("榴弹炮塔爆炸伤害")
            .defineInRange("grenadeLauncherExplosionDamage", 10.0, 0.0, 10000.0);

    public static final ForgeConfigSpec.DoubleValue GRENADE_LAUNCHER_EXPLOSION_RADIUS = BUILDER
            .comment("榴弹炮塔爆炸半径")
            .defineInRange("grenadeLauncherExplosionRadius", 3.0, 0.1, 64.0);

    public static final ForgeConfigSpec.DoubleValue GRENADE_LAUNCHER_RANGE = BUILDER
            .comment("榴弹炮塔搜索范围")
            .defineInRange("grenadeLauncherRange", 32.0, 1.0, 256.0);

    public static final ForgeConfigSpec.IntValue GRENADE_LAUNCHER_FIRE_RATE = BUILDER
            .comment("榴弹炮塔射击间隔 (tick/发)")
            .defineInRange("grenadeLauncherFireRate", 40, 1, 2400);

    public static final ForgeConfigSpec.DoubleValue GRENADE_LAUNCHER_BULLET_SPEED = BUILDER
            .comment("榴弹炮塔榴弹初速度")
            .defineInRange("grenadeLauncherBulletSpeed", 1.5, 0.1, 64.0);

    public static final ForgeConfigSpec.DoubleValue GRENADE_LAUNCHER_GRAVITY = BUILDER
            .comment("榴弹炮塔重力系数")
            .defineInRange("grenadeLauncherGravity", 0.05, 0.001, 1.0);

    public static final ForgeConfigSpec.DoubleValue GRENADE_LAUNCHER_MIN_ARC_HEIGHT = BUILDER
            .comment("榴弹炮塔最小抛物线高度")
            .defineInRange("grenadeLauncherMinArcHeight", 0.8, 0.0, 64.0);

    public static final ForgeConfigSpec.DoubleValue GRENADE_LAUNCHER_MAX_ARC_HEIGHT = BUILDER
            .comment("榴弹炮塔最大抛物线高度")
            .defineInRange("grenadeLauncherMaxArcHeight", 5.0, 0.0, 128.0);

    public static final ForgeConfigSpec.IntValue GRENADE_LAUNCHER_ENERGY_COST = BUILDER
            .comment("榴弹炮塔每次射击能量消耗")
            .defineInRange("grenadeLauncherEnergyCost", 3000, 1, 1000000);

    // 垃圾炮塔
    public static final ForgeConfigSpec.DoubleValue JUNK_TURRET_DAMAGE = BUILDER
            .comment("垃圾炮塔投射物伤害")
            .defineInRange("junkTurretDamage", 4.0, 0.1, 1000.0);

    public static final ForgeConfigSpec.DoubleValue JUNK_TURRET_RANGE = BUILDER
            .comment("垃圾炮塔搜索范围")
            .defineInRange("junkTurretRange", 16.0, 1.0, 256.0);

    public static final ForgeConfigSpec.IntValue JUNK_TURRET_FIRE_RATE = BUILDER
            .comment("垃圾炮塔射击间隔 (tick/发)")
            .defineInRange("junkTurretFireRate", 40, 1, 2400);

    public static final ForgeConfigSpec.DoubleValue JUNK_TURRET_BULLET_SPEED = BUILDER
            .comment("垃圾炮塔投射物初速度")
            .defineInRange("junkTurretBulletSpeed", 2.0, 0.1, 64.0);

    public static final ForgeConfigSpec.DoubleValue JUNK_TURRET_GRAVITY = BUILDER
            .comment("垃圾炮塔重力系数")
            .defineInRange("junkTurretGravity", 0.05, 0.001, 1.0);

    public static final ForgeConfigSpec.DoubleValue JUNK_TURRET_MIN_ARC_HEIGHT = BUILDER
            .comment("垃圾炮塔最小抛物线高度")
            .defineInRange("junkTurretMinArcHeight", 0.6, 0.0, 64.0);

    public static final ForgeConfigSpec.DoubleValue JUNK_TURRET_MAX_ARC_HEIGHT = BUILDER
            .comment("垃圾炮塔最大抛物线高度")
            .defineInRange("junkTurretMaxArcHeight", 4.2, 0.0, 128.0);

    public static final ForgeConfigSpec.IntValue JUNK_TURRET_ENERGY_COST = BUILDER
            .comment("垃圾炮塔每次射击能量消耗")
            .defineInRange("junkTurretEnergyCost", 20, 1, 100000);

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

    public static double machineGunDamage;
    public static double machineGunRange;
    public static int machineGunFireRate;
    public static double machineGunBulletSpeed;
    public static int machineGunEnergyCost;

    public static double railgunDamage;
    public static double railgunRange;
    public static int railgunFireRate;
    public static double railgunBulletSpeed;
    public static int railgunEnergyCost;
    public static int railgunPenetrationCount;

    public static double laserDamagePerTick;
    public static double laserRange;
    public static int laserEnergyPerTick;
    public static int laserFireSeconds;
    public static double laserAimThreshold;
    public static double laserTurnSpeed;

    public static double rocketDirectDamage;
    public static double rocketExplosionDamage;
    public static double rocketExplosionRadius;
    public static double rocketRange;
    public static int rocketFireRate;
    public static double rocketBulletSpeed;
    public static double rocketAcceleration;
    public static int rocketEnergyCost;

    public static double missileDirectDamage;
    public static double missileExplosionDamage;
    public static double missileExplosionRadius;
    public static double missileRange;
    public static int missileFireRate;
    public static double missileBulletSpeed;
    public static double missileAcceleration;
    public static double missileTurnRate;
    public static int missileEnergyCost;

    public static double grenadeLauncherDirectDamage;
    public static double grenadeLauncherExplosionDamage;
    public static double grenadeLauncherExplosionRadius;
    public static double grenadeLauncherRange;
    public static int grenadeLauncherFireRate;
    public static double grenadeLauncherBulletSpeed;
    public static double grenadeLauncherGravity;
    public static double grenadeLauncherMinArcHeight;
    public static double grenadeLauncherMaxArcHeight;
    public static int grenadeLauncherEnergyCost;

    public static double junkTurretDamage;
    public static double junkTurretRange;
    public static int junkTurretFireRate;
    public static double junkTurretBulletSpeed;
    public static double junkTurretGravity;
    public static double junkTurretMinArcHeight;
    public static double junkTurretMaxArcHeight;
    public static int junkTurretEnergyCost;

    public static int solarEnergyGeneration;
    public static double ammoRecycleChance;
    public static int redstoneToEnergyRatio;

    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }

        turretBaseMaxEnergyT1 = TURRET_BASE_MAX_ENERGY_T1.get();
        turretBaseMaxEnergyT2 = TURRET_BASE_MAX_ENERGY_T2.get();
        turretBaseMaxEnergyT3 = TURRET_BASE_MAX_ENERGY_T3.get();
        maxTransferRate = MAX_TRANSFER_RATE.get();

        machineGunDamage = MACHINE_GUN_DAMAGE.get();
        machineGunRange = MACHINE_GUN_RANGE.get();
        machineGunFireRate = MACHINE_GUN_FIRE_RATE.get();
        machineGunBulletSpeed = MACHINE_GUN_BULLET_SPEED.get();
        machineGunEnergyCost = MACHINE_GUN_ENERGY_COST.get();

        railgunDamage = RAILGUN_DAMAGE.get();
        railgunRange = RAILGUN_RANGE.get();
        railgunFireRate = RAILGUN_FIRE_RATE.get();
        railgunBulletSpeed = RAILGUN_BULLET_SPEED.get();
        railgunEnergyCost = RAILGUN_ENERGY_COST.get();
        railgunPenetrationCount = RAILGUN_PENETRATION.get();

        laserDamagePerTick = LASER_DAMAGE_PER_TICK.get();
        laserRange = LASER_RANGE.get();
        laserEnergyPerTick = LASER_ENERGY_PER_TICK.get();
        laserFireSeconds = LASER_FIRE_SECONDS.get();
        laserAimThreshold = LASER_AIM_THRESHOLD.get();
        laserTurnSpeed = LASER_TURN_SPEED.get();

        rocketDirectDamage = ROCKET_DIRECT_DAMAGE.get();
        rocketExplosionDamage = ROCKET_EXPLOSION_DAMAGE.get();
        rocketExplosionRadius = ROCKET_EXPLOSION_RADIUS.get();
        rocketRange = ROCKET_RANGE.get();
        rocketFireRate = ROCKET_FIRE_RATE.get();
        rocketBulletSpeed = ROCKET_BULLET_SPEED.get();
        rocketAcceleration = ROCKET_ACCELERATION.get();
        rocketEnergyCost = ROCKET_ENERGY_COST.get();

        missileDirectDamage = MISSILE_DIRECT_DAMAGE.get();
        missileExplosionDamage = MISSILE_EXPLOSION_DAMAGE.get();
        missileExplosionRadius = MISSILE_EXPLOSION_RADIUS.get();
        missileRange = MISSILE_RANGE.get();
        missileFireRate = MISSILE_FIRE_RATE.get();
        missileBulletSpeed = MISSILE_BULLET_SPEED.get();
        missileAcceleration = MISSILE_ACCELERATION.get();
        missileTurnRate = MISSILE_TURN_RATE.get();
        missileEnergyCost = MISSILE_ENERGY_COST.get();

        grenadeLauncherDirectDamage = GRENADE_LAUNCHER_DIRECT_DAMAGE.get();
        grenadeLauncherExplosionDamage = GRENADE_LAUNCHER_EXPLOSION_DAMAGE.get();
        grenadeLauncherExplosionRadius = GRENADE_LAUNCHER_EXPLOSION_RADIUS.get();
        grenadeLauncherRange = GRENADE_LAUNCHER_RANGE.get();
        grenadeLauncherFireRate = GRENADE_LAUNCHER_FIRE_RATE.get();
        grenadeLauncherBulletSpeed = GRENADE_LAUNCHER_BULLET_SPEED.get();
        grenadeLauncherGravity = GRENADE_LAUNCHER_GRAVITY.get();
        grenadeLauncherMinArcHeight = GRENADE_LAUNCHER_MIN_ARC_HEIGHT.get();
        grenadeLauncherMaxArcHeight = GRENADE_LAUNCHER_MAX_ARC_HEIGHT.get();
        grenadeLauncherEnergyCost = GRENADE_LAUNCHER_ENERGY_COST.get();

        junkTurretDamage = JUNK_TURRET_DAMAGE.get();
        junkTurretRange = JUNK_TURRET_RANGE.get();
        junkTurretFireRate = JUNK_TURRET_FIRE_RATE.get();
        junkTurretBulletSpeed = JUNK_TURRET_BULLET_SPEED.get();
        junkTurretGravity = JUNK_TURRET_GRAVITY.get();
        junkTurretMinArcHeight = JUNK_TURRET_MIN_ARC_HEIGHT.get();
        junkTurretMaxArcHeight = JUNK_TURRET_MAX_ARC_HEIGHT.get();
        junkTurretEnergyCost = JUNK_TURRET_ENERGY_COST.get();

        solarEnergyGeneration = SOLAR_ENERGY_GENERATION.get();
        ammoRecycleChance = AMMO_RECYCLE_CHANCE.get();
        redstoneToEnergyRatio = REDSTONE_TO_ENERGY_RATIO.get();

        LOGGER.info("已加载炮塔配置文件: {}", event.getConfig().getFileName());
        LOGGER.info("当前关键能耗配置: 激光={} FE/t, 磁轨={} FE/发", laserEnergyPerTick, railgunEnergyCost);

        if (laserEnergyPerTick == 10 || railgunEnergyCost == 1000) {
            LOGGER.warn("检测到旧版能耗配置仍在生效。当前值: 激光={} FE/t, 磁轨={} FE/发。若这不是你的主动配置，请检查并更新 {}",
                    laserEnergyPerTick,
                    railgunEnergyCost,
                    event.getConfig().getFileName());
        }
    }
}
