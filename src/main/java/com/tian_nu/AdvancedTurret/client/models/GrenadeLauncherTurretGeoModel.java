package com.tian_nu.AdvancedTurret.client.models;

import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.blocks.GrenadeLauncherTurretBlock;
import com.tian_nu.AdvancedTurret.blocks.entitys.GrenadeLauncherTurretBlockEntity;
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
 * 榴弹发射器炮塔GeoModel类
 */
public class GrenadeLauncherTurretGeoModel extends GeoModel<GrenadeLauncherTurretBlockEntity> {

    @Override
    public ResourceLocation getModelResource(GrenadeLauncherTurretBlockEntity animatable) {
        return TurretMod.location("geo/block/machine_gun_turret.geo.json");
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
                Vec3 targetPos = new Vec3(targetX(animatable), targetY(animatable), targetZ(animatable));
                
                Direction direction = animatable.getBlockState().getValue(GrenadeLauncherTurretBlock.FACING);
                
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

    private double targetX(GrenadeLauncherTurretBlockEntity animatable) {
        Double targetX = animatable.getAnimData(GrenadeLauncherTurretBlockEntity.TARGET_POS_X);
        return targetX != null ? targetX : 0;
    }
    
    private double targetY(GrenadeLauncherTurretBlockEntity animatable) {
        Double targetY = animatable.getAnimData(GrenadeLauncherTurretBlockEntity.TARGET_POS_Y);
        return targetY != null ? targetY : 0;
    }
    
    private double targetZ(GrenadeLauncherTurretBlockEntity animatable) {
        Double targetZ = animatable.getAnimData(GrenadeLauncherTurretBlockEntity.TARGET_POS_Z);
        return targetZ != null ? targetZ : 0;
    }
}
