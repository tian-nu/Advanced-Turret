package com.tian_nu.AdvancedTurret.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.entity.MissileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * 导弹实体渲染器
 */
public class MissileRenderer extends EntityRenderer<MissileEntity> {

    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(TurretMod.MOD_ID, "textures/item/missile.png");
    private static final float BODY_WIDTH = 3.0F / 16.0F;
    private static final float BODY_HEIGHT = 3.0F / 16.0F;
    private static final float BODY_DEPTH = 10.0F / 16.0F;

    public MissileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MissileEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        alignToMotion(entity, poseStack);
        ProjectileBoxRenderHelper.renderBox(
            poseStack,
            buffer,
            TEXTURE,
            packedLight,
            BODY_WIDTH,
            BODY_HEIGHT,
            BODY_DEPTH,
            255,
            255,
            255,
            255
        );
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void alignToMotion(MissileEntity entity, PoseStack poseStack) {
        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.lengthSqr() < 1.0E-6D) {
            poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
            poseStack.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));
            return;
        }

        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float yaw = (float) (Math.atan2(velocity.x, velocity.z) * 180.0D / Math.PI);
        float pitch = (float) (Math.atan2(velocity.y, horizontal) * 180.0D / Math.PI);

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
    }

    @Override
    public ResourceLocation getTextureLocation(MissileEntity entity) {
        return TEXTURE;
    }
}
