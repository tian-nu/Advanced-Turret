package com.tian_nu.AdvancedTurret.client.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.blocks.LaserTurretBlock;
import com.tian_nu.AdvancedTurret.blocks.entitys.LaserTurretBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/**
 * 激光炮塔GeoModel类
 */
public class LaserTurretGeoModel extends GeoModel<LaserTurretBlockEntity> {

    /** 激光边长（方柱体边长） */
    private static final float LASER_SIZE = 0.06F;

    @Override
    public ResourceLocation getModelResource(LaserTurretBlockEntity animatable) {
        return TurretMod.location("geo/block/laser_turret.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LaserTurretBlockEntity animatable) {
        return TurretMod.location("textures/block/laser_turret.png");
    }

    @Override
    public ResourceLocation getAnimationResource(LaserTurretBlockEntity animatable) {
        return TurretMod.location("animations/block/laser_turret.animation.json");
    }

    @Override
    public void setCustomAnimations(LaserTurretBlockEntity animatable, long instanceId, AnimationState<LaserTurretBlockEntity> animationState) {
        CoreGeoBone turret = getAnimationProcessor().getBone("turret");

        if (turret != null) {
            if (hasTarget(animatable)) {
                Vec3 targetPos = new Vec3(targetX(animatable), targetY(animatable), targetZ(animatable));
                Direction direction = animatable.getBlockState().getValue(LaserTurretBlock.FACING);
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

    /**
     * 渲染激光光束（使用局部坐标）
     */
    public void renderLaserBeamLocal(LaserTurretBlockEntity animatable, PoseStack poseStack, MultiBufferSource bufferSource, float partialTick, Vec3 startLocal, BlockPos turretPos) {
        Boolean beamActive = animatable.getAnimData(LaserTurretBlockEntity.BEAM_ACTIVE);
        if (!Boolean.TRUE.equals(beamActive)) return;
        if (!hasTarget(animatable)) return;

        // 获取终点（转换为局部坐标）
        Vec3 endWorld = new Vec3(targetX(animatable), targetY(animatable), targetZ(animatable));
        Vec3 endLocal = new Vec3(endWorld.x - turretPos.getX(), endWorld.y - turretPos.getY(), endWorld.z - turretPos.getZ());

        // 计算透明度（射速加成影响）
        float alpha = 0.4F;
        Integer fireRateCount = animatable.getAnimData(LaserTurretBlockEntity.FIRE_RATE_COUNT);
        if (fireRateCount != null && fireRateCount > 0) {
            alpha = Math.min(1.0F, alpha + fireRateCount * 0.06F);
        }

        renderBeam(poseStack, bufferSource, startLocal, endLocal, LASER_SIZE, alpha);
    }

    /**
     * 渲染激光光束（使用世界坐标）
     * 在 BlockEntityRenderer 中调用，poseStack 已经被 super.render() 恢复
     */
    public void renderLaserBeamWorld(LaserTurretBlockEntity animatable, PoseStack poseStack, MultiBufferSource bufferSource, BlockPos turretPos) {
        Boolean beamActive = animatable.getAnimData(LaserTurretBlockEntity.BEAM_ACTIVE);
        if (!Boolean.TRUE.equals(beamActive)) return;
        if (!hasTarget(animatable)) return;

        // 获取起点和终点（世界坐标）
        Vec3 startWorld = new Vec3(turretPos.getX() + 0.5, turretPos.getY() + 0.5, turretPos.getZ() + 0.5);
        Vec3 endWorld = new Vec3(targetX(animatable), targetY(animatable), targetZ(animatable));

        // 计算透明度（射速加成影响）
        float alpha = 0.4F;
        Integer fireRateCount = animatable.getAnimData(LaserTurretBlockEntity.FIRE_RATE_COUNT);
        if (fireRateCount != null && fireRateCount > 0) {
            alpha = Math.min(1.0F, alpha + fireRateCount * 0.06F);
        }

        // 转换为相对于方块位置的局部坐标
        Vec3 startLocal = new Vec3(0.5, 0.5, 0.5);
        Vec3 endLocal = new Vec3(endWorld.x - turretPos.getX(), endWorld.y - turretPos.getY(), endWorld.z - turretPos.getZ());

        renderBeam(poseStack, bufferSource, startLocal, endLocal, LASER_SIZE, alpha);
    }

    /**
     * 渲染实心方柱形激光
     */
    private void renderBeam(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 start, Vec3 end, float size, float alpha) {
        Vec3 direction = end.subtract(start);
        double length = direction.length();
        if (length < 0.001) return;

        // 红色
        float r = 1.0F, g = 0.0F, b = 0.0F;
        float hs = size / 2.0F; // 半边长

        // 直接绘制从起点到终点的线条盒子
        // 使用 LevelRenderer 的渲染方法
        net.minecraft.client.renderer.LevelRenderer.renderLineBox(
            poseStack,
            bufferSource.getBuffer(RenderType.LINES),
            start.x - hs, start.y - hs, start.z - hs,
            end.x + hs, end.y + hs, end.z + hs,
            r, g, b, alpha
        );
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

    private boolean hasTarget(LaserTurretBlockEntity animatable) {
        Boolean hasTarget = animatable.getAnimData(LaserTurretBlockEntity.HAS_TARGET);
        return Boolean.TRUE.equals(hasTarget);
    }

    private double targetX(LaserTurretBlockEntity animatable) {
        Double v = animatable.getAnimData(LaserTurretBlockEntity.TARGET_POS_X);
        return v != null ? v : 0;
    }

    private double targetY(LaserTurretBlockEntity animatable) {
        Double v = animatable.getAnimData(LaserTurretBlockEntity.TARGET_POS_Y);
        return v != null ? v : 0;
    }

    private double targetZ(LaserTurretBlockEntity animatable) {
        Double v = animatable.getAnimData(LaserTurretBlockEntity.TARGET_POS_Z);
        return v != null ? v : 0;
    }
}
