package com.tian_nu.AdvancedTurret.client.models;

import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.blocks.RocketTurretBlock;
import com.tian_nu.AdvancedTurret.blocks.entitys.RocketTurretBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/**
 * 火箭炮塔GeoModel类
 * 
 * <p>使用GeckoLib渲染炮塔模型，处理瞄准动画</p>
 */
public class RocketTurretGeoModel extends GeoModel<RocketTurretBlockEntity> {

    @Override
    public ResourceLocation getModelResource(RocketTurretBlockEntity animatable) {
        return TurretMod.location("geo/block/rocket_turret.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RocketTurretBlockEntity animatable) {
        return TurretMod.location("textures/block/rocket_turret.png");
    }

    @Override
    public ResourceLocation getAnimationResource(RocketTurretBlockEntity animatable) {
        return TurretMod.location("animations/block/rocket_turret.animation.json");
    }

    @Override
    public void setCustomAnimations(RocketTurretBlockEntity animatable, long instanceId, AnimationState<RocketTurretBlockEntity> animationState) {
        CoreGeoBone turret = getAnimationProcessor().getBone("turret");

        if (turret != null) {
            if (hasTarget(animatable)) {
                Vec3 targetPos = new Vec3(targetX(animatable), targetY(animatable), targetZ(animatable));
                Direction direction = animatable.getBlockState().getValue(RocketTurretBlock.FACING);
                Vec3 center = animatable.getBlockPos().getCenter();

                Vector3f deltaPos = getTransform(direction).transform(
                        new Vec3(targetPos.x - center.x, targetPos.y - center.y, targetPos.z - center.z).toVector3f()
                );

                double horizontalDist = Math.sqrt(deltaPos.x * deltaPos.x + deltaPos.z * deltaPos.z);
                float yRot = (float) -Math.atan2(deltaPos.x, deltaPos.z);

                if (direction == Direction.UP || direction == Direction.EAST || direction == Direction.WEST) {
                    yRot += (float) Math.PI;
                }

                float xRot = (float) -Math.atan2(deltaPos.y, horizontalDist);

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

    private boolean hasTarget(RocketTurretBlockEntity animatable) {
        Boolean hasTarget = animatable.getAnimData(RocketTurretBlockEntity.HAS_TARGET);
        return Boolean.TRUE.equals(hasTarget);
    }

    private double targetX(RocketTurretBlockEntity animatable) {
        Double targetX = animatable.getAnimData(RocketTurretBlockEntity.TARGET_POS_X);
        return targetX != null ? targetX : 0;
    }

    private double targetY(RocketTurretBlockEntity animatable) {
        Double targetY = animatable.getAnimData(RocketTurretBlockEntity.TARGET_POS_Y);
        return targetY != null ? targetY : 0;
    }

    private double targetZ(RocketTurretBlockEntity animatable) {
        Double targetZ = animatable.getAnimData(RocketTurretBlockEntity.TARGET_POS_Z);
        return targetZ != null ? targetZ : 0;
    }
}
