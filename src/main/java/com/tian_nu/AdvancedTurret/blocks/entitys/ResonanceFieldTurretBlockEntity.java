package com.tian_nu.AdvancedTurret.blocks.entitys;

import com.tian_nu.AdvancedTurret.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.network.SerializableDataTicket;

import java.util.List;

/**
 * 谐振立场炮塔方块实体。
 *
 * <p>对范围内主人与驯服生物持续施加增益，并根据升级组件提升效果。</p>
 */
public class ResonanceFieldTurretBlockEntity extends AbstractFieldTurretBlockEntity {

    public static SerializableDataTicket<Boolean> WORKING_ACTIVE;

    public ResonanceFieldTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESONANCE_FIELD_TURRET.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ResonanceFieldTurretBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        blockEntity.tickServer();
    }

    @Override
    protected double getBaseRange() {
        return Config.resonanceFieldRange;
    }

    @Override
    protected double getRangeBonusPerComponent() {
        return 8.0D;
    }

    @Override
    protected int getBaseEnergyCostPerTick() {
        return Config.resonanceFieldEnergyPerTick;
    }

    @Override
    protected int getBaseEffectDuration() {
        return Config.resonanceFieldEffectDuration;
    }

    @Override
    protected int getFireRateDurationBonusTicks() {
        return 1200;
    }

    @Override
    protected SerializableDataTicket<Boolean> getWorkingDataTicket() {
        return WORKING_ACTIVE;
    }

    @Override
    protected List<LivingEntity> collectTargets(Level level, BlockPos pos, double range, TurretBaseBlockEntity base) {
        return level.getEntitiesOfClass(LivingEntity.class, createSearchBox(pos, range), entity ->
                entity.isAlive() && !entity.isInvulnerable() && isFriendlyTarget(entity, base));
    }

    @Override
    protected void applyEffects(List<LivingEntity> targets, TurretBaseBlockEntity base, Direction facing) {
        int amplifierBonus = getEffectAmplifierBonus(base, facing);
        int duration = getEffectDuration(base, facing);
        int refreshInterval = getRefreshInterval(facing);
        for (LivingEntity target : targets) {
            refreshEffect(target, MobEffects.DAMAGE_BOOST, amplifierBonus, duration, refreshInterval);
            refreshEffect(target, MobEffects.MOVEMENT_SPEED, amplifierBonus, duration, refreshInterval);
            refreshEffect(target, MobEffects.DIG_SPEED, amplifierBonus, duration, refreshInterval);
        }
    }
}
