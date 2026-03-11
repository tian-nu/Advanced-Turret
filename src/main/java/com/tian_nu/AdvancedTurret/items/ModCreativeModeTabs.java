package com.tian_nu.AdvancedTurret.items;

import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.blocks.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 创造模式标签页注册类
 * 
 * @author tian_nu
 */
public class ModCreativeModeTabs {
    
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TurretMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> TURRET_TAB =
            CREATIVE_MODE_TABS.register("turret_tab", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.TURRET.get()))
                    .title(Component.translatable("itemGroup.turret_tab"))
                    .displayItems((params, output) -> {
                        // 基础物品
                        output.accept(ModItems.TURRET.get());
                        
                        // 弹药
                        output.accept(ModItems.MACHINE_GUN_BULLET.get());
                        output.accept(ModItems.RAILGUN_BULLET.get());
                        output.accept(ModItems.ROCKET.get());
                        output.accept(ModItems.MISSILE.get());
                        output.accept(ModItems.GRENADE.get());


                        // 炮塔类型
                        output.accept(ModBlocks.MACHINE_GUN_TURRET.get());
                        output.accept(ModBlocks.RAILGUN_TURRET.get());
                        output.accept(ModBlocks.ROCKET_TURRET.get());
                        output.accept(ModBlocks.MISSILE_TURRET.get());
                        output.accept(ModBlocks.LASER_TURRET.get());
                        output.accept(ModBlocks.GRENADE_LAUNCHER_TURRET.get());
                        output.accept(ModBlocks.JUNK_TURRET.get());
                        
                        // 特殊组件
                        output.accept(ModItems.CREATIVE_POWER_COMPONENT.get());
                        output.accept(ModItems.SMART_CHIP.get());
                        output.accept(ModItems.SOLAR_PLUGIN.get());
                        output.accept(ModItems.AMMO_RECYCLING_PLUGIN.get());
                        output.accept(ModItems.REDSTONE_CONVERSION_PLUGIN.get());
                        output.accept(ModItems.DESTRUCTION_PLUGIN.get());
                        output.accept(ModItems.ATTACK_BOOST_COMPONENT.get());
                        output.accept(ModItems.ENERGY_EFFICIENCY_COMPONENT.get());
                        output.accept(ModItems.RANGE_COMPONENT.get());
                        output.accept(ModItems.ACCURACY_COMPONENT.get());
                        output.accept(ModItems.FIRE_RATE_COMPONENT.get());
                        
                        // 炮塔基座
                        output.accept(ModBlocks.TURRET_BASE_T1.get());
                        output.accept(ModBlocks.TURRET_BASE_T2.get());
                        output.accept(ModBlocks.TURRET_BASE_T3.get());
                        output.accept(ModBlocks.TURRET_BASE_T4.get());
                        output.accept(ModBlocks.TURRET_BASE_T5.get());
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
    
    /**
     * 添加物品到创造模式标签页
     */
    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        // 可以在这里添加物品到其他原版标签页
    }
}
