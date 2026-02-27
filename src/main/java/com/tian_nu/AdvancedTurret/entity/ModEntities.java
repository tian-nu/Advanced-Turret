package com.tian_nu.AdvancedTurret.entity;

import com.tian_nu.AdvancedTurret.TurretMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TurretMod.MOD_ID);

    public static final RegistryObject<EntityType<TurretBulletEntity>> TURRET_BULLET = ENTITIES.register(
            "turret_bullet",
            () -> EntityType.Builder.<TurretBulletEntity>of(
                    TurretBulletEntity::new,
                    MobCategory.MISC
            )
            .sized(0.25F, 0.25F)
            .clientTrackingRange(64)
            .updateInterval(1)
            .fireImmune()
            .build(TurretMod.location("turret_bullet").toString())
    );

    public static final RegistryObject<EntityType<RailgunBulletEntity>> RAILGUN_BULLET = ENTITIES.register(
            "railgun_bullet",
            () -> EntityType.Builder.<RailgunBulletEntity>of(
                    RailgunBulletEntity::new,
                    MobCategory.MISC
            )
            .sized(0.5F, 0.5F)  // 磁轨炮子弹更大
            .clientTrackingRange(64)
            .updateInterval(1)
            .fireImmune()
            .build(TurretMod.location("railgun_bullet").toString())
    );

    public static final RegistryObject<EntityType<RocketEntity>> ROCKET = ENTITIES.register(
            "rocket",
            () -> EntityType.Builder.<RocketEntity>of(
                    RocketEntity::new,
                    MobCategory.MISC
            )
            .sized(0.5F, 0.5F)  // 火箭弹尺寸
            .clientTrackingRange(64)
            .updateInterval(1)
            .fireImmune()
            .build(TurretMod.location("rocket").toString())
    );

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
