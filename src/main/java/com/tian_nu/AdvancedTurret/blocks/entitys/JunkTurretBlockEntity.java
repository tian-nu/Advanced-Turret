package com.tian_nu.AdvancedTurret.blocks.entitys;

import com.tian_nu.AdvancedTurret.Config;
import com.tian_nu.AdvancedTurret.blocks.JunkTurretBlock;
import com.tian_nu.AdvancedTurret.entity.JunkProjectileEntity;
import com.tian_nu.AdvancedTurret.items.SmartChipItem;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.network.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.List;

/**
 * 垃圾炮塔方块实体
 *
 * <p>特点：</p>
 * <ul>
 *   <li>任意物品作弹药</li>
 *   <li>抛物线弹道</li>
 *   <li>低伤害低成本</li>
 *   <li>投射物渲染为弹药物品</li>
 * </ul>
 *
 * @author tian_nu
 */
public class JunkTurretBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    // ========== 常量 ==========

    /** 射击间隔 (tick) - 0.5发/秒 */
    public static final int FIRE_RATE = 40;
    /** 搜索范围 */
    public static final double SEARCH_RADIUS = 16.0;
    /** 子弹速度 */
    public static final double BULLET_SPEED = 2.0;
    /** 子弹伤害 */
    public static final float BULLET_DAMAGE = 2.0F;

    /** 重力常数 */
    public static final double GRAVITY = 0.05;
    /** 最小抛物线高度（相对于起点） */
    private static final double MIN_ARC_HEIGHT = 0.6;
    /** 最大抛物线高度（相对于起点） */
    private static final double MAX_ARC_HEIGHT = 4.2;

    public static int getFireRate() { return Config.junkTurretFireRate; }
    public static double getSearchRadius() { return Config.junkTurretRange; }
    public static float getBulletDamage() { return (float) Config.junkTurretDamage; }
    public static double getBulletSpeed() { return Config.junkTurretBulletSpeed; }
    public static double getGravity() { return Config.junkTurretGravity; }
    public static double getMinArcHeight() { return Config.junkTurretMinArcHeight; }
    public static double getMaxArcHeight() { return Config.junkTurretMaxArcHeight; }

    // ========== GeckoLib数据同步票 ==========
    public static SerializableDataTicket<Boolean> HAS_TARGET;
    public static SerializableDataTicket<Double> TARGET_POS_X;
    public static SerializableDataTicket<Double> TARGET_POS_Y;
    public static SerializableDataTicket<Double> TARGET_POS_Z;
    public static SerializableDataTicket<Double> AIM_DIR_X;
    public static SerializableDataTicket<Double> AIM_DIR_Y;
    public static SerializableDataTicket<Double> AIM_DIR_Z;

    // ========== GeckoLib动画缓存 ==========
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ========== 字段 ==========

    private int cooldown = 0;
    private LivingEntity target = null;
    private int targetLostTicks = 0;
    private Vec3 visibleTargetPoint = null;

    public float yRot0 = 0.0f;
    public float xRot0 = 0.0f;

    public JunkTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.JUNK_TURRET.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, JunkTurretBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        TurretBaseBlockEntity base = blockEntity.getBaseEntity();
        if (base == null) return;

        // 无电量低头动画提示
        if (base.getEnergyStored() == 0) {
            blockEntity.target = null;
            blockEntity.setAnimData(HAS_TARGET, true);
            blockEntity.setAnimData(TARGET_POS_X, pos.getX() + 0.5);
            blockEntity.setAnimData(TARGET_POS_Y, pos.getY() - 2.0);
            blockEntity.setAnimData(TARGET_POS_Z, pos.getZ() + 0.5);
            blockEntity.setAnimData(AIM_DIR_X, 0.0);
            blockEntity.setAnimData(AIM_DIR_Y, -1.0);
            blockEntity.setAnimData(AIM_DIR_Z, 0.0);
            return;
        }

        Direction facing = state.getValue(JunkTurretBlock.FACING);
        if (!base.isFaceEnabled(facing)) {
            blockEntity.target = null;
            blockEntity.setAnimData(HAS_TARGET, false);
            blockEntity.setAnimData(AIM_DIR_X, 0.0);
            blockEntity.setAnimData(AIM_DIR_Y, 0.0);
            blockEntity.setAnimData(AIM_DIR_Z, 0.0);
            return;
        }

        if (blockEntity.cooldown > 0) {
            blockEntity.cooldown--;
        }

        blockEntity.updateTarget(level, pos, base, facing);

        if (blockEntity.target != null && blockEntity.cooldown <= 0) {
            if (blockEntity.canShoot(base)) {
                blockEntity.shoot(level, pos, state, base);
                blockEntity.cooldown = base.getFireRateForFace(facing, getFireRate());
            }
        }
    }

    // ========== GeckoLib动画控制 ==========

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> PlayState.CONTINUE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /**
     * 获取连接的炮塔基座
     */
    public TurretBaseBlockEntity getBaseEntity() {
        Level level = getLevel();
        if (level == null) return null;

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof JunkTurretBlock)) return null;

        Direction facing = state.getValue(JunkTurretBlock.FACING);
        BlockPos basePos = worldPosition.relative(facing.getOpposite());

        BlockEntity blockEntity = level.getBlockEntity(basePos);
        if (blockEntity instanceof TurretBaseBlockEntity base) {
            return base;
        }
        return null;
    }

    /**
     * 更新目标
     */
    private void updateTarget(Level level, BlockPos pos, TurretBaseBlockEntity base, Direction facing) {
        if (target == null || !isValidTarget(target, level, pos)) {
            if (target != null && base.isThriftyMode()) {
                base.cancelReservation(target.getId());
            }
            target = findTarget(level, pos, base.getSearchRadiusForFace(facing, getSearchRadius()));
            targetLostTicks = 0;

            if (target != null && base.isThriftyMode()) {
                float expectedDamage = getExpectedDamage(base);
                base.reserveDamage(target.getId(), expectedDamage, target.getHealth(), level.getGameTime());
            }
        } else {
            if (!isTargetInRange(target, pos, base.getSearchRadiusForFace(facing, getSearchRadius()))) {
                targetLostTicks++;
                if (targetLostTicks > 20) {
                    if (base.isThriftyMode()) {
                        base.cancelReservation(target.getId());
                    }
                    target = null;
                    visibleTargetPoint = null;
                    setAnimData(HAS_TARGET, false);
                    setAnimData(AIM_DIR_X, 0.0);
                    setAnimData(AIM_DIR_Y, 0.0);
                    setAnimData(AIM_DIR_Z, 0.0);
                }
            } else {
                Vec3 visiblePoint = getVisibleTargetPoint(target);
                if (visiblePoint == null) {
                    targetLostTicks++;
                    if (targetLostTicks > 20) {
                        if (base.isThriftyMode()) {
                            base.cancelReservation(target.getId());
                        }
                        target = null;
                        visibleTargetPoint = null;
                        setAnimData(HAS_TARGET, false);
                    setAnimData(AIM_DIR_X, 0.0);
                    setAnimData(AIM_DIR_Y, 0.0);
                    setAnimData(AIM_DIR_Z, 0.0);
                }
                } else {
                    targetLostTicks = 0;
                    visibleTargetPoint = visiblePoint;
                    setAnimData(TARGET_POS_X, visiblePoint.x);
                    setAnimData(TARGET_POS_Y, visiblePoint.y);
                    setAnimData(TARGET_POS_Z, visiblePoint.z);
                    setAnimData(HAS_TARGET, true);
                    syncAimDirectionData(pos, getBlockState().getValue(JunkTurretBlock.FACING));
                }
            }
        }
    }

    /**
     * 检查是否可以射击
     */
    private boolean canShoot(TurretBaseBlockEntity base) {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof JunkTurretBlock)) return false;
        Direction facing = state.getValue(JunkTurretBlock.FACING);
        int energyCost = base.getEnergyCostForFace(facing, Config.junkTurretEnergyCost);

        if (base.getEnergyStored() < energyCost) return false;

        // 垃圾炮塔接受任意物品作弹药
        return hasAnyAmmo(base);
    }

    /**
     * 检查弹药槽是否有任意物品
     */
    private boolean hasAnyAmmo(TurretBaseBlockEntity base) {
        IItemHandler ammoInv = base.getAmmoInventory();
        for (int i = 0; i < ammoInv.getSlots(); i++) {
            ItemStack stack = ammoInv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取任意弹药物品
     */
    private ItemStack findAnyAmmo(TurretBaseBlockEntity base) {
        IItemHandler ammoInv = base.getAmmoInventory();
        for (int i = 0; i < ammoInv.getSlots(); i++) {
            ItemStack stack = ammoInv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 消耗一个弹药物品
     */
    private void consumeAmmo(ItemStack ammoStack) {
        ammoStack.shrink(1);
    }

    /**
     * 执行射击
     */
    private void shoot(Level level, BlockPos pos, BlockState state, TurretBaseBlockEntity base) {
        Direction facing = state.getValue(JunkTurretBlock.FACING);
        int energyCost = base.getEnergyCostForFace(facing, Config.junkTurretEnergyCost);
        if (base.getEnergyStored() < energyCost) return;

        ItemStack ammoStack = findAnyAmmo(base);
        if (ammoStack.isEmpty()) return;

        if (!(level instanceof ServerLevel)) return;

        Vec3 muzzlePos = calculateMuzzlePosition(pos, facing);
        Vec3 targetPos = (visibleTargetPoint != null)
            ? visibleTargetPoint
            : getBallisticTargetPoint(target);

        // 预判瞄准
        if (base.isPredictiveAiming() && target != null) {
            double dist = muzzlePos.distanceTo(targetPos);
            double time = dist / getBulletSpeed();
            Vec3 movement = target.getDeltaMovement();
            // 只做水平预判，避免实体重力导致Y轴瞄准点被压到地面
            targetPos = targetPos.add(movement.x * time, 0.0, movement.z * time);
        }

        Vec3 velocity = calculateParabolicVelocity(muzzlePos, targetPos);

        base.consumeEnergy(energyCost);

        Level baseLevel = base.getLevel();
        if (baseLevel != null && base.hasAmmoRecyclingPlugin()) {
            if (baseLevel.random.nextFloat() >= Config.ammoRecycleChance) {
                consumeAmmo(ammoStack);
            }
        } else {
            consumeAmmo(ammoStack);
        }

        float damage = base.getDamageForFace(facing, getBulletDamage());

        JunkProjectileEntity junk = new JunkProjectileEntity(level, muzzlePos.x, muzzlePos.y, muzzlePos.z, damage);
        junk.setOwner(null);
        junk.setSourcePos(pos);
        junk.setMaxTravelDistance(base.getSearchRadiusForFace(facing, getSearchRadius()) * 1.5D);
        junk.setBasePos(pos.relative(facing.getOpposite()));
        junk.setAmmoItem(ammoStack.copy());
        junk.setDeltaMovement(velocity);

        boolean spawned = level.addFreshEntity(junk);
        if (!spawned) {
            LOGGER.warn("Failed to spawn junk projectile at {}", muzzlePos);
        }

        level.playSound(null, pos, SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 0.5F, 0.8F);
    }

    /**
     * 计算抛物线初速度（平滑曲线）
     */
    private Vec3 calculateParabolicVelocity(Vec3 start, Vec3 end) {
        Vec3 diff = end.subtract(start);
        double horizontalDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        if (horizontalDist < 0.1) {
            return new Vec3(0, getBulletSpeed(), 0);
        }

        double heightDiff = diff.y;

        // 距离越远弧线越高，但整体保持低抛
        double arcFactor = 1.0 - Math.exp(-horizontalDist / 8.0);
        double arcHeight = getMinArcHeight() + (getMaxArcHeight() - getMinArcHeight()) * arcFactor;
        // 目标较高时保证最小越顶余量
        arcHeight = Math.max(arcHeight, heightDiff + 0.6);

        double riseTime = Math.sqrt(2 * arcHeight / getGravity());
        double fallHeight = arcHeight - heightDiff;
        if (fallHeight < 0) fallHeight = 0.3;
        double fallTime = Math.sqrt(2 * fallHeight / getGravity());
        double totalTime = riseTime + fallTime;

        double vx = diff.x / totalTime;
        double vz = diff.z / totalTime;
        double vy = getGravity() * riseTime;

        if (vy < 0.2) vy = 0.2;

        return new Vec3(vx, vy, vz);
    }

    /**
     * 统一弹道瞄准点（目标躯干中部偏上）
     */
    private Vec3 getBallisticTargetPoint(LivingEntity targetEntity) {
        return targetEntity.position().add(0, targetEntity.getBbHeight() * 0.55, 0);
    }

    /**
     * 检查抛物线是否可达
     */
    private boolean canReachWithParabola(LivingEntity targetEntity, Level level, BlockPos pos, double maxRange) {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof JunkTurretBlock)) return false;

        Direction facing = state.getValue(JunkTurretBlock.FACING);
        Vec3 start = calculateMuzzlePosition(pos, facing);
        Vec3 end = getBallisticTargetPoint(targetEntity);

        Vec3 diff = end.subtract(start);
        double horizontalDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        if (horizontalDist > maxRange) return false;

        Vec3 velocity = calculateParabolicVelocity(start, end);
        Vec3 currentPos = start;
        final double hitThresholdSqr = 1.3 * 1.3;

        for (int tick = 0; tick < 100; tick++) {
            Vec3 nextPos = currentPos.add(velocity);

            if (pointToSegmentDistanceSqr(end, currentPos, nextPos) <= hitThresholdSqr) {
                return true;
            }

            BlockHitResult hitResult = level.clip(new ClipContext(
                currentPos,
                nextPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
            ));

            if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                return false;
            }

            currentPos = nextPos;
            velocity = new Vec3(velocity.x, velocity.y - getGravity(), velocity.z);

            if (currentPos.y < level.getMinBuildHeight() || currentPos.y > level.getMaxBuildHeight()) {
                return false;
            }
        }

        return false;
    }

    /**
     * 点到线段最短距离平方
     */
    private double pointToSegmentDistanceSqr(Vec3 point, Vec3 segStart, Vec3 segEnd) {
        Vec3 seg = segEnd.subtract(segStart);
        double segLenSqr = seg.lengthSqr();
        if (segLenSqr < 1.0E-8) {
            return point.distanceToSqr(segStart);
        }
        double t = point.subtract(segStart).dot(seg) / segLenSqr;
        t = Math.max(0.0, Math.min(1.0, t));
        Vec3 projection = segStart.add(seg.scale(t));
        return point.distanceToSqr(projection);
    }

    /**
     * 获取预期伤害（用于厉行节约）
     */
    public float getExpectedDamage(TurretBaseBlockEntity base) {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof JunkTurretBlock)) return getBulletDamage();
        Direction facing = state.getValue(JunkTurretBlock.FACING);
        return base.getDamageForFace(facing, getBulletDamage());
    }

    /**
     * 计算炮口位置
     */
    private Vec3 calculateMuzzlePosition(BlockPos pos, Direction facing) {
        double outwardOffset = 0.3;

        Vec3 center = new Vec3(
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5
        );

        if (facing == Direction.UP) {
            center = new Vec3(center.x, center.y + outwardOffset, center.z);
        } else if (facing == Direction.DOWN) {
            center = new Vec3(center.x, center.y - outwardOffset, center.z);
        } else {
            Vec3 outward = new Vec3(facing.getStepX(), 0, facing.getStepZ()).scale(outwardOffset);
            center = center.add(outward);
        }

        return center;
    }

    /**
     * 搜索目标
     */
    private LivingEntity findTarget(Level level, BlockPos pos, double searchRadius) {
        AABB searchArea = new AABB(
            pos.getX() - searchRadius, pos.getY() - searchRadius, pos.getZ() - searchRadius,
            pos.getX() + searchRadius, pos.getY() + searchRadius, pos.getZ() + searchRadius
        );

        List<LivingEntity> enemies = level.getEntitiesOfClass(
            LivingEntity.class,
            searchArea,
            entity -> isValidTarget(entity, level, pos)
        );

        if (enemies.isEmpty()) {
            setAnimData(HAS_TARGET, false);
            return null;
        }

        Vec3 turretPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        LivingEntity closest = enemies.stream()
            .min(Comparator.comparingDouble(e -> e.distanceToSqr(turretPos)))
            .orElse(null);

        if (closest != null) {
            Vec3 visiblePoint = getVisibleTargetPoint(closest);
            if (visiblePoint != null) {
                visibleTargetPoint = visiblePoint;
                setAnimData(TARGET_POS_X, visiblePoint.x);
                setAnimData(TARGET_POS_Y, visiblePoint.y);
                setAnimData(TARGET_POS_Z, visiblePoint.z);
                    setAnimData(HAS_TARGET, true);
                    syncAimDirectionData(pos, getBlockState().getValue(JunkTurretBlock.FACING));
            } else {
                setAnimData(HAS_TARGET, false);
                    setAnimData(AIM_DIR_X, 0.0);
                    setAnimData(AIM_DIR_Y, 0.0);
                    setAnimData(AIM_DIR_Z, 0.0);
                }
        }

        return closest;
    }

    /**
     * 检查是否为有效目标
     */
    private boolean isValidTarget(LivingEntity entity, Level level, BlockPos pos) {
        TurretBaseBlockEntity base = getBaseEntity();
        if (base == null) {
            return false;
        }

        Direction facing = getBlockState().getValue(JunkTurretBlock.FACING);
        double searchRadius = base.getSearchRadiusForFace(facing, getSearchRadius());
        if (!TurretTargetFilterHelper.passesCommonChecks(entity, base, pos, searchRadius)) {
            return false;
        }

        if (TurretTargetFilterHelper.shouldSkipForThrifty(entity, base)) {
            return false;
        }

        return canReachWithParabola(entity, level, pos, searchRadius);
    }

    private boolean isTargetInRange(LivingEntity target, BlockPos pos, double range) {
        double dist = target.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return dist <= range * range;
    }

    /**
     * 垃圾炮塔使用与发射一致的弹道瞄准点
     */
    private Vec3 getVisibleTargetPoint(LivingEntity target) {
        return getBallisticTargetPoint(target);
    }

    /**
     * 计算炮塔抛物线瞄准方向（初速度方向）
     */
    private Vec3 getAimDirection(BlockPos pos, Direction facing) {
        if (visibleTargetPoint == null) return new Vec3(0, 0, 0);

        Vec3 muzzlePos = calculateMuzzlePosition(pos, facing);
        Vec3 velocity = calculateParabolicVelocity(muzzlePos, visibleTargetPoint);
        return velocity.normalize();
    }

    /**
     * 同步抛物线瞄准方向到客户端动画
     */
    private void syncAimDirectionData(BlockPos pos, Direction facing) {
        Vec3 aimDirection = getAimDirection(pos, facing);
        setAnimData(AIM_DIR_X, aimDirection.x);
        setAnimData(AIM_DIR_Y, aimDirection.y);
        setAnimData(AIM_DIR_Z, aimDirection.z);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        TurretOwnerHelper.saveOwnerNameFromBase(tag, getBaseEntity());
        tag.putInt("Cooldown", cooldown);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        cooldown = tag.getInt("Cooldown");
    }
}
