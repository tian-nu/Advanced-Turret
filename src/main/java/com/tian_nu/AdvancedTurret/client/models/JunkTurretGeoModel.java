package com.tian_nu.AdvancedTurret.client.models;

import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.blocks.JunkTurretBlock;
import com.tian_nu.AdvancedTurret.blocks.entitys.JunkTurretBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/**
 * 垃圾炮塔 GeoModel
 */
public class JunkTurretGeoModel extends GeoModel<JunkTurretBlockEntity> {

    @Override
    public ResourceLocation getModelResource(JunkTurretBlockEntity animatable) {
        return TurretMod.location("geo/block/junk_turret.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(JunkTurretBlockEntity animatable) {
        return TurretMod.location("textures/block/junk_turret.png");
    }

    @Override
    public ResourceLocation getAnimationResource(JunkTurretBlockEntity animatable) {
        return TurretMod.location("animations/block/machine_gun_turret.animation.json");
    }

    @Override
    public void setCustomAnimations(JunkTurretBlockEntity animatable, long instanceId, AnimationState<JunkTurretBlockEntity> animationState) {
        CoreGeoBone turret = getAnimationProcessor().getBone("turret");

        if (turret != null) {
            if (hasTarget(animatable)) {
                Direction direction = animatable.getBlockState().getValue(JunkTurretBlock.FACING);

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
        return switch (direction) {
            case NORTH -> new Quaternionf().rotationX((float) (-Math.PI / 2));
            case EAST -> new Quaternionf().rotationZ((float) (-Math.PI / 2));
            case SOUTH -> new Quaternionf().rotationX((float) (Math.PI / 2));
            case WEST -> new Quaternionf().rotationZ((float) (Math.PI / 2));
            case UP -> new Quaternionf().rotationZ((float) Math.PI);
            case DOWN -> new Quaternionf();
        };
    }

    private float lerp(float start, float end) {
        return Mth.rotLerp(0.1F, start * Mth.RAD_TO_DEG, end * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
    }

    private boolean hasTarget(JunkTurretBlockEntity animatable) {
        Boolean hasTarget = animatable.getAnimData(JunkTurretBlockEntity.HAS_TARGET);
        return Boolean.TRUE.equals(hasTarget);
    }

    private double aimDirX(JunkTurretBlockEntity animatable) {
        Double aimDirX = animatable.getAnimData(JunkTurretBlockEntity.AIM_DIR_X);
        return aimDirX != null ? aimDirX : 0;
    }

    private double aimDirY(JunkTurretBlockEntity animatable) {
        Double aimDirY = animatable.getAnimData(JunkTurretBlockEntity.AIM_DIR_Y);
        return aimDirY != null ? aimDirY : 0;
    }

    private double aimDirZ(JunkTurretBlockEntity animatable) {
        Double aimDirZ = animatable.getAnimData(JunkTurretBlockEntity.AIM_DIR_Z);
        return aimDirZ != null ? aimDirZ : 0;
    }
}
