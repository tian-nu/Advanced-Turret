package com.tian_nu.AdvancedTurret.client;

import com.tian_nu.AdvancedTurret.blocks.entitys.GrenadeLauncherTurretBlockEntity;
import com.tian_nu.AdvancedTurret.client.models.GrenadeLauncherTurretGeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * 榴弹发射器炮塔渲染器
 */
public class GrenadeLauncherTurretGeoRenderer extends GeoBlockRenderer<GrenadeLauncherTurretBlockEntity> {

    public GrenadeLauncherTurretGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new GrenadeLauncherTurretGeoModel());
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
