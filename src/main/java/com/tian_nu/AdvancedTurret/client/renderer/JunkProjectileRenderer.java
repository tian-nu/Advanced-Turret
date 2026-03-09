package com.tian_nu.AdvancedTurret.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.entity.JunkProjectileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 垃圾投射物渲染器
 * 
 * <p>将投射物渲染为弹药物品的3D模型</p>
 * 
 * @author tian_nu
 */
public class JunkProjectileRenderer extends EntityRenderer<JunkProjectileEntity> {

    private final ItemRenderer itemRenderer;
    private static final ResourceLocation DEFAULT_TEXTURE = ResourceLocation.fromNamespaceAndPath(TurretMod.MOD_ID, "textures/entity/turret_bullet.png");

    public JunkProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public ResourceLocation getTextureLocation(JunkProjectileEntity entity) {
        return DEFAULT_TEXTURE;
    }

    @Override
    public void render(JunkProjectileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        
        // 获取弹药物品
        ItemStack ammoItem = entity.getAmmoItem();
        if (ammoItem.isEmpty()) {
            ammoItem = new ItemStack(Items.COBBLESTONE); // 默认渲染为圆石
        }
        
        poseStack.pushPose();
        
        // 调整大小和旋转
        poseStack.scale(0.5f, 0.5f, 0.5f);
        
        // 根据飞行方向旋转
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        
        // 渲染物品
        BakedModel model = itemRenderer.getModel(ammoItem, null, null, 0);
        itemRenderer.render(
            ammoItem,
            ItemDisplayContext.GROUND,
            false,
            poseStack,
            buffer,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            model
        );
        
        poseStack.popPose();
    }
}
