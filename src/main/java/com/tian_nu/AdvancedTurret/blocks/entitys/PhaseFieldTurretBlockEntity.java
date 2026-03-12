package com.tian_nu.AdvancedTurret.blocks.entitys;

import com.tian_nu.AdvancedTurret.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 相位立场炮塔方块实体。
 *
 * <p>对范围内敌对生物持续施加减益，并根据升级组件提升效果。</p>
 */
public class PhaseFieldTurretBlockEntity extends AbstractFieldTurretBlockEntity {

    public PhaseFieldTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHASE_FIELD_TURRET.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PhaseFieldTurretBlockEntity blockEntity) {
        blockEntity.tickServer();
    }

    @Override
    protected double getBaseRange() {
        return Config.phaseFieldRange;
    }

    @Override
    protected double getRangeBonusPerComponent() {
        return 4.0D;
    }

    @Override
    protected int getBaseEnergyCostPerTick() {
        return Config.phaseFieldEnergyPerTick;
    }

    @Override
    protected int getBaseEffectDuration() {
        return Config.phaseFieldEffectDuration;
    }

    @Override
    protected int getFireRateDurationBonusTicks() {
        return 600;
    }

    @Override
    protected List<LivingEntity> collectTargets(Level level, BlockPos pos, double range, TurretBaseBlockEntity base) {
        return level.getEntitiesOfClass(LivingEntity.class, createSearchBox(pos, range), entity ->
                entity.isAlive() && !entity.isInvulnerable() && entity instanceof Enemy);
    }

    @Override
    protected void applyEffects(List<LivingEntity> targets, TurretBaseBlockEntity base, Direction facing) {
        int amplifierBonus = getEffectAmplifierBonus(base, facing);
        int duration = getEffectDuration(base, facing);
        int refreshInterval = getRefreshInterval(facing);
        for (LivingEntity target : targets) {
            refreshEffect(target, MobEffects.WEAKNESS, amplifierBonus, duration, refreshInterval);
            refreshEffect(target, MobEffects.MOVEMENT_SLOWDOWN, amplifierBonus, duration, refreshInterval);
            refreshEffect(target, MobEffects.DIG_SLOWDOWN, amplifierBonus, duration, refreshInterval);
        }
    }
}