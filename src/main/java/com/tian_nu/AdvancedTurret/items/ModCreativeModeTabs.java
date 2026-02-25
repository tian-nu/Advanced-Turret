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
                        
                        // 炮塔类型
                        output.accept(ModBlocks.MACHINE_GUN_TURRET.get());
                        output.accept(ModItems.RAILGUN_TURRET.get());
                        
                        // 特殊组件
                        output.accept(ModItems.CREATIVE_POWER_COMPONENT.get());
                        output.accept(ModItems.SMART_CHIP.get());
                        
                        // 炮塔基座
                        output.accept(ModBlocks.TURRET_BASE_T1.get());
                        output.accept(ModBlocks.TURRET_BASE_T2.get());
                        output.accept(ModBlocks.TURRET_BASE_T3.get());
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
