package com.tian_nu.AdvancedTurret.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.entity.TurretBulletEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class TurretBulletRenderer extends EntityRenderer<TurretBulletEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TurretMod.MOD_ID, "textures/entity/turret_bullet.png");

    public TurretBulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TurretBulletEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        
        poseStack.scale(1.0F, 1.0F, 1.0F);
        
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(this.getTextureLocation(entity)));
        
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();
        
        float size = 0.25F;
        
        consumer.vertex(matrix4f, -size, -size, 0.0F)
                .color(255, 255, 255, 255)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                .endVertex();
        
        consumer.vertex(matrix4f, size, -size, 0.0F)
                .color(255, 255, 255, 255)
                .uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                .endVertex();
        
        consumer.vertex(matrix4f, size, size, 0.0F)
                .color(255, 255, 255, 255)
                .uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                .endVertex();
        
        consumer.vertex(matrix4f, -size, size, 0.0F)
                .color(255, 255, 255, 255)
                .uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                .endVertex();
        
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TurretBulletEntity entity) {
        return TEXTURE;
    }
}
