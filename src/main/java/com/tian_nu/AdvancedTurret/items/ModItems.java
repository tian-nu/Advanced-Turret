package com.tian_nu.AdvancedTurret.items;

import com.tian_nu.AdvancedTurret.TurretMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TurretMod.MOD_ID);

    public static final RegistryObject<Item> TURRET =
            ITEMS.register("turret", () -> new Item(new Item.Properties()));

    // 炮塔方块物品在 ModBlocks 中作为 BlockItem 注册。

    public static final RegistryObject<Item> MACHINE_GUN_BULLET =
            ITEMS.register("machine_gun_bullet", () -> describedItem("item.advanced_turret.machine_gun_bullet.tooltip"));
    public static final RegistryObject<Item> RAILGUN_BULLET =
            ITEMS.register("railgun_bullet", () -> describedItem("item.advanced_turret.railgun_bullet.tooltip"));
    public static final RegistryObject<Item> ROCKET =
            ITEMS.register("rocket", () -> describedItem("item.advanced_turret.rocket.tooltip"));
    public static final RegistryObject<Item> MISSILE =
            ITEMS.register("missile", () -> describedItem("item.advanced_turret.missile.tooltip"));
    public static final RegistryObject<Item> GRENADE =
            ITEMS.register("grenade", () -> describedItem("item.advanced_turret.grenade.tooltip"));

    public static final RegistryObject<Item> CREATIVE_POWER_COMPONENT =
            ITEMS.register("creative_power_component", () -> describedItem("item.advanced_turret.creative_power_component.tooltip"));
    public static final RegistryObject<Item> SOLAR_PLUGIN =
            ITEMS.register("solar_plugin", () -> describedItem("item.advanced_turret.solar_plugin.tooltip"));
    public static final RegistryObject<Item> AMMO_RECYCLING_PLUGIN =
            ITEMS.register("ammo_recycling_plugin", () -> describedItem("item.advanced_turret.ammo_recycling_plugin.tooltip"));
    public static final RegistryObject<Item> REDSTONE_CONVERSION_PLUGIN =
            ITEMS.register("redstone_conversion_plugin", () -> describedItem("item.advanced_turret.redstone_conversion_plugin.tooltip"));
    public static final RegistryObject<Item> DESTRUCTION_PLUGIN =
            ITEMS.register("destruction_plugin", () -> describedItem("item.advanced_turret.destruction_plugin.tooltip"));

    public static final RegistryObject<Item> SMART_CHIP =
            ITEMS.register("smart_chip", () -> new SmartChipItem(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ATTACK_BOOST_COMPONENT =
            ITEMS.register("attack_boost_component", () -> describedItem("item.advanced_turret.attack_boost_component.tooltip"));
    public static final RegistryObject<Item> ENERGY_EFFICIENCY_COMPONENT =
            ITEMS.register("energy_efficiency_component", () -> describedItem("item.advanced_turret.energy_efficiency_component.tooltip"));
    public static final RegistryObject<Item> RANGE_COMPONENT =
            ITEMS.register("range_component", () -> describedItem("item.advanced_turret.range_component.tooltip"));
    public static final RegistryObject<Item> ACCURACY_COMPONENT =
            ITEMS.register("accuracy_component", () -> describedItem("item.advanced_turret.accuracy_component.tooltip"));
    public static final RegistryObject<Item> FIRE_RATE_COMPONENT =
            ITEMS.register("fire_rate_component", () -> describedItem("item.advanced_turret.fire_rate_component.tooltip"));

    private static Item describedItem(String... tooltipKeys) {
        return new Item(new Item.Properties().stacksTo(64)) {
            @Override
            public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                for (String tooltipKey : tooltipKeys) {
                    tooltip.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
                }
                super.appendHoverText(stack, level, tooltip, flag);
            }
        };
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}