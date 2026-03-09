package com.tian_nu.AdvancedTurret.blocks;

import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.items.ModItems;
import com.tian_nu.AdvancedTurret.blocks.entitys.LaserTurretBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, TurretMod.MOD_ID);

    // T1炮塔基座：石头材质，铁镐等级挖掘
    public static final RegistryObject<Block> TURRET_BASE_T1 =
            registerBlock("turret_base_t1", () -> new TurretBaseBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(1.5f, 6.0f)));
    
    // T2炮塔基座：铁材质，铁镐等级挖掘
    public static final RegistryObject<Block> TURRET_BASE_T2 =
            registerBlock("turret_base_t2", () -> new TurretBaseBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(2.0f, 8.0f)));
    
    // T3炮塔基座：金材质，铁镐等级挖掘
    public static final RegistryObject<Block> TURRET_BASE_T3 =
            registerBlock("turret_base_t3", () -> new TurretBaseBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .requiresCorrectToolForDrops()
                    .strength(2.5f, 10.0f)));

    // T4炮塔基座：钻石材质，铁镐等级挖掘
    public static final RegistryObject<Block> TURRET_BASE_T4 =
            registerBlock("turret_base_t4", () -> new TurretBaseBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIAMOND)
                    .requiresCorrectToolForDrops()
                    .strength(3.0f, 12.0f)));

    // T5炮塔基座：绿宝石材质，铁镐等级挖掘
    public static final RegistryObject<Block> TURRET_BASE_T5 =
            registerBlock("turret_base_t5", () -> new TurretBaseBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.EMERALD)
                    .requiresCorrectToolForDrops()
                    .strength(3.5f, 14.0f)));
    
    // 机枪炮塔
    public static final RegistryObject<Block> MACHINE_GUN_TURRET =
            registerBlock("machine_gun_turret", () -> new MachineGunTurretBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .noOcclusion()
                    .strength(1.0f, 6.0f)));

    public static final RegistryObject<Block> RAILGUN_TURRET =
            registerBlock("railgun_turret", () -> new RailgunTurretBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .noOcclusion()
                    .strength(1.0f, 6.0f)));

    // 火箭炮塔
    public static final RegistryObject<Block> ROCKET_TURRET =
            registerBlock("rocket_turret", () -> new RocketTurretBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .noOcclusion()
                    .strength(1.0f, 6.0f)));

    // 导弹炮塔
    public static final RegistryObject<Block> MISSILE_TURRET =
            registerBlock("missile_turret", () -> new MissileTurretBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .noOcclusion()
                    .strength(1.0f, 6.0f)));

    // 激光炮塔
    public static final RegistryObject<Block> LASER_TURRET =
            registerBlock("laser_turret", () -> new LaserTurretBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .noOcclusion()
                    .strength(1.0f, 6.0f)));
    
    // 榴弹发射器炮塔
    public static final RegistryObject<Block> GRENADE_LAUNCHER_TURRET =
            registerBlock("grenade_launcher_turret", () -> new GrenadeLauncherTurretBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .noOcclusion()
                    .strength(1.0f, 6.0f)));
    
    // 垃圾炮塔
    public static final RegistryObject<Block> JUNK_TURRET =
            registerBlock("junk_turret", () -> new JunkTurretBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .noOcclusion()
                    .strength(1.0f, 6.0f)));

    private static <T extends Block> void registerBlockItems(String name, RegistryObject<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> blocks = BLOCKS.register(name, block);
        registerBlockItems(name, blocks);
        return blocks;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
