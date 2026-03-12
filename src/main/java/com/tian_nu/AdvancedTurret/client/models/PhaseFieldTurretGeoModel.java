package com.tian_nu.AdvancedTurret.client.models;

import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.blocks.entitys.PhaseFieldTurretBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/** 相位立场炮塔 Geo 模型。 */
public class PhaseFieldTurretGeoModel extends GeoModel<PhaseFieldTurretBlockEntity> {

    @Override
    public ResourceLocation getModelResource(PhaseFieldTurretBlockEntity animatable) {
        return TurretMod.location("geo/block/machine_gun_turret.geo.json");
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
    }
}