package com.tian_nu.AdvancedTurret.blocks.entitys;

import com.tian_nu.AdvancedTurret.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.network.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

/**
 * 立场炮塔公共方块实体基类。
 *
 * <p>统一处理基座连接、按面启用、能耗、刷新频率与升级组件加成。</p>
 */
public abstract class AbstractFieldTurretBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final int BASE_REFRESH_INTERVAL_TICKS = 40;
    private static final int THRESHOLD_COMPONENT_COUNT = 4;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected AbstractFieldTurretBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> PlayState.CONTINUE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public TurretBaseBlockEntity getBaseEntity() {
        Level level = getLevel();
        if (level == null) {
            return null;
        }
        Direction facing = getFacing();
        BlockPos basePos = worldPosition.relative(facing.getOpposite());
        BlockEntity blockEntity = level.getBlockEntity(basePos);
        if (blockEntity instanceof TurretBaseBlockEntity base) {
            return base;
        }
        return null;
    }

    protected Direction getFacing() {
        return getBlockState().getValue(BlockStateProperties.FACING);
    }

    protected void tickServer() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        TurretBaseBlockEntity base = getBaseEntity();
        if (base == null) {
            setWorkingAnimation(false);
            return;
        }

        Direction facing = getFacing();
        if (!base.isFaceEnabled(facing)) {
            setWorkingAnimation(false);
            return;
        }

        double range = getFieldRange(base, facing);
        int energyCost = base.getEnergyCostForFace(facing, getBaseEnergyCostPerTick());
        List<LivingEntity> targets = collectTargets(level, worldPosition, range, base);
        if (targets.isEmpty()) {
            setWorkingAnimation(false);
            return;
        }
        if (base.getEnergyStored() < energyCost) {
            setWorkingAnimation(false);
            return;
        }

        base.consumeEnergy(energyCost);
        setWorkingAnimation(true);
        applyEffects(targets, base, facing);
    }

    protected double getFieldRange(TurretBaseBlockEntity base, Direction facing) {
        int rangeCount = getUpgradeCount(base, facing, ModItems.RANGE_COMPONENT.get());
        double upgradedRange = getBaseRange() + rangeCount * getRangeBonusPerComponent();
        double manualLimit = base.getManualRangeLimit();
        if (manualLimit > 0.0D) {
            return Math.max(1.0D, Math.min(upgradedRange, manualLimit));
        }
        return upgradedRange;
    }

    protected int getRefreshInterval(Direction facing) {
        return BASE_REFRESH_INTERVAL_TICKS;
    }

    protected int getEffectDuration(TurretBaseBlockEntity base, Direction facing) {
        int fireRateCount = getUpgradeCount(base, facing, ModItems.FIRE_RATE_COMPONENT.get());
        return getBaseEffectDuration() + fireRateCount * getFireRateDurationBonusTicks();
    }

    protected int getEffectAmplifierBonus(TurretBaseBlockEntity base, Direction facing) {
        return getUpgradeCount(base, facing, ModItems.ATTACK_BOOST_COMPONENT.get()) >= THRESHOLD_COMPONENT_COUNT ? 1 : 0;
    }

    protected int getUpgradeCount(TurretBaseBlockEntity base, Direction facing, Item item) {
        return base.getUpgradeItemCountForFace(facing, item);
    }

    protected void setWorkingAnimation(boolean working) {
        setAnimData(getWorkingDataTicket(), working);
    }

    protected boolean shouldApplyEffect(LivingEntity entity, MobEffect effect, int amplifier, int duration, int refreshInterval) {
        MobEffectInstance current = entity.getEffect(effect);
        if (current == null) {
            return true;
        }
        if (current.getAmplifier() != amplifier) {
            return true;
        }
        return current.getDuration() <= duration - refreshInterval;
    }

    protected void refreshEffect(LivingEntity entity, MobEffect effect, int amplifier, int duration, int refreshInterval) {
        if (shouldApplyEffect(entity, effect, amplifier, duration, refreshInterval)) {
            entity.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true));
        }
    }

    protected abstract double getBaseRange();

    protected abstract double getRangeBonusPerComponent();

    protected abstract int getBaseEnergyCostPerTick();

    protected abstract int getBaseEffectDuration();

    protected abstract int getFireRateDurationBonusTicks();

    protected abstract SerializableDataTicket<Boolean> getWorkingDataTicket();

    protected abstract List<LivingEntity> collectTargets(Level level, BlockPos pos, double range, TurretBaseBlockEntity base);

    protected abstract void applyEffects(List<LivingEntity> targets, TurretBaseBlockEntity base, Direction facing);

    protected AABB createSearchBox(BlockPos pos, double radius) {
        return new AABB(
                pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
                pos.getX() + radius + 1.0D, pos.getY() + radius + 1.0D, pos.getZ() + radius + 1.0D
        );
    }

    protected boolean isFriendlyTarget(LivingEntity entity, TurretBaseBlockEntity base) {
        UUID ownerId = base.getOwner();
        if (ownerId == null) {
            return false;
        }
        if (entity instanceof Player player) {
            return player.getUUID().equals(ownerId) && !player.isSpectator();
        }
        if (entity instanceof net.minecraft.world.entity.TamableAnimal tamable) {
            UUID tameOwner = tamable.getOwnerUUID();
            return tameOwner != null && tameOwner.equals(ownerId);
        }
        return false;
    }

    @Override
    protected void saveAdditional(@NotNull net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
        TurretOwnerHelper.saveOwnerNameFromBase(tag, getBaseEntity());
    }

    @Override
    public void load(@NotNull net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
    }
}
