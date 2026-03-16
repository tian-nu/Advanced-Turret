package com.tian_nu.AdvancedTurret.blocks;

import com.tian_nu.AdvancedTurret.Config;
import com.tian_nu.AdvancedTurret.blocks.entitys.ModBlockEntities;
import com.tian_nu.AdvancedTurret.blocks.entitys.ResonanceFieldTurretBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** 谐振立场炮塔方块。 */
public class ResonanceFieldTurretBlock extends AbstractFieldTurretBlock {

    public ResonanceFieldTurretBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void appendFieldStats(List<Component> tooltip) {
        TurretTooltipHelper.addGrayLine(tooltip, "tooltip.advanced_turret.resonance_field_turret.range_energy",
                Config.resonanceFieldRange,
                Config.resonanceFieldEnergyPerTick);
        TurretTooltipHelper.addGrayLine(tooltip, "tooltip.advanced_turret.resonance_field_turret.duration",
                Config.resonanceFieldEffectDuration / 20.0);
        TurretTooltipHelper.addDarkGrayLine(tooltip, "tooltip.advanced_turret.resonance_field_turret.effect");
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ResonanceFieldTurretBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state,
                                                                            @NotNull BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.RESONANCE_FIELD_TURRET.get(),
                ResonanceFieldTurretBlockEntity::tick);
    }
}