package com.tian_nu.AdvancedTurret.client.models;

import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.blocks.entitys.ResonanceFieldTurretBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/** 谐振立场炮塔 Geo 模型。 */
public class ResonanceFieldTurretGeoModel extends GeoModel<ResonanceFieldTurretBlockEntity> {

    @Override
    public ResourceLocation getModelResource(ResonanceFieldTurretBlockEntity animatable) {
        return TurretMod.location("geo/block/machine_gun_turret.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ResonanceFieldTurretBlockEntity animatable) {
        return TurretMod.location("textures/block/resonance_field_turret.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ResonanceFieldTurretBlockEntity animatable) {
        return TurretMod.location("animations/block/machine_gun_turret.animation.json");
    }

    @Override
    public void setCustomAnimations(ResonanceFieldTurretBlockEntity animatable, long instanceId,
                                    AnimationState<ResonanceFieldTurretBlockEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
    }
}