package com.tian_nu.AdvancedTurret.client;

import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.blocks.entitys.ModBlockEntities;
import com.tian_nu.AdvancedTurret.gui.ModMenuTypes;
import com.tian_nu.AdvancedTurret.gui.TurretScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端事件处理器
 *
 * <p>负责注册客户端特定的功能，如GUI屏幕和渲染器</p>
 * <p>使用GeckoLib渲染炮塔模型</p>
 *
 * @author tian_nu
 */
@Mod.EventBusSubscriber(modid = TurretMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.TURRET_BASE.get(), TurretScreen::new);
            
            com.tian_nu.AdvancedTurret.ConfigManager.loadConfig();
        });
    }
    
    /**
     * 注册方块实体渲染器
     */
    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
            ModBlockEntities.MACHINE_GUN_TURRET.get(), 
            MachineGunTurretGeoRenderer::new
        );
    }
}
