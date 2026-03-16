package com.tian_nu.AdvancedTurret.blocks;

import com.tian_nu.AdvancedTurret.blocks.entitys.MachineGunTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.ModBlockEntities;
import com.tian_nu.AdvancedTurret.blocks.entitys.TurretBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import java.util.List;
import net.minecraft.ChatFormatting;
import com.tian_nu.AdvancedTurret.Config;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * 机枪炮塔方块
 * 
 * <p>只能放置在炮塔基座的6个面上，使用基座的能量进行射击</p>
 * 
 * @author tian_nu
 */
public class MachineGunTurretBlock extends BaseEntityBlock {
    
    // 朝向属性
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    
    // 碰撞箱形状
    private static final VoxelShape SHAPE_UP = Block.box(4, 0, 4, 12, 8, 12);
    private static final VoxelShape SHAPE_DOWN = Block.box(4, 8, 4, 12, 16, 12);
    private static final VoxelShape SHAPE_NORTH = Block.box(4, 4, 8, 12, 12, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(4, 4, 0, 12, 12, 8);
    private static final VoxelShape SHAPE_EAST = Block.box(0, 4, 4, 8, 12, 12);
    private static final VoxelShape SHAPE_WEST = Block.box(8, 4, 4, 16, 12, 12);
    
    public MachineGunTurretBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player player) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MachineGunTurretBlockEntity turret) {
                TurretBaseBlockEntity base = turret.getBaseEntity();
                if (base != null && base.getOwner() == null) {
                    base.setOwner(player.getUUID());
                }
                
                // 将所有者信息保存到方块实体中，以便在被破坏时恢复到掉落物
                net.minecraft.nbt.CompoundTag tag = stack.getOrCreateTag();
                if (tag.contains("OwnerName")) {
                    // 这里可以保存更多信息
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        TurretTooltipHelper.addPlacementTooltip(tooltip);
        TurretTooltipHelper.addGrayLine(tooltip, "tooltip.advanced_turret.machine_gun_turret.damage",
                MachineGunTurretBlockEntity.getBulletDamage());
        TurretTooltipHelper.addGrayLine(tooltip, "tooltip.advanced_turret.machine_gun_turret.range_rate",
                MachineGunTurretBlockEntity.getSearchRadius(),
                MachineGunTurretBlockEntity.getFireRate());
        TurretTooltipHelper.addGrayLine(tooltip, "tooltip.advanced_turret.machine_gun_turret.energy_ammo",
                Config.machineGunEnergyCost);
        if (stack.hasTag() && stack.getTag().contains("OwnerName")) {
            String ownerName = stack.getTag().getString("OwnerName");
            tooltip.add(Component.translatable("gui.advanced_turret.owner_tooltip", ownerName).withStyle(ChatFormatting.GOLD));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        BlockPos clickedPos = context.getClickedPos();
        BlockPos basePos = clickedPos.relative(clickedFace.getOpposite());
        Level level = context.getLevel();
        
        // 检查被点击的方块是否是炮塔基座
        BlockEntity blockEntity = level.getBlockEntity(basePos);
        if (blockEntity instanceof TurretBaseBlockEntity) {
            // 返回朝向基座反方向的方块状态
            return this.defaultBlockState().setValue(FACING, clickedFace);
        }
        
        return null;
    }
    
    @SuppressWarnings("deprecation")
    public boolean canSurvive(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos basePos = pos.relative(facing.getOpposite());
        BlockEntity blockEntity = level.getBlockEntity(basePos);
        return blockEntity instanceof TurretBaseBlockEntity;
    }
    
    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, 
                                           @NotNull BlockState neighborState, @NotNull LevelAccessor level, 
                                           @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        // 如果基座被移除，破坏炮塔
        if (direction == facing.getOpposite()) {
            Direction relative = facing.getOpposite();
            BlockPos basePos = pos.relative(relative);
            if (!(level.getBlockEntity(basePos) instanceof TurretBaseBlockEntity)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
    
    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, 
                                        @NotNull BlockPos pos, @NotNull CollisionContext context) {
        switch (state.getValue(FACING)) {
            case UP: return SHAPE_UP;
            case DOWN: return SHAPE_DOWN;
            case NORTH: return SHAPE_NORTH;
            case SOUTH: return SHAPE_SOUTH;
            case EAST: return SHAPE_EAST;
            case WEST: return SHAPE_WEST;
            default: return SHAPE_UP;
        }
    }
    
    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
    
    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new MachineGunTurretBlockEntity(pos, state);
    }
    
    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, 
                                                                            @NotNull BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.MACHINE_GUN_TURRET.get(), 
                MachineGunTurretBlockEntity::tick);
    }
    
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, 
                                          @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        // 右键点击可以打开基座的GUI
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            BlockPos basePos = pos.relative(facing.getOpposite());
            BlockEntity blockEntity = level.getBlockEntity(basePos);
            if (blockEntity instanceof TurretBaseBlockEntity baseEntity) {
                // 调用基座的use方法打开GUI
                return baseEntity.getBlockState().getBlock().use(baseEntity.getBlockState(), level, basePos, player, hand, 
                        new BlockHitResult(hit.getLocation(), facing.getOpposite(), basePos, hit.isInside()));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
