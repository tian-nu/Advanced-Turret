package com.tian_nu.AdvancedTurret.blocks;

import com.tian_nu.AdvancedTurret.Config;
import com.tian_nu.AdvancedTurret.blocks.entitys.ModBlockEntities;
import com.tian_nu.AdvancedTurret.blocks.entitys.PhaseFieldTurretBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** 相位立场炮塔方块。 */
public class PhaseFieldTurretBlock extends AbstractFieldTurretBlock {

    public PhaseFieldTurretBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void appendFieldStats(List<Component> tooltip) {
        tooltip.add(Component.translatable(
                "tooltip.advanced_turret.phase_field_turret.stats",
                Config.phaseFieldRange,
                Config.phaseFieldEnergyPerTick,
                Config.phaseFieldEffectDuration
        ).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new PhaseFieldTurretBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state,
                                                                            @NotNull BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.PHASE_FIELD_TURRET.get(),
                PhaseFieldTurretBlockEntity::tick);
    }
}