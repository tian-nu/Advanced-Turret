package com.tian_nu.AdvancedTurret.client;

import com.tian_nu.AdvancedTurret.blocks.entitys.RocketTurretBlockEntity;
import com.tian_nu.AdvancedTurret.client.models.RocketTurretGeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * 火箭炮塔渲染器
 * 
 * <p>使用GeckoLib渲染炮塔模型</p>
 */
public class RocketTurretGeoRenderer extends GeoBlockRenderer<RocketTurretBlockEntity> {

    public RocketTurretGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new RocketTurretGeoModel());
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
