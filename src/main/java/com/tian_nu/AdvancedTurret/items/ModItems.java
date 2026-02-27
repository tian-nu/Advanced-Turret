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
    
    // 弹药物品
    public static final RegistryObject<Item> MACHINE_GUN_BULLET =
            ITEMS.register("machine_gun_bullet", () -> new Item(new Item.Properties().stacksTo(64)));
    
    // 插件物品
    public static final RegistryObject<Item> CREATIVE_POWER_COMPONENT =
            ITEMS.register("creative_power_component", () -> new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> SOLAR_PLUGIN =
            ITEMS.register("solar_plugin", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> AMMO_RECYCLING_PLUGIN =
            ITEMS.register("ammo_recycling_plugin", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> REDSTONE_CONVERSION_PLUGIN =
            ITEMS.register("redstone_conversion_plugin", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DESTRUCTION_PLUGIN =
            ITEMS.register("destruction_plugin", () -> new Item(new Item.Properties().stacksTo(1)));

    // 智能芯片插件
    public static final RegistryObject<Item> SMART_CHIP =
            ITEMS.register("smart_chip", () -> new SmartChipItem(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ATTACK_BOOST_COMPONENT =
            ITEMS.register("attack_boost_component", () -> new Item(new Item.Properties().stacksTo(4)));
    public static final RegistryObject<Item> ENERGY_EFFICIENCY_COMPONENT =
            ITEMS.register("energy_efficiency_component", () -> new Item(new Item.Properties().stacksTo(4)));
    public static final RegistryObject<Item> RANGE_COMPONENT =
            ITEMS.register("range_component", () -> new Item(new Item.Properties().stacksTo(4)));
    public static final RegistryObject<Item> ACCURACY_COMPONENT =
            ITEMS.register("accuracy_component", () -> new Item(new Item.Properties().stacksTo(4)));
    public static final RegistryObject<Item> FIRE_RATE_COMPONENT =
            ITEMS.register("fire_rate_component", () -> new Item(new Item.Properties().stacksTo(4)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
