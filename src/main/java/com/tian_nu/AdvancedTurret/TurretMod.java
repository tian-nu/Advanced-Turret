package com.tian_nu.AdvancedTurret;

import com.mojang.logging.LogUtils;
import com.tian_nu.AdvancedTurret.blocks.ModBlocks;
import com.tian_nu.AdvancedTurret.blocks.entitys.GrenadeLauncherTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.JunkTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.LaserTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.MachineGunTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.MissileTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.ModBlockEntities;
import com.tian_nu.AdvancedTurret.blocks.entitys.PhaseFieldTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.RailgunTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.ResonanceFieldTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.RocketTurretBlockEntity;
import com.tian_nu.AdvancedTurret.entity.GrenadeEntity;
import com.tian_nu.AdvancedTurret.entity.ModEntities;
import com.tian_nu.AdvancedTurret.gui.ModMenuTypes;
import com.tian_nu.AdvancedTurret.items.ModCreativeModeTabs;
import com.tian_nu.AdvancedTurret.items.ModItems;
import com.tian_nu.AdvancedTurret.network.ModNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import software.bernie.geckolib.network.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 炮塔模组的主入口点。
 */
@Mod(TurretMod.MOD_ID)
public class TurretMod {

    public static final String MOD_ID = "advanced_turret";
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 创建此模组 ID 下的命名空间资源位置。
     */
    public static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public TurretMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModEntities.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModCreativeModeTabs::addCreative);

        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        registerDataTickets();
    }

    private void registerDataTickets() {
        MachineGunTurretBlockEntity.HAS_TARGET = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofBoolean(location("has_target")));
        MachineGunTurretBlockEntity.TARGET_POS_X = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("target_pos_x")));
        MachineGunTurretBlockEntity.TARGET_POS_Y = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("target_pos_y")));
        MachineGunTurretBlockEntity.TARGET_POS_Z = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("target_pos_z")));

        RailgunTurretBlockEntity.HAS_TARGET = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofBoolean(location("railgun_has_target")));
        RailgunTurretBlockEntity.TARGET_POS_X = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("railgun_target_pos_x")));
        RailgunTurretBlockEntity.TARGET_POS_Y = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("railgun_target_pos_y")));
        RailgunTurretBlockEntity.TARGET_POS_Z = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("railgun_target_pos_z")));

        RocketTurretBlockEntity.HAS_TARGET = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofBoolean(location("rocket_has_target")));
        RocketTurretBlockEntity.TARGET_POS_X = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("rocket_target_pos_x")));
        RocketTurretBlockEntity.TARGET_POS_Y = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("rocket_target_pos_y")));
        RocketTurretBlockEntity.TARGET_POS_Z = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("rocket_target_pos_z")));

        MissileTurretBlockEntity.HAS_TARGET = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofBoolean(location("missile_has_target")));
        MissileTurretBlockEntity.TARGET_POS_X = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("missile_target_pos_x")));
        MissileTurretBlockEntity.TARGET_POS_Y = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("missile_target_pos_y")));
        MissileTurretBlockEntity.TARGET_POS_Z = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("missile_target_pos_z")));

        LaserTurretBlockEntity.HAS_TARGET = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofBoolean(location("laser_has_target")));
        LaserTurretBlockEntity.TARGET_POS_X = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("laser_target_pos_x")));
        LaserTurretBlockEntity.TARGET_POS_Y = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("laser_target_pos_y")));
        LaserTurretBlockEntity.TARGET_POS_Z = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("laser_target_pos_z")));
        LaserTurretBlockEntity.BEAM_ACTIVE = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofBoolean(location("laser_beam_active")));
        LaserTurretBlockEntity.FIRE_RATE_COUNT = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofInt(location("laser_fire_rate_count")));

        GrenadeLauncherTurretBlockEntity.HAS_TARGET = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofBoolean(location("grenade_has_target")));
        GrenadeLauncherTurretBlockEntity.TARGET_POS_X = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("grenade_target_pos_x")));
        GrenadeLauncherTurretBlockEntity.TARGET_POS_Y = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("grenade_target_pos_y")));
        GrenadeLauncherTurretBlockEntity.TARGET_POS_Z = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("grenade_target_pos_z")));
        GrenadeLauncherTurretBlockEntity.AIM_DIR_X = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("grenade_aim_dir_x")));
        GrenadeLauncherTurretBlockEntity.AIM_DIR_Y = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("grenade_aim_dir_y")));
        GrenadeLauncherTurretBlockEntity.AIM_DIR_Z = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("grenade_aim_dir_z")));

        JunkTurretBlockEntity.HAS_TARGET = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofBoolean(location("junk_has_target")));
        JunkTurretBlockEntity.TARGET_POS_X = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("junk_target_pos_x")));
        JunkTurretBlockEntity.TARGET_POS_Y = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("junk_target_pos_y")));
        JunkTurretBlockEntity.TARGET_POS_Z = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("junk_target_pos_z")));
        JunkTurretBlockEntity.AIM_DIR_X = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("junk_aim_dir_x")));
        JunkTurretBlockEntity.AIM_DIR_Y = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("junk_aim_dir_y")));
        JunkTurretBlockEntity.AIM_DIR_Z = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofDouble(location("junk_aim_dir_z")));

        PhaseFieldTurretBlockEntity.WORKING_ACTIVE = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofBoolean(location("phase_field_working")));
        ResonanceFieldTurretBlockEntity.WORKING_ACTIVE = GeckoLibUtil.addDataTicket(
                SerializableDataTicket.ofBoolean(location("resonance_field_working")));
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Initializing Advanced Turret...");

        event.enqueueWork(() -> {
            ModNetwork.register();
            LOGGER.info("Network channel registered");
        });

        LOGGER.info("Advanced Turret initialization complete");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Advanced Turret server starting");
    }

    @SubscribeEvent
    public void onLivingFall(LivingFallEvent event) {
        if (event.getEntity().getPersistentData().getBoolean(GrenadeEntity.NEXT_FALL_DAMAGE_IMMUNE_TAG)) {
            event.setCanceled(true);
            event.getEntity().fallDistance = 0.0F;
            event.getEntity().getPersistentData().remove(GrenadeEntity.NEXT_FALL_DAMAGE_IMMUNE_TAG);
        }
    }
}
