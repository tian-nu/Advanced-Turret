package com.tian_nu.AdvancedTurret.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.entity.RailgunBulletEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 磁轨炮子弹渲染器
 * 
 * <p>渲染高速电弧子弹，使用蓝紫色纹理</p>
 * 
 * @author tian_nu
 */
public class RailgunBulletRenderer extends EntityRenderer<RailgunBulletEntity> {

    // 临时使用机枪子弹纹理，后续可替换为专用纹理
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TurretMod.MOD_ID, "textures/entity/turret_bullet.png");

    public RailgunBulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RailgunBulletEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        
        // 磁轨炮子弹更大更亮
        poseStack.scale(1.5F, 1.5F, 1.5F);
        
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(this.getTextureLocation(entity)));
        
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();
        
        float size = 0.25F;
        
        // 使用更亮的颜色（青蓝色）
        int r = 100;
        int g = 200;
        int b = 255;
        int a = 255;
        
        consumer.vertex(matrix4f, -size, -size, 0.0F)
                .color(r, g, b, a)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880) // 全亮
                .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                .endVertex();
        
        consumer.vertex(matrix4f, size, -size, 0.0F)
                .color(r, g, b, a)
                .uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                .endVertex();
        
        consumer.vertex(matrix4f, size, size, 0.0F)
                .color(r, g, b, a)
                .uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                .endVertex();
        
        consumer.vertex(matrix4f, -size, size, 0.0F)
                .color(r, g, b, a)
                .uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                .endVertex();
        
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RailgunBulletEntity entity) {
        return TEXTURE;
    }
}
