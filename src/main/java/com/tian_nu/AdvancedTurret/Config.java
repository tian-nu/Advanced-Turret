package com.tian_nu.AdvancedTurret;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 婵☆垽绱曠划宥夋煀瀹ュ洨鏋傜紒? *
 * <p>濞达綀娉曢弫?Forge 闂佹澘绉堕悿?API 缂佺媴绱曢幃濠囨倷椤旂⒈鏁婃鐐茬枃閵嗏偓濞戞挸瀛╄ぐ鍐╃鐠哄搫妫橀柡?/p>
 */
@Mod.EventBusSubscriber(modid = TurretMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // 闂侇偅姘ㄩ弫銈夋煀瀹ュ洨鏋?
    public static final ForgeConfigSpec.IntValue TURRET_BASE_MAX_ENERGY_T1 = BUILDER
            .comment("config")
            .defineInRange("turretBaseMaxEnergyT1", 10000, 1000, 100000);

    public static final ForgeConfigSpec.IntValue TURRET_BASE_MAX_ENERGY_T2 = BUILDER
            .comment("config")
            .defineInRange("turretBaseMaxEnergyT2", 40000, 1000, 500000);

    public static final ForgeConfigSpec.IntValue TURRET_BASE_MAX_ENERGY_T3 = BUILDER
            .comment("config")
            .defineInRange("turretBaseMaxEnergyT3", 100000, 1000, 1000000);

    public static final ForgeConfigSpec.IntValue MAX_TRANSFER_RATE = BUILDER
            .comment("config")
            .defineInRange("maxTransferRate", 1000, 100, 10000);

    public static final ForgeConfigSpec.IntValue TURRET_BASE_MAX_TRANSFER_RATE_T1 = BUILDER
            .comment("config")
            .defineInRange("turretBaseMaxTransferRateT1", 100, 10, 100000);

    public static final ForgeConfigSpec.IntValue TURRET_BASE_MAX_TRANSFER_RATE_T2 = BUILDER
            .comment("config")
            .defineInRange("turretBaseMaxTransferRateT2", 200, 10, 100000);

    public static final ForgeConfigSpec.IntValue TURRET_BASE_MAX_TRANSFER_RATE_T3 = BUILDER
            .comment("config")
            .defineInRange("turretBaseMaxTransferRateT3", 500, 10, 100000);

    public static final ForgeConfigSpec.IntValue TURRET_BASE_MAX_TRANSFER_RATE_T4 = BUILDER
            .comment("config")
            .defineInRange("turretBaseMaxTransferRateT4", 2000, 10, 100000);

    public static final ForgeConfigSpec.IntValue TURRET_BASE_MAX_TRANSFER_RATE_T5 = BUILDER
            .comment("config")
            .defineInRange("turretBaseMaxTransferRateT5", 10000, 10, 1000000);

    // 闁哄牏鍎ら悘娆撴倷椤旂⒈鏁?
    public static final ForgeConfigSpec.DoubleValue MACHINE_GUN_DAMAGE = BUILDER
            .comment("config")
            .defineInRange("machineGunDamage", 4.0, 0.1, 1000.0);

    public static final ForgeConfigSpec.DoubleValue MACHINE_GUN_RANGE = BUILDER
            .comment("config")
            .defineInRange("machineGunRange", 32.0, 1.0, 256.0);

    public static final ForgeConfigSpec.IntValue MACHINE_GUN_FIRE_RATE = BUILDER
            .comment("config")
            .defineInRange("machineGunFireRate", 5, 1, 1200);

    public static final ForgeConfigSpec.DoubleValue MACHINE_GUN_BULLET_SPEED = BUILDER
            .comment("config")
            .defineInRange("machineGunBulletSpeed", 3.0, 0.1, 64.0);

    public static final ForgeConfigSpec.IntValue MACHINE_GUN_ENERGY_COST = BUILDER
            .comment("config")
            .defineInRange("machineGunEnergyCost", 100, 1, 100000);

    public static final ForgeConfigSpec.DoubleValue RAILGUN_DAMAGE = BUILDER
            .comment("config")
            .defineInRange("railgunDamage", 60.0, 0.1, 10000.0);

    public static final ForgeConfigSpec.DoubleValue RAILGUN_RANGE = BUILDER
            .comment("config")
            .defineInRange("railgunRange", 64.0, 1.0, 256.0);

    public static final ForgeConfigSpec.IntValue RAILGUN_FIRE_RATE = BUILDER
            .comment("config")
            .defineInRange("railgunFireRate", 100, 1, 2400);

    public static final ForgeConfigSpec.DoubleValue RAILGUN_BULLET_SPEED = BUILDER
            .comment("config")
            .defineInRange("railgunBulletSpeed", 6.0, 0.1, 128.0);

    public static final ForgeConfigSpec.IntValue RAILGUN_ENERGY_COST = BUILDER
            .comment("config")
            .defineInRange("railgunEnergyCost", 10000, 1, 1000000);

    public static final ForgeConfigSpec.IntValue RAILGUN_PENETRATION = BUILDER
            .comment("config")
            .defineInRange("railgunPenetrationCount", 3, 1, 64);

    public static final ForgeConfigSpec.DoubleValue LASER_DAMAGE_PER_TICK = BUILDER
            .comment("config")
            .defineInRange("laserDamagePerTick", 2.0, 0.1, 1000.0);

    public static final ForgeConfigSpec.DoubleValue LASER_RANGE = BUILDER
            .comment("config")
            .defineInRange("laserRange", 32.0, 1.0, 256.0);

    public static final ForgeConfigSpec.IntValue LASER_ENERGY_PER_TICK = BUILDER
            .comment("config")
            .defineInRange("laserEnergyPerTick", 500, 1, 100000);

    public static final ForgeConfigSpec.IntValue LASER_FIRE_SECONDS = BUILDER
            .comment("config")
            .defineInRange("laserFireSeconds", 3, 0, 60);

    public static final ForgeConfigSpec.DoubleValue LASER_AIM_THRESHOLD = BUILDER
            .comment("config")
            .defineInRange("laserAimThreshold", 0.26, 0.01, 3.14);

    public static final ForgeConfigSpec.DoubleValue LASER_TURN_SPEED = BUILDER
            .comment("config")
            .defineInRange("laserTurnSpeed", 0.18, 0.01, 3.14);

    // 闁诲浚鍋嗛鍕倷椤旂⒈鏁?
    public static final ForgeConfigSpec.DoubleValue ROCKET_DIRECT_DAMAGE = BUILDER
            .comment("config")
            .defineInRange("rocketDirectDamage", 10.0, 0.1, 10000.0);

    public static final ForgeConfigSpec.DoubleValue ROCKET_EXPLOSION_DAMAGE = BUILDER
            .comment("config")
            .defineInRange("rocketExplosionDamage", 10.0, 0.0, 10000.0);

    public static final ForgeConfigSpec.DoubleValue ROCKET_EXPLOSION_RADIUS = BUILDER
            .comment("config")
            .defineInRange("rocketExplosionRadius", 4.0, 0.1, 64.0);

    public static final ForgeConfigSpec.DoubleValue ROCKET_RANGE = BUILDER
            .comment("config")
            .defineInRange("rocketRange", 48.0, 1.0, 256.0);

    public static final ForgeConfigSpec.IntValue ROCKET_FIRE_RATE = BUILDER
            .comment("config")
            .defineInRange("rocketFireRate", 100, 1, 2400);

    public static final ForgeConfigSpec.DoubleValue ROCKET_BULLET_SPEED = BUILDER
            .comment("config")
            .defineInRange("rocketBulletSpeed", 2.0, 0.1, 64.0);

    public static final ForgeConfigSpec.DoubleValue ROCKET_ACCELERATION = BUILDER
            .comment("config")
            .defineInRange("rocketAcceleration", 0.047, 0.0, 4.0);

    public static final ForgeConfigSpec.IntValue ROCKET_ENERGY_COST = BUILDER
            .comment("config")
            .defineInRange("rocketEnergyCost", 5000, 1, 1000000);

    // 閻庣數鍘ч懘濠囨倷椤旂⒈鏁?
    public static final ForgeConfigSpec.DoubleValue MISSILE_DIRECT_DAMAGE = BUILDER
            .comment("config")
            .defineInRange("missileDirectDamage", 10.0, 0.1, 10000.0);

    public static final ForgeConfigSpec.DoubleValue MISSILE_EXPLOSION_DAMAGE = BUILDER
            .comment("config")
            .defineInRange("missileExplosionDamage", 15.0, 0.0, 10000.0);

    public static final ForgeConfigSpec.DoubleValue MISSILE_EXPLOSION_RADIUS = BUILDER
            .comment("config")
            .defineInRange("missileExplosionRadius", 4.0, 0.1, 64.0);

    public static final ForgeConfigSpec.DoubleValue MISSILE_RANGE = BUILDER
            .comment("config")
            .defineInRange("missileRange", 64.0, 1.0, 256.0);

    public static final ForgeConfigSpec.IntValue MISSILE_FIRE_RATE = BUILDER
            .comment("config")
            .defineInRange("missileFireRate", 133, 1, 2400);

    public static final ForgeConfigSpec.DoubleValue MISSILE_BULLET_SPEED = BUILDER
            .comment("config")
            .defineInRange("missileBulletSpeed", 1.5, 0.1, 64.0);

    public static final ForgeConfigSpec.DoubleValue MISSILE_ACCELERATION = BUILDER
            .comment("config")
            .defineInRange("missileAcceleration", 0.03, 0.0, 4.0);

    public static final ForgeConfigSpec.DoubleValue MISSILE_TURN_RATE = BUILDER
            .comment("config")
            .defineInRange("missileTurnRate", 0.3, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue MISSILE_ENERGY_COST = BUILDER
            .comment("config")
            .defineInRange("missileEnergyCost", 10000, 1, 1000000);

    // 婵帗娼欓懘濠囨倷椤旂⒈鏁?
    public static final ForgeConfigSpec.DoubleValue GRENADE_LAUNCHER_DIRECT_DAMAGE = BUILDER
            .comment("config")
            .defineInRange("grenadeLauncherDirectDamage", 5.0, 0.1, 10000.0);

    public static final ForgeConfigSpec.DoubleValue GRENADE_LAUNCHER_EXPLOSION_DAMAGE = BUILDER
            .comment("config")
            .defineInRange("grenadeLauncherExplosionDamage", 10.0, 0.0, 10000.0);

    public static final ForgeConfigSpec.DoubleValue GRENADE_LAUNCHER_EXPLOSION_RADIUS = BUILDER
            .comment("config")
            .defineInRange("grenadeLauncherExplosionRadius", 3.0, 0.1, 64.0);

    public static final ForgeConfigSpec.DoubleValue GRENADE_LAUNCHER_RANGE = BUILDER
            .comment("config")
            .defineInRange("grenadeLauncherRange", 32.0, 1.0, 256.0);

    public static final ForgeConfigSpec.IntValue GRENADE_LAUNCHER_FIRE_RATE = BUILDER
            .comment("config")
            .defineInRange("grenadeLauncherFireRate", 40, 1, 2400);

    public static final ForgeConfigSpec.DoubleValue GRENADE_LAUNCHER_BULLET_SPEED = BUILDER
            .comment("config")
            .defineInRange("grenadeLauncherBulletSpeed", 1.5, 0.1, 64.0);

    public static final ForgeConfigSpec.DoubleValue GRENADE_LAUNCHER_GRAVITY = BUILDER
            .comment("config")
            .defineInRange("grenadeLauncherGravity", 0.05, 0.001, 1.0);

    public static final ForgeConfigSpec.DoubleValue GRENADE_LAUNCHER_MIN_ARC_HEIGHT = BUILDER
            .comment("config")
            .defineInRange("grenadeLauncherMinArcHeight", 0.8, 0.0, 64.0);

    public static final ForgeConfigSpec.DoubleValue GRENADE_LAUNCHER_MAX_ARC_HEIGHT = BUILDER
            .comment("config")
            .defineInRange("grenadeLauncherMaxArcHeight", 5.0, 0.0, 128.0);

    public static final ForgeConfigSpec.IntValue GRENADE_LAUNCHER_ENERGY_COST = BUILDER
            .comment("config")
            .defineInRange("grenadeLauncherEnergyCost", 3000, 1, 1000000);

    // 闁搞劌鍟┃鍥倷椤旂⒈鏁?
    public static final ForgeConfigSpec.DoubleValue JUNK_TURRET_DAMAGE = BUILDER
            .comment("config")
            .defineInRange("junkTurretDamage", 4.0, 0.1, 1000.0);

    public static final ForgeConfigSpec.DoubleValue JUNK_TURRET_RANGE = BUILDER
            .comment("config")
            .defineInRange("junkTurretRange", 16.0, 1.0, 256.0);

    public static final ForgeConfigSpec.IntValue JUNK_TURRET_FIRE_RATE = BUILDER
            .comment("config")
            .defineInRange("junkTurretFireRate", 40, 1, 2400);

    public static final ForgeConfigSpec.DoubleValue JUNK_TURRET_BULLET_SPEED = BUILDER
            .comment("config")
            .defineInRange("junkTurretBulletSpeed", 2.0, 0.1, 64.0);

    public static final ForgeConfigSpec.DoubleValue JUNK_TURRET_GRAVITY = BUILDER
            .comment("config")
            .defineInRange("junkTurretGravity", 0.05, 0.001, 1.0);

    public static final ForgeConfigSpec.DoubleValue JUNK_TURRET_MIN_ARC_HEIGHT = BUILDER
            .comment("config")
            .defineInRange("junkTurretMinArcHeight", 0.6, 0.0, 64.0);

    public static final ForgeConfigSpec.DoubleValue JUNK_TURRET_MAX_ARC_HEIGHT = BUILDER
            .comment("config")
            .defineInRange("junkTurretMaxArcHeight", 4.2, 0.0, 128.0);

    public static final ForgeConfigSpec.IntValue JUNK_TURRET_ENERGY_COST = BUILDER
            .comment("config")
            .defineInRange("junkTurretEnergyCost", 20, 1, 100000);

    // 闁圭粯甯婂▎銏ゆ煀瀹ュ洨鏋?
    public static final ForgeConfigSpec.IntValue SOLAR_ENERGY_GENERATION = BUILDER
            .comment("config")
            .defineInRange("solarEnergyGeneration", 10, 1, 500);

    public static final ForgeConfigSpec.DoubleValue AMMO_RECYCLE_CHANCE = BUILDER
            .comment("config")
            .defineInRange("ammoRecycleChance", 0.2, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue REDSTONE_TO_ENERGY_RATIO = BUILDER
            .comment("config")
            .defineInRange("redstoneToEnergyRatio", 2000, 100, 10000);

    public static final ForgeConfigSpec.DoubleValue PHASE_FIELD_RANGE = BUILDER
            .comment("config")
            .defineInRange("phaseFieldRange", 32.0, 1.0, 256.0);
    public static final ForgeConfigSpec.IntValue PHASE_FIELD_ENERGY_PER_TICK = BUILDER
            .comment("config")
            .defineInRange("phaseFieldEnergyPerTick", 100, 1, 100000);
    public static final ForgeConfigSpec.IntValue PHASE_FIELD_EFFECT_DURATION = BUILDER
            .comment("config")
            .defineInRange("phaseFieldEffectDuration", 600, 1, 12000);
    public static final ForgeConfigSpec.DoubleValue RESONANCE_FIELD_RANGE = BUILDER
            .comment("config")
            .defineInRange("resonanceFieldRange", 64.0, 1.0, 256.0);
    public static final ForgeConfigSpec.IntValue RESONANCE_FIELD_ENERGY_PER_TICK = BUILDER
            .comment("config")
            .defineInRange("resonanceFieldEnergyPerTick", 100, 1, 100000);
    public static final ForgeConfigSpec.IntValue RESONANCE_FIELD_EFFECT_DURATION = BUILDER
            .comment("config")
            .defineInRange("resonanceFieldEffectDuration", 1200, 1, 12000);
    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int turretBaseMaxEnergyT1;
    public static int turretBaseMaxEnergyT2;
    public static int turretBaseMaxEnergyT3;
    public static int maxTransferRate;
    public static int turretBaseMaxTransferRateT1;
    public static int turretBaseMaxTransferRateT2;
    public static int turretBaseMaxTransferRateT3;
    public static int turretBaseMaxTransferRateT4;
    public static int turretBaseMaxTransferRateT5;

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
    public static double phaseFieldRange;
    public static int phaseFieldEnergyPerTick;
    public static int phaseFieldEffectDuration;
    public static double resonanceFieldRange;
    public static int resonanceFieldEnergyPerTick;
    public static int resonanceFieldEffectDuration;

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
        turretBaseMaxTransferRateT1 = TURRET_BASE_MAX_TRANSFER_RATE_T1.get();
        turretBaseMaxTransferRateT2 = TURRET_BASE_MAX_TRANSFER_RATE_T2.get();
        turretBaseMaxTransferRateT3 = TURRET_BASE_MAX_TRANSFER_RATE_T3.get();
        turretBaseMaxTransferRateT4 = TURRET_BASE_MAX_TRANSFER_RATE_T4.get();
        turretBaseMaxTransferRateT5 = TURRET_BASE_MAX_TRANSFER_RATE_T5.get();

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
        phaseFieldRange = PHASE_FIELD_RANGE.get();
        phaseFieldEnergyPerTick = PHASE_FIELD_ENERGY_PER_TICK.get();
        phaseFieldEffectDuration = PHASE_FIELD_EFFECT_DURATION.get();
        resonanceFieldRange = RESONANCE_FIELD_RANGE.get();
        resonanceFieldEnergyPerTick = RESONANCE_FIELD_ENERGY_PER_TICK.get();
        resonanceFieldEffectDuration = RESONANCE_FIELD_EFFECT_DURATION.get();

        solarEnergyGeneration = SOLAR_ENERGY_GENERATION.get();
        ammoRecycleChance = AMMO_RECYCLE_CHANCE.get();
        redstoneToEnergyRatio = REDSTONE_TO_ENERGY_RATIO.get();


    }
}
