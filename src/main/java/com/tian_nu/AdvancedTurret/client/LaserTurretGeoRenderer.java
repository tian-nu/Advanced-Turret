package com.tian_nu.AdvancedTurret.client;

import com.tian_nu.AdvancedTurret.blocks.entitys.LaserTurretBlockEntity;
import com.tian_nu.AdvancedTurret.client.models.LaserTurretGeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * 激光炮塔渲染器
 *
 * <p>使用GeckoLib渲染炮塔模型</p>
 * <p>激光光束通过 RenderLevelStageEvent 渲染，见 LaserBeamRenderer</p>
 */
public class LaserTurretGeoRenderer extends GeoBlockRenderer<LaserTurretBlockEntity> {

    public LaserTurretGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new LaserTurretGeoModel());
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        switch (facing) {
            case NORTH -> {
                poseStack.translate(0, 0.5, 0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(270));
            }
            case EAST -> {
                poseStack.translate(-0.5, 0.5, 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
            }
            case SOUTH -> {
                poseStack.translate(0, 0.5, -0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
            }
            case WEST -> {
                poseStack.translate(0.5, 0.5, 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(90));
            }
            case DOWN -> {
                poseStack.translate(0, 1, 0);
                poseStack.mulPose(Axis.XP.rotationDegrees(180));
            }
            case UP -> {
            }
        }
    }
}
