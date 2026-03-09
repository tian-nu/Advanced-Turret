package com.tian_nu.AdvancedTurret.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.entity.GrenadeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 榴弹渲染器
 * 
 * <p>渲染抛物线飞行的榴弹，使用暗绿色纹理</p>
 * 
 * @author tian_nu
 */
public class GrenadeRenderer extends EntityRenderer<GrenadeEntity> {

    private static final ResourceLocation TEXTURE = TurretMod.location("textures/entity/grenade.png");

    public GrenadeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(GrenadeEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(GrenadeEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        
        poseStack.pushPose();
        
        // 旋转面向飞行方向
        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.lengthSqr() > 0.001) {
            float yaw = (float) (Math.atan2(velocity.z, velocity.x) * 180.0 / Math.PI) - 90.0F;
            float pitch = (float) (Math.asin(velocity.y / velocity.length()) * 180.0 / Math.PI);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));
        }
        
        poseStack.scale(0.8F, 0.8F, 0.8F);
        
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(this.getTextureLocation(entity)));
        
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();
        
        float size = 0.3F;
        
        // 暗绿色（军绿色）
        int r = 80;
        int g = 100;
        int b = 60;
        int a = 255;
        
        // 渲染交叉四边形
        renderCrossedQuads(consumer, matrix4f, matrix3f, size, r, g, b, a, packedLight);
        
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
    
    private void renderCrossedQuads(VertexConsumer consumer, Matrix4f matrix4f, Matrix3f matrix3f, 
                                     float size, int r, int g, int b, int a, int packedLight) {
        // 第一个四边形（XZ平面）
        consumer.vertex(matrix4f, -size, 0, -size)
                .color(r, g, b, a)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(matrix3f, 0.0F, 1.0F, 0.0F)
                .endVertex();
        
        consumer.vertex(matrix4f, size, 0, -size)
                .color(r, g, b, a)
                .uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(matrix3f, 0.0F, 1.0F, 0.0F)
                .endVertex();
        
        consumer.vertex(matrix4f, size, 0, size)
                .color(r, g, b, a)
                .uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(matrix3f, 0.0F, 1.0F, 0.0F)
                .endVertex();
        
        consumer.vertex(matrix4f, -size, 0, size)
                .color(r, g, b, a)
                .uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(matrix3f, 0.0F, 1.0F, 0.0F)
                .endVertex();
        
        // 第二个四边形（XY平面）
        consumer.vertex(matrix4f, -size, -size, 0)
                .color(r, g, b, a)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                .endVertex();
        
        consumer.vertex(matrix4f, size, -size, 0)
                .color(r, g, b, a)
                .uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                .endVertex();
        
        consumer.vertex(matrix4f, size, size, 0)
                .color(r, g, b, a)
                .uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                .endVertex();
        
        consumer.vertex(matrix4f, -size, size, 0)
                .color(r, g, b, a)
                .uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                .endVertex();
    }
}
