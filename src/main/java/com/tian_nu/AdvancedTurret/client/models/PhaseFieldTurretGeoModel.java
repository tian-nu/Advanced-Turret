package com.tian_nu.AdvancedTurret.client.models;

import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.blocks.entitys.PhaseFieldTurretBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/** 相位立场炮塔 Geo 模型。 */
public class PhaseFieldTurretGeoModel extends GeoModel<PhaseFieldTurretBlockEntity> {

    private static final float CORE_SPIN_SPEED = -0.18F;

    @Override
    public ResourceLocation getModelResource(PhaseFieldTurretBlockEntity animatable) {
        return TurretMod.location("geo/block/phase_field_turret.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PhaseFieldTurretBlockEntity animatable) {
        return TurretMod.location("textures/block/phase_field_turret.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PhaseFieldTurretBlockEntity animatable) {
        return TurretMod.location("animations/block/machine_gun_turret.animation.json");
    }

    @Override
    public void setCustomAnimations(PhaseFieldTurretBlockEntity animatable, long instanceId,
                                    AnimationState<PhaseFieldTurretBlockEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        CoreGeoBone coreBone = getRotatingCoreBone();
        if (coreBone == null) {
            return;
        }

        if (Boolean.TRUE.equals(animatable.getAnimData(PhaseFieldTurretBlockEntity.WORKING_ACTIVE))) {
            double gameTime = animatable.getLevel() != null ? animatable.getLevel().getGameTime() : 0.0D;
            float rotY = (float) ((gameTime + animationState.getPartialTick()) * CORE_SPIN_SPEED);
            coreBone.setRotY(rotY);
        } else {
            coreBone.setRotY(0.0F);
        }
    }

    private CoreGeoBone getRotatingCoreBone() {
        CoreGeoBone bone = getAnimationProcessor().getBone("炮塔主体");
        if (bone != null) {
            return bone;
        }
        bone = getAnimationProcessor().getBone("core");
        if (bone != null) {
            return bone;
        }
        return getAnimationProcessor().getBone("turret");
    }
}