package com.tian_nu.AdvancedTurret.blocks.entitys;

import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.blocks.ModBlocks;
import com.tian_nu.AdvancedTurret.blocks.LaserTurretBlock;
import com.tian_nu.AdvancedTurret.blocks.MissileTurretBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 方块实体类型注册类
 *
 * @author tian_nu
 */
public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TurretMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<TurretBaseBlockEntity>> TURRET_BASE =
            BLOCK_ENTITIES.register("turret_base", () ->
                    BlockEntityType.Builder.of(TurretBaseBlockEntity::new,
                            ModBlocks.TURRET_BASE_T1.get(),
                            ModBlocks.TURRET_BASE_T2.get(),
                            ModBlocks.TURRET_BASE_T3.get(),
                            ModBlocks.TURRET_BASE_T4.get(),
                            ModBlocks.TURRET_BASE_T5.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<MachineGunTurretBlockEntity>> MACHINE_GUN_TURRET =
            BLOCK_ENTITIES.register("machine_gun_turret", () ->
                    BlockEntityType.Builder.of(MachineGunTurretBlockEntity::new,
                            ModBlocks.MACHINE_GUN_TURRET.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<RailgunTurretBlockEntity>> RAILGUN_TURRET =
            BLOCK_ENTITIES.register("railgun_turret", () ->
                    BlockEntityType.Builder.of(RailgunTurretBlockEntity::new,
                            ModBlocks.RAILGUN_TURRET.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<RocketTurretBlockEntity>> ROCKET_TURRET =
            BLOCK_ENTITIES.register("rocket_turret", () ->
                    BlockEntityType.Builder.of(RocketTurretBlockEntity::new,
                            ModBlocks.ROCKET_TURRET.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<MissileTurretBlockEntity>> MISSILE_TURRET =
            BLOCK_ENTITIES.register("missile_turret", () ->
                    BlockEntityType.Builder.of(MissileTurretBlockEntity::new,
                            ModBlocks.MISSILE_TURRET.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<LaserTurretBlockEntity>> LASER_TURRET =
            BLOCK_ENTITIES.register("laser_turret", () ->
                    BlockEntityType.Builder.of(LaserTurretBlockEntity::new,
                            ModBlocks.LASER_TURRET.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
