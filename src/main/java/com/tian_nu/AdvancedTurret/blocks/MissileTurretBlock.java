package com.tian_nu.AdvancedTurret.blocks;

import com.tian_nu.AdvancedTurret.blocks.entitys.MissileTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.ModBlockEntities;
import com.tian_nu.AdvancedTurret.blocks.entitys.TurretBaseBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

import java.util.List;

/**
 * 导弹炮塔方块
 *
 * <p>特点：</p>
 * <ul>
 *   <li>远程攻击（64格范围）</li>
 *   <li>制导追踪（导弹自动追踪目标）</li>
 *   <li>爆炸伤害（直击10 + 爆炸15，半径4格）</li>
 *   <li>低射速（6.7秒/发）</li>
 *   <li>高能耗（10000 FE/发）</li>
 *   <li>越飞越快（导弹加速飞行）</li>
 * </ul>
 *
 * @author tian_nu
 */
public class MissileTurretBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final VoxelShape SHAPE_UP = Block.box(4, 0, 4, 12, 8, 12);
    private static final VoxelShape SHAPE_DOWN = Block.box(4, 8, 4, 12, 16, 12);
    private static final VoxelShape SHAPE_NORTH = Block.box(4, 4, 8, 12, 12, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(4, 4, 0, 12, 12, 8);
    private static final VoxelShape SHAPE_EAST = Block.box(0, 4, 4, 8, 12, 12);
    private static final VoxelShape SHAPE_WEST = Block.box(8, 4, 4, 16, 12, 12);

    public MissileTurretBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player player) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MissileTurretBlockEntity turret) {
                TurretBaseBlockEntity base = turret.getBaseEntity();
                if (base != null && base.getOwner() == null) {
                    base.setOwner(player.getUUID());
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.advanced_turret.turret.place_on_base").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.advanced_turret.missile_turret.stats",
                MissileTurretBlockEntity.DIRECT_DAMAGE,
                MissileTurretBlockEntity.EXPLOSION_DAMAGE,
                MissileTurretBlockEntity.EXPLOSION_RADIUS,
                MissileTurretBlockEntity.SEARCH_RADIUS,
                MissileTurretBlockEntity.FIRE_RATE / 20.0,
                MissileTurretBlockEntity.ENERGY_COST
        ).withStyle(ChatFormatting.GRAY));
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

        BlockEntity blockEntity = level.getBlockEntity(basePos);
        if (blockEntity instanceof TurretBaseBlockEntity) {
            return this.defaultBlockState().setValue(FACING, clickedFace);
        }

        return null;
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction,
                                           @NotNull BlockState neighborState, @NotNull LevelAccessor level,
                                           @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        if (direction == facing.getOpposite()) {
            BlockPos basePos = pos.relative(facing.getOpposite());
            if (!(level.getBlockEntity(basePos) instanceof TurretBaseBlockEntity)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new MissileTurretBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.MISSILE_TURRET.get(), MissileTurretBlockEntity::tick);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> SHAPE_UP;
            case DOWN -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
        };
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                          @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            BlockPos basePos = pos.relative(facing.getOpposite());
            BlockEntity blockEntity = level.getBlockEntity(basePos);
            if (blockEntity instanceof TurretBaseBlockEntity baseEntity) {
                return baseEntity.getBlockState().getBlock().use(baseEntity.getBlockState(), level, basePos, player, hand,
                        new BlockHitResult(hit.getLocation(), facing.getOpposite(), basePos, hit.isInside()));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
