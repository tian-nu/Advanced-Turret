package com.tian_nu.AdvancedTurret.items;

import com.tian_nu.AdvancedTurret.TurretMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TurretMod.MOD_ID);

    public static final RegistryObject<Item> TURRET =
            ITEMS.register("turret", () -> new Item(new Item.Properties()));
    
    // 炮塔类型物品
    // 机枪炮塔物品在ModBlocks中注册为BlockItem
    
    public static final RegistryObject<Item> RAILGUN_TURRET =
            ITEMS.register("railgun_turret", () -> new Item(new Item.Properties().stacksTo(1)));
    
    // 创造能量组件
    public static final RegistryObject<Item> CREATIVE_POWER_COMPONENT =
            ITEMS.register("creative_power_component", () -> new Item(new Item.Properties().stacksTo(64)));

    // 智能芯片插件
    public static final RegistryObject<Item> SMART_CHIP =
            ITEMS.register("smart_chip", () -> new SmartChipItem(new Item.Properties().stacksTo(64)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
