package com.tian_nu.AdvancedTurret;

import com.mojang.logging.LogUtils;
import com.tian_nu.AdvancedTurret.blocks.ModBlocks;
import com.tian_nu.AdvancedTurret.blocks.entitys.MachineGunTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.RailgunTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.RocketTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.MissileTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.ModBlockEntities;
import com.tian_nu.AdvancedTurret.entity.ModEntities;
import com.tian_nu.AdvancedTurret.gui.ModMenuTypes;
import com.tian_nu.AdvancedTurret.items.ModCreativeModeTabs;
import com.tian_nu.AdvancedTurret.items.ModItems;
import com.tian_nu.AdvancedTurret.network.ModNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
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
 * 炮塔模组主类
 * 
 * @author tian_nu
 */
@Mod(TurretMod.MOD_ID)
public class TurretMod {
    
    public static final String MOD_ID = "advanced_turret";
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * 创建资源定位器的便捷方法
     */
    public static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
    
    public TurretMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        
        // 注册延迟注册器
        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModEntities.register(modEventBus);
        
        // 注册事件监听器
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModCreativeModeTabs::addCreative);
        
        // 注册Forge事件总线
        MinecraftForge.EVENT_BUS.register(this);
        
        // 注册配置
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        
        // 注册GeckoLib数据票
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
    }
    
    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("炮塔模组初始化中...");
        
        event.enqueueWork(() -> {
            ModNetwork.register();
            LOGGER.info("网络系统注册完成");
        });
        
        LOGGER.info("炮塔模组初始化完成");
    }
    
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("炮塔模组服务器启动");
    }
    

}
