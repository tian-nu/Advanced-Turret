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

    // 测试物品 - 用于创造模式物品栏
    public static final RegistryObject<Item> TEST_ITEM =
            ITEMS.register("test_item", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> TURRET =
            ITEMS.register("turret", () -> new Item(new Item.Properties()));
    
    // 炮塔类型物品
    // 机枪炮塔物品在ModBlocks中注册为BlockItem
    
    public static final RegistryObject<Item> RAILGUN_TURRET =
            ITEMS.register("railgun_turret", () -> new Item(new Item.Properties().stacksTo(1)));
    
    // 创造能量组件
    public static final RegistryObject<Item> CREATIVE_POWER_COMPONENT =
            ITEMS.register("creative_power_component", () -> new Item(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}