package com.tian_nu.AdvancedTurret.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tian_nu.AdvancedTurret.entity.JunkProjectileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 垃圾投射物渲染器
 *
 * <p>优先渲染实际弹药物品；如果没有可用物品数据，则回退到专用实体贴图。</p>
 */
public class JunkProjectileRenderer extends EntityRenderer<JunkProjectileEntity> {

    private final ItemRenderer itemRenderer;
    private static final ItemStack COBBLESTONE_STACK = new ItemStack(Items.COBBLESTONE);

    public JunkProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public ResourceLocation getTextureLocation(JunkProjectileEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    public void render(JunkProjectileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(0.7f, 0.7f, 0.7f);
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        BakedModel model = itemRenderer.getModel(COBBLESTONE_STACK, null, null, 0);
        itemRenderer.render(
            COBBLESTONE_STACK,
            ItemDisplayContext.GROUND,
            false,
            poseStack,
            buffer,
            packedLight,
            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
            model
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
}
