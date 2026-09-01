package com.tian_nu.AdvancedTurret.client.models;

import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.blocks.entitys.BodyRestorationTurretBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/** 身躯复原炮塔 Geo 模型（占位模型，待 ashfox 批量产出后替换）。 */
public class BodyRestorationTurretGeoModel extends GeoModel<BodyRestorationTurretBlockEntity> {

    private static final float CORE_SPIN_SPEED = 0.10F;

    @Override
    public ResourceLocation getModelResource(BodyRestorationTurretBlockEntity animatable) {
        return TurretMod.location("geo/block/body_restoration_turret.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BodyRestorationTurretBlockEntity animatable) {
        return TurretMod.location("textures/block/body_restoration_turret.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BodyRestorationTurretBlockEntity animatable) {
        return TurretMod.location("animations/block/body_restoration_turret.animation.json");
    }

    @Override
    public void setCustomAnimations(BodyRestorationTurretBlockEntity animatable, long instanceId,
                                    AnimationState<BodyRestorationTurretBlockEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        CoreGeoBone coreBone = getAnimationProcessor().getBone("turret");
        if (coreBone == null) {
            coreBone = getAnimationProcessor().getBone("炮塔主体");
        }
        if (coreBone == null) {
            coreBone = getAnimationProcessor().getBone("core");
        }
        if (coreBone == null) {
            return;
        }

        double gameTime = animatable.getLevel() != null ? animatable.getLevel().getGameTime() : 0.0D;
        float rotY = (float) ((gameTime + animationState.getPartialTick()) * CORE_SPIN_SPEED);
        coreBone.setRotY(rotY);
    }
}
