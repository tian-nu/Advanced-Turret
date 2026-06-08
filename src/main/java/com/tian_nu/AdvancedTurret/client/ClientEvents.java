package com.tian_nu.AdvancedTurret.client;

import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.blocks.entitys.ModBlockEntities;
import com.tian_nu.AdvancedTurret.client.renderer.GrenadeRenderer;
import com.tian_nu.AdvancedTurret.client.renderer.JunkProjectileRenderer;
import com.tian_nu.AdvancedTurret.client.renderer.MissileRenderer;
import com.tian_nu.AdvancedTurret.client.renderer.RailgunBulletRenderer;
import com.tian_nu.AdvancedTurret.client.renderer.RocketRenderer;
import com.tian_nu.AdvancedTurret.client.renderer.TurretBulletRenderer;
import com.tian_nu.AdvancedTurret.entity.ModEntities;
import com.tian_nu.AdvancedTurret.gui.ModMenuTypes;
import com.tian_nu.AdvancedTurret.gui.TurretFaceConfigScreen;
import com.tian_nu.AdvancedTurret.gui.TurretScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端菜单与渲染器注册。
 */
@Mod.EventBusSubscriber(modid = TurretMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.TURRET_BASE.get(), TurretScreen::new);
            MenuScreens.register(ModMenuTypes.TURRET_FACE_CONFIG.get(), TurretFaceConfigScreen::new);
            com.tian_nu.AdvancedTurret.ConfigManager.loadConfig();
        });
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MACHINE_GUN_TURRET.get(), MachineGunTurretGeoRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RAILGUN_TURRET.get(), RailgunTurretGeoRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ROCKET_TURRET.get(), RocketTurretGeoRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MISSILE_TURRET.get(), MissileTurretGeoRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LASER_TURRET.get(), LaserTurretGeoRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PHASE_FIELD_TURRET.get(), PhaseFieldTurretGeoRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RESONANCE_FIELD_TURRET.get(), ResonanceFieldTurretGeoRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GRENADE_LAUNCHER_TURRET.get(), GrenadeLauncherTurretGeoRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.JUNK_TURRET.get(), JunkTurretGeoRenderer::new);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.TURRET_BULLET.get(), TurretBulletRenderer::new);
        event.registerEntityRenderer(ModEntities.RAILGUN_BULLET.get(), RailgunBulletRenderer::new);
        event.registerEntityRenderer(ModEntities.ROCKET.get(), RocketRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE.get(), MissileRenderer::new);
        event.registerEntityRenderer(ModEntities.GRENADE.get(), GrenadeRenderer::new);
        event.registerEntityRenderer(ModEntities.LAUNCHER_GRENADE.get(), GrenadeRenderer::new);
        event.registerEntityRenderer(ModEntities.JUNK_PROJECTILE.get(), JunkProjectileRenderer::new);
    }
}
