package com.tian_nu.AdvancedTurret.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.entity.RocketEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * 火箭弹渲染器
 */
public class RocketRenderer extends EntityRenderer<RocketEntity> {

    private static final ResourceLocation TEXTURE = TurretMod.location("textures/entity/projectile_box.png");
    private static final float BOX_SIZE = 4.0F / 16.0F;

    public RocketRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RocketEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        alignToMotion(entity, poseStack);
        ProjectileBoxRenderHelper.renderBox(poseStack, buffer, TEXTURE, packedLight,
                BOX_SIZE, BOX_SIZE, BOX_SIZE,
                140, 140, 140, 255);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void alignToMotion(RocketEntity entity, PoseStack poseStack) {
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
    public ResourceLocation getTextureLocation(RocketEntity entity) {
        return TEXTURE;
    }
}
