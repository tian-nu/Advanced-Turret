package com.tian_nu.AdvancedTurret.gui;

import com.tian_nu.AdvancedTurret.TurretMod;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 菜单类型注册类
 * 
 * @author tian_nu
 */
public class ModMenuTypes {
    
    public static final DeferredRegister<MenuType<?>> MENUS = 
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, TurretMod.MOD_ID);
    
    public static final RegistryObject<MenuType<TurretMenu>> TURRET_BASE = 
            MENUS.register("turret_base", () -> IForgeMenuType.create(TurretMenu::new));

    public static final RegistryObject<MenuType<TurretFaceConfigMenu>> TURRET_FACE_CONFIG =
            MENUS.register("turret_face_config", () -> IForgeMenuType.create(TurretFaceConfigMenu::new));
    
    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
