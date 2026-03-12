package com.tian_nu.AdvancedTurret.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.entity.RailgunBulletEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * 磁轨炮子弹渲染器。
 *
 * <p>使用固定朝向的白色长条盒体，避免平面物品在不同视角下始终面向玩家。</p>
 */
public class RailgunBulletRenderer extends EntityRenderer<RailgunBulletEntity> {

    private static final ResourceLocation TEXTURE = TurretMod.location("textures/entity/projectile_box.png");
    private static final float BOX_WIDTH = 1.0F / 16.0F;
    private static final float BOX_HEIGHT = 1.0F / 16.0F;
    private static final float BOX_LENGTH = 5.0F / 16.0F;

    public RailgunBulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RailgunBulletEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        alignToMotion(entity, poseStack, partialTick);
        ProjectileBoxRenderHelper.renderBox(poseStack, buffer, TEXTURE, 15728880,
                BOX_WIDTH, BOX_HEIGHT, BOX_LENGTH,
                255, 255, 255, 255);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void alignToMotion(RailgunBulletEntity entity, PoseStack poseStack, float partialTick) {
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
    public ResourceLocation getTextureLocation(RailgunBulletEntity entity) {
        return TEXTURE;
    }
}