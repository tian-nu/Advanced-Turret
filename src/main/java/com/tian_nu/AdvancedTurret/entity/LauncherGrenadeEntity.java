package com.tian_nu.AdvancedTurret.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class LauncherGrenadeEntity extends GrenadeEntity {
    private static final double LAUNCHER_GRAVITY = 0.022D;

    public LauncherGrenadeEntity(EntityType<? extends LauncherGrenadeEntity> type, Level level) {
        super(type, level);
        configureLauncherBehavior();
    }

    public LauncherGrenadeEntity(Level level, double x, double y, double z, float damage) {
        super(ModEntities.LAUNCHER_GRENADE.get(), level, x, y, z, damage);
        configureLauncherBehavior();
    }

    private void configureLauncherBehavior() {
        setBlastKnockback(1.45D, 0.9D, true);
        setSelfBlastKnockbackMultiplier(2.8D);
        setSelfDamageDisabled(true);
        setFallProtectionTicks(20);
    }

    @Override
    protected double getGravityStrength() {
        return LAUNCHER_GRAVITY;
    }
}
