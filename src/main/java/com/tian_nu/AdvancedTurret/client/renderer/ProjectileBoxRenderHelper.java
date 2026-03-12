package com.tian_nu.AdvancedTurret.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 盒体投射物渲染辅助。
 *
 * <p>用于把简单投射物渲染成固定朝向的长条/方块，而不是始终朝向玩家的平面片。</p>
 */
public final class ProjectileBoxRenderHelper {

    private ProjectileBoxRenderHelper() {
    }

    public static void renderBox(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture,
                                 int packedLight, float width, float height, float depth,
                                 int red, int green, int blue, int alpha) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();

        float halfWidth = width * 0.5F;
        float halfHeight = height * 0.5F;
        float halfDepth = depth * 0.5F;

        // 前面
        addQuad(consumer, matrix4f, matrix3f,
                -halfWidth, -halfHeight, halfDepth,
                halfWidth, -halfHeight, halfDepth,
                halfWidth, halfHeight, halfDepth,
                -halfWidth, halfHeight, halfDepth,
                0.0F, 0.0F, 1.0F,
                red, green, blue, alpha, packedLight);

        // 后面
        addQuad(consumer, matrix4f, matrix3f,
                halfWidth, -halfHeight, -halfDepth,
                -halfWidth, -halfHeight, -halfDepth,
                -halfWidth, halfHeight, -halfDepth,
                halfWidth, halfHeight, -halfDepth,
                0.0F, 0.0F, -1.0F,
                red, green, blue, alpha, packedLight);

        // 左面
        addQuad(consumer, matrix4f, matrix3f,
                -halfWidth, -halfHeight, -halfDepth,
                -halfWidth, -halfHeight, halfDepth,
                -halfWidth, halfHeight, halfDepth,
                -halfWidth, halfHeight, -halfDepth,
                -1.0F, 0.0F, 0.0F,
                red, green, blue, alpha, packedLight);

        // 右面
        addQuad(consumer, matrix4f, matrix3f,
                halfWidth, -halfHeight, halfDepth,
                halfWidth, -halfHeight, -halfDepth,
                halfWidth, halfHeight, -halfDepth,
                halfWidth, halfHeight, halfDepth,
                1.0F, 0.0F, 0.0F,
                red, green, blue, alpha, packedLight);

        // 上面
        addQuad(consumer, matrix4f, matrix3f,
                -halfWidth, halfHeight, halfDepth,
                halfWidth, halfHeight, halfDepth,
                halfWidth, halfHeight, -halfDepth,
                -halfWidth, halfHeight, -halfDepth,
                0.0F, 1.0F, 0.0F,
                red, green, blue, alpha, packedLight);

        // 下面
        addQuad(consumer, matrix4f, matrix3f,
                -halfWidth, -halfHeight, -halfDepth,
                halfWidth, -halfHeight, -halfDepth,
                halfWidth, -halfHeight, halfDepth,
                -halfWidth, -halfHeight, halfDepth,
                0.0F, -1.0F, 0.0F,
                red, green, blue, alpha, packedLight);
    }

    private static void addQuad(VertexConsumer consumer, Matrix4f matrix4f, Matrix3f matrix3f,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4,
                                float normalX, float normalY, float normalZ,
                                int red, int green, int blue, int alpha, int packedLight) {
        consumer.vertex(matrix4f, x1, y1, z1)
                .color(red, green, blue, alpha)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(matrix3f, normalX, normalY, normalZ)
                .endVertex();

        consumer.vertex(matrix4f, x2, y2, z2)
                .color(red, green, blue, alpha)
                .uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(matrix3f, normalX, normalY, normalZ)
                .endVertex();

        consumer.vertex(matrix4f, x3, y3, z3)
                .color(red, green, blue, alpha)
                .uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(matrix3f, normalX, normalY, normalZ)
                .endVertex();

        consumer.vertex(matrix4f, x4, y4, z4)
                .color(red, green, blue, alpha)
                .uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(matrix3f, normalX, normalY, normalZ)
                .endVertex();
    }
}