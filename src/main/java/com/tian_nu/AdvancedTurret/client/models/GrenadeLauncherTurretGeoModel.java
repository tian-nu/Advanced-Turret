package com.tian_nu.AdvancedTurret.client.models;

import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.blocks.GrenadeLauncherTurretBlock;
import com.tian_nu.AdvancedTurret.blocks.entitys.GrenadeLauncherTurretBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/**
 * 榴弹发射器炮塔GeoModel类
 */
public class GrenadeLauncherTurretGeoModel extends GeoModel<GrenadeLauncherTurretBlockEntity> {

    @Override
    public ResourceLocation getModelResource(GrenadeLauncherTurretBlockEntity animatable) {
        return TurretMod.location("geo/block/grenade_launcher_turret.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GrenadeLauncherTurretBlockEntity animatable) {
        return TurretMod.location("textures/block/grenade_launcher_turret.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GrenadeLauncherTurretBlockEntity animatable) {
        return TurretMod.location("animations/block/machine_gun_turret.animation.json");
    }

    @Override
    public void setCustomAnimations(GrenadeLauncherTurretBlockEntity animatable, long instanceId, AnimationState<GrenadeLauncherTurretBlockEntity> animationState) {
        CoreGeoBone turret = getAnimationProcessor().getBone("turret");

        if (turret != null) {
            if (hasTarget(animatable)) {
                Direction direction = animatable.getBlockState().getValue(GrenadeLauncherTurretBlock.FACING);

                // 使用服务端同步的抛物线初速度方向，保证炮口朝向和真实弹道一致
                Vector3f worldAimDirection = new Vector3f(
                    (float) aimDirX(animatable),
                    (float) aimDirY(animatable),
                    (float) aimDirZ(animatable)
                );

                if (worldAimDirection.lengthSquared() < 1.0E-6f) {
                    worldAimDirection.set(0.0f, 0.0f, 1.0f);
                } else {
                    worldAimDirection.normalize();
                }

                Vector3f localAimDirection = getTransform(direction).transform(worldAimDirection);
                double horizontalDist = Math.sqrt(localAimDirection.x * localAimDirection.x + localAimDirection.z * localAimDirection.z);

                float yRot = (float) -Math.atan2(localAimDirection.x, localAimDirection.z);

                if (direction == Direction.UP || direction == Direction.EAST || direction == Direction.WEST) {
                    yRot += (float) Math.PI;
                }

                float xRot = (float) -Math.atan2(localAimDirection.y, horizontalDist);

                yRot = lerp(animatable.yRot0, yRot);
                xRot = lerp(animatable.xRot0, xRot);

                animatable.xRot0 = xRot;
                animatable.yRot0 = yRot;

                turret.setRotX(xRot);
                turret.setRotY(yRot);
            } else {
                float xRot = lerp(animatable.xRot0, 0);
                float yRot = lerp(animatable.yRot0, 0);
                animatable.xRot0 = xRot;
                animatable.yRot0 = yRot;
                turret.setRotX(xRot);
                turret.setRotY(yRot);
            }
        }
    }

    private Quaternionf getTransform(Direction direction) {
        switch (direction) {
            case NORTH -> { return new Quaternionf().rotationX((float) (-Math.PI / 2)); }
            case EAST -> { return new Quaternionf().rotationZ((float) (-Math.PI / 2)); }
            case SOUTH -> { return new Quaternionf().rotationX((float) (Math.PI / 2)); }
            case WEST -> { return new Quaternionf().rotationZ((float) (Math.PI / 2)); }
            case UP -> { return new Quaternionf().rotationZ((float) Math.PI); }
            case DOWN -> { return new Quaternionf(); }
        }
        return new Quaternionf();
    }

    private float lerp(float start, float end) {
        return Mth.rotLerp(0.1F, start * Mth.RAD_TO_DEG, end * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
    }

    private boolean hasTarget(GrenadeLauncherTurretBlockEntity animatable) {
        Boolean hasTarget = animatable.getAnimData(GrenadeLauncherTurretBlockEntity.HAS_TARGET);
        return Boolean.TRUE.equals(hasTarget);
    }

    private double aimDirX(GrenadeLauncherTurretBlockEntity animatable) {
        Double aimDirX = animatable.getAnimData(GrenadeLauncherTurretBlockEntity.AIM_DIR_X);
        return aimDirX != null ? aimDirX : 0;
    }

    private double aimDirY(GrenadeLauncherTurretBlockEntity animatable) {
        Double aimDirY = animatable.getAnimData(GrenadeLauncherTurretBlockEntity.AIM_DIR_Y);
        return aimDirY != null ? aimDirY : 0;
    }

    private double aimDirZ(GrenadeLauncherTurretBlockEntity animatable) {
        Double aimDirZ = animatable.getAnimData(GrenadeLauncherTurretBlockEntity.AIM_DIR_Z);
        return aimDirZ != null ? aimDirZ : 0;
    }
}