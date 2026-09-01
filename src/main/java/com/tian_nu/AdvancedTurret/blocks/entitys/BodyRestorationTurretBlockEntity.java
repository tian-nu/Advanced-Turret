package com.tian_nu.AdvancedTurret.blocks.entitys;

import com.tian_nu.AdvancedTurret.Config;
import com.tian_nu.AdvancedTurret.items.ModItems;
import com.tian_nu.AdvancedTurret.items.SmartChipItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 身躯复原炮塔方块实体。
 *
 * <p>挂载后持续消耗能量待机；范围内白名单玩家死亡时，消耗能量与躯体储液罐将其原地复原
 * （类似不死图腾的复活效果）。</p>
 */
public class BodyRestorationTurretBlockEntity extends BlockEntity implements GeoBlockEntity {

    /** 白名单实体 ID（玩家）。 */
    public static final String PLAYER_ENTITY_ID = "minecraft:player";

    /** 已加载的身躯复原炮塔索引（仅服务端线程访问）。 */
    public static final List<BodyRestorationTurretBlockEntity> ACTIVE_TURRETS = new ArrayList<>();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean indexed = false;

    public BodyRestorationTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BODY_RESTORATION_TURRET.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BodyRestorationTurretBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        blockEntity.tickServer(level);
    }

    private void tickServer(Level level) {
        TurretBaseBlockEntity base = getBaseEntity();
        if (base == null) {
            return;
        }
        Direction facing = getFacing();
        if (!base.isFaceEnabled(facing)) {
            return;
        }
        // 待机耗能（节能组件可降低）
        int standby = base.getEnergyCostForFace(facing, Config.bodyRestorationStandbyEnergy);
        if (base.getEnergyStored() < standby) {
            return;
        }
        base.consumeEnergy(standby);
    }

    public TurretBaseBlockEntity getBaseEntity() {
        Level level = getLevel();
        if (level == null) {
            return null;
        }
        Direction facing = getFacing();
        BlockPos basePos = worldPosition.relative(facing.getOpposite());
        BlockEntity blockEntity = level.getBlockEntity(basePos);
        return blockEntity instanceof TurretBaseBlockEntity base ? base : null;
    }

    public Direction getFacing() {
        return getBlockState().getValue(BlockStateProperties.FACING);
    }

    /**
     * 复活作用范围：基础范围 + 射程升级组件加成，受基座手动限制约束。
     */
    public double getReviveRange(TurretBaseBlockEntity base, Direction facing) {
        int rangeCount = base.getUpgradeItemCountForFace(facing, ModItems.RANGE_COMPONENT.get());
        double range = Config.bodyRestorationRange + rangeCount * 1.0D;
        double manual = base.getManualRangeLimit();
        if (manual > 0.0D) {
            return Math.max(1.0D, Math.min(range, manual));
        }
        return range;
    }

    /**
     * 尝试复原目标玩家。全部条件（白名单/范围/能量/储液罐）满足才消耗并返回 true。
     */
    public boolean tryRevive(ServerPlayer player) {
        TurretBaseBlockEntity base = getBaseEntity();
        if (base == null) {
            return false;
        }
        Direction facing = getFacing();
        if (!base.isFaceEnabled(facing)) {
            return false;
        }
        // 必须处于芯片白名单中
        if (!SmartChipItem.getWhitelist(base.getPluginStack()).contains(PLAYER_ENTITY_ID)) {
            return false;
        }
        double range = getReviveRange(base, facing);
        double dist = player.distanceToSqr(
                worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D);
        if (dist > range * range) {
            return false;
        }
        int cost = Config.bodyRestorationReviveEnergy;
        if (base.getEnergyStored() < cost) {
            return false;
        }
        if (!hasCanister(base)) {
            return false;
        }
        // 扣除能量与储液罐（弹药回收插件有概率不消耗储液罐）
        base.consumeEnergy(cost);
        Level level = base.getLevel();
        if (!(level != null && base.hasAmmoRecyclingPlugin()
                && level.random.nextFloat() < Config.ammoRecycleChance)) {
            consumeCanister(base);
        }
        return true;
    }

    private boolean hasCanister(TurretBaseBlockEntity base) {
        IItemHandler ammo = base.getAmmoInventory();
        for (int i = 0; i < ammo.getSlots(); i++) {
            ItemStack stack = ammo.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(ModItems.BODY_ESSENCE_CANISTER.get())) {
                return true;
            }
        }
        return false;
    }

    private void consumeCanister(TurretBaseBlockEntity base) {
        IItemHandler ammo = base.getAmmoInventory();
        for (int i = 0; i < ammo.getSlots(); i++) {
            ItemStack stack = ammo.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(ModItems.BODY_ESSENCE_CANISTER.get())) {
                ammo.extractItem(i, 1, false);
                return;
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        Level level = getLevel();
        if (level != null && !level.isClientSide && !indexed) {
            ACTIVE_TURRETS.add(this);
            indexed = true;
        }
    }

    @Override
    public void setRemoved() {
        if (indexed) {
            ACTIVE_TURRETS.remove(this);
            indexed = false;
        }
        super.setRemoved();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> PlayState.CONTINUE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
    }
}
