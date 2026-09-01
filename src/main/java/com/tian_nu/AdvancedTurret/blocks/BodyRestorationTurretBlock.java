package com.tian_nu.AdvancedTurret.blocks;

import com.tian_nu.AdvancedTurret.Config;
import com.tian_nu.AdvancedTurret.blocks.entitys.BodyRestorationTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** 身躯复原炮塔方块。 */
public class BodyRestorationTurretBlock extends AbstractFieldTurretBlock {

    public BodyRestorationTurretBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void appendFieldStats(List<Component> tooltip) {
        TurretTooltipHelper.addGrayLine(tooltip, "tooltip.advanced_turret.body_restoration_turret.range_energy",
                Config.bodyRestorationRange,
                Config.bodyRestorationStandbyEnergy);
        TurretTooltipHelper.addGrayLine(tooltip, "tooltip.advanced_turret.body_restoration_turret.revive_cost",
                Config.bodyRestorationReviveEnergy);
        TurretTooltipHelper.addGrayLine(tooltip, "tooltip.advanced_turret.body_restoration_turret.revive_cd",
                Config.bodyRestorationReviveCooldownTicks / 20.0);
        TurretTooltipHelper.addDarkGrayLine(tooltip, "tooltip.advanced_turret.body_restoration_turret.whitelist");
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new BodyRestorationTurretBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state,
                                                                            @NotNull BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.BODY_RESTORATION_TURRET.get(),
                BodyRestorationTurretBlockEntity::tick);
    }
}
