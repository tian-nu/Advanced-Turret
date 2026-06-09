package com.tian_nu.AdvancedTurret.blocks.entitys;

import com.tian_nu.AdvancedTurret.Config;
import com.tian_nu.AdvancedTurret.blocks.LaserTurretBlock;
import com.tian_nu.AdvancedTurret.items.SmartChipItem;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
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
 * 激光炮塔方块实体。
 *
 * <p>特性：</p>
 * <ul>
 *   <li>无弹药消耗，纯能量驱动</li>
 *   <li>每 tick 持续伤害目标</li>
 *   <li>点燃效果</li>
 *   <li>光束渲染与服务器状态同步</li>
 * </ul>
 *
 * @author tian_nu
 */
public class LaserTurretBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    // ========== 常量 ==========
    /** 每 tick 基础伤害（不含面配置加成）。 */
    public static final float DAMAGE_PER_TICK = 2.0F;
    /** 默认搜索半径（格）。 */
    public static final double SEARCH_RADIUS = 32.0;
    /** 点燃目标的持续时间（秒）。 */
    public static final int FIRE_SECONDS = 3;

    public static float getDamagePerTick() { return (float) Config.laserDamagePerTick; }
    public static double getSearchRadius() { return Config.laserRange; }
    public static int getFireSeconds() { return Config.laserFireSeconds; }
    public static float getAimThreshold() { return (float) Config.laserAimThreshold; }
    public static float getTurnSpeed() { return (float) Config.laserTurnSpeed; }
    /** 瞄准角度阈值（弧度，约15度）。低于此值即视为已瞄准。 */
    public static final float AIM_THRESHOLD = 0.26F;
    /** 转向速度（弧度/tick，约0.18 rad/tick = 约10度/tick = 200度/秒）。 */
    public static final float TURN_SPEED = 0.18F;

    // ========== GeckoLib 同步数据票 ==========
    public static SerializableDataTicket<Boolean> HAS_TARGET;
    public static SerializableDataTicket<Double> TARGET_POS_X;
    public static SerializableDataTicket<Double> TARGET_POS_Y;
    public static SerializableDataTicket<Double> TARGET_POS_Z;
    /** 光束是否激活（用于渲染）。 */
    public static SerializableDataTicket<Boolean> BEAM_ACTIVE;
    /** 已安装的射速组件数量（用于激光透明度效果）。 */
    public static SerializableDataTicket<Integer> FIRE_RATE_COUNT;

    // ========== GeckoLib 动画缓存 ==========
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ========== 字段 ==========
    private LivingEntity target = null;
    private int targetLostTicks = 0;
    private Vec3 visibleTargetPoint = null;

    public float yRot0 = 0.0f;
    public float xRot0 = 0.0f;
    
    /** 旋转目标角度（用于瞄准判断）。 */
    private float targetYRot = 0.0f;
    private float targetXRot = 0.0f;
    /** 炮塔是否已完成对目标的瞄准。 */
    private boolean isAimed = false;

    public LaserTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LASER_TURRET.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LaserTurretBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        TurretBaseBlockEntity base = blockEntity.getBaseEntity();
        if (base == null) return;

        // 能量不足：低电量动画（炮口朝下）。
        if (base.getEnergyStored() < Config.laserEnergyPerTick) {
            blockEntity.target = null;
            blockEntity.isAimed = false;
            blockEntity.setAnimData(HAS_TARGET, true);
            blockEntity.setAnimData(TARGET_POS_X, pos.getX() + 0.5);
            blockEntity.setAnimData(TARGET_POS_Y, pos.getY() - 2.0);
            blockEntity.setAnimData(TARGET_POS_Z, pos.getZ() + 0.5);
            blockEntity.setAnimData(BEAM_ACTIVE, false);
            // 旋转归零（回到默认姿态）。
            blockEntity.yRot0 = blockEntity.lerpAngle(blockEntity.yRot0, 0);
            blockEntity.xRot0 = blockEntity.lerpAngle(blockEntity.xRot0, 0);
            return;
        }

        Direction facing = state.getValue(LaserTurretBlock.FACING);
        if (!base.isFaceEnabled(facing)) {
            blockEntity.target = null;
            blockEntity.isAimed = false;
            blockEntity.setAnimData(HAS_TARGET, false);
            blockEntity.setAnimData(BEAM_ACTIVE, false);
            // 旋转归零（回到默认姿态）。
            blockEntity.yRot0 = blockEntity.lerpAngle(blockEntity.yRot0, 0);
            blockEntity.xRot0 = blockEntity.lerpAngle(blockEntity.xRot0, 0);
            return;
        }

        blockEntity.updateTarget(level, pos, base, facing);

        if (blockEntity.target != null && blockEntity.target.isAlive()) {
            // 根据当前目标位置更新目标角度。
            blockEntity.updateTargetAngles(pos);
            
            // 服务端：通过更新当前角度来模拟旋转过程。
            blockEntity.updateCurrentAngles();
            
            // 检查是否已瞄准目标。
            blockEntity.updateAimedState();
            
            if (blockEntity.isAimed && blockEntity.canHitTarget(blockEntity.target, level, pos)) {
                // 对目标造成伤害。
                blockEntity.dealDamageToTarget(blockEntity.target, base, level);

                // 每 tick 消耗能量。
                base.consumeEnergy(Config.laserEnergyPerTick);

                // 同步光束位置到客户端。
                blockEntity.syncBeamPosition();
                
                // 同步射速组件数量（用于透明度效果）。
                int fireRateCount = blockEntity.countFireRateComponents(base, facing);
                blockEntity.setAnimData(FIRE_RATE_COUNT, fireRateCount);
            } else {
                // 未瞄准时不显示光束。
                blockEntity.setAnimData(BEAM_ACTIVE, false);
            }
            
            // 即使未瞄准也要同步目标位置（用于炮塔旋转动画）。
            if (blockEntity.visibleTargetPoint != null) {
                blockEntity.setAnimData(TARGET_POS_X, blockEntity.visibleTargetPoint.x);
                blockEntity.setAnimData(TARGET_POS_Y, blockEntity.visibleTargetPoint.y);
                blockEntity.setAnimData(TARGET_POS_Z, blockEntity.visibleTargetPoint.z);
                blockEntity.setAnimData(HAS_TARGET, true);
            }
            
            if (!blockEntity.isAimed || !blockEntity.canHitTarget(blockEntity.target, level, pos)) {
                // 目标丢失：未瞄准且目标不可击中。
                // 注意：不取消目标，让炮塔继续旋转追踪。
            }
        } else {
            blockEntity.isAimed = false;
            blockEntity.setAnimData(BEAM_ACTIVE, false);
            // 没有目标：旋转归零（回到默认姿态）。
            blockEntity.yRot0 = blockEntity.lerpAngle(blockEntity.yRot0, 0);
            blockEntity.xRot0 = blockEntity.lerpAngle(blockEntity.xRot0, 0);
        }
    }

    // ========== GeckoLib 动画控制 ==========

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> PlayState.CONTINUE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /**
     * 获取连接的炮塔基座方块实体。
     */
    public TurretBaseBlockEntity getBaseEntity() {
        Level level = getLevel();
        if (level == null) return null;

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof LaserTurretBlock)) return null;

        Direction facing = state.getValue(LaserTurretBlock.FACING);
        BlockPos basePos = worldPosition.relative(facing.getOpposite());

        BlockEntity blockEntity = level.getBlockEntity(basePos);
        if (blockEntity instanceof TurretBaseBlockEntity base) {
            return base;
        }
        return null;
    }

    /**
     * 更新当前目标：验证、寻找新目标或处理目标丢失。
     */
    private void updateTarget(Level level, BlockPos pos, TurretBaseBlockEntity base, Direction facing) {
        if (target == null || !isValidTarget(target, level, pos)) {
            if (target != null && base.isThriftyMode()) {
                base.cancelReservation(target.getId());
            }
            target = findTarget(level, pos, base.getSearchRadiusForFace(facing, getSearchRadius()));
            targetLostTicks = 0;
            // 新目标需要重新瞄准。
            isAimed = false;

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
                    isAimed = false;
                    setAnimData(HAS_TARGET, false);
                    setAnimData(BEAM_ACTIVE, false);
                }
            } else {
                Vec3 visiblePoint = getVisibleTargetPoint(target, level, pos);
                if (visiblePoint == null) {
                    targetLostTicks++;
                    if (targetLostTicks > 20) {
                        if (base.isThriftyMode()) {
                            base.cancelReservation(target.getId());
                        }
                        target = null;
                        visibleTargetPoint = null;
                        isAimed = false;
                        setAnimData(HAS_TARGET, false);
                        setAnimData(BEAM_ACTIVE, false);
                    }
                } else {
                    targetLostTicks = 0;
                    visibleTargetPoint = visiblePoint;
                    // 注意：BEAM_ACTIVE 不在此处设置，由 tick() 中的 isAimed 检查决定。
                }
            }
        }
    }

    /**
     * 检查激光是否能击中目标（视线无方块阻挡）。
     */
    private boolean canHitTarget(LivingEntity target, Level level, BlockPos pos) {
        if (!target.isAlive()) return false;

        Direction facing = getBlockState().getValue(LaserTurretBlock.FACING);
        Vec3 start = calculateMuzzlePosition(pos, facing);
        Vec3 end = target.position().add(0, target.getEyeHeight() * 0.5, 0);

        Vec3 toTarget = end.subtract(start);
        if (toTarget.lengthSqr() < 1.0E-6) {
            return true;
        }

        Vec3 adjustedStart = start.add(
                facing.getStepX() * 0.65D,
                facing.getStepY() * 0.65D,
                facing.getStepZ() * 0.65D
        );
        net.minecraft.world.phys.BlockHitResult hitResult = level.clip(new net.minecraft.world.level.ClipContext(
                adjustedStart, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                null
        ));

        return hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }


    /**
     * 对目标造成魔法伤害并附加点燃效果。
     */
    private void dealDamageToTarget(LivingEntity target, TurretBaseBlockEntity base, Level level) {
        // 清除无敌帧，使激光能造成持续伤害。
        target.invulnerableTime = 0;
        target.hurtTime = 0;

        // 计算伤害：基础伤害 + 面配置加成。
        Direction facing = getBlockState().getValue(LaserTurretBlock.FACING);
        float damage = base.getDamageForFace(facing, getDamagePerTick());

        // 造成魔法伤害。
        target.hurt(level.damageSources().magic(), damage);

        // 点燃效果：每 20 tick 刷新一次。
        if (level.getGameTime() % 20 == 0) {
            target.setSecondsOnFire(getFireSeconds());
        }
    }

    /**
     * 将光束激活状态同步到客户端用于渲染。
     */
    private void syncBeamPosition() {
        setAnimData(BEAM_ACTIVE, true);
    }

    /**
     * 获取每 tick 预期伤害（用于节约模式的预扣计算）。
     */
    public float getExpectedDamage(TurretBaseBlockEntity base) {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof LaserTurretBlock)) return getDamagePerTick();
        Direction facing = state.getValue(LaserTurretBlock.FACING);
        return base.getDamageForFace(facing, getDamagePerTick());
    }
    
    /**
     * 根据可见目标点更新旋转目标角度。
     */
    private void updateTargetAngles(BlockPos pos) {
        if (visibleTargetPoint == null) return;
        
        Direction facing = getBlockState().getValue(LaserTurretBlock.FACING);
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vec3 delta = new Vec3(visibleTargetPoint.x - center.x, visibleTargetPoint.y - center.y, visibleTargetPoint.z - center.z);
        
        // 根据炮塔朝向变换坐标系。
        double dx = delta.x, dy = delta.y, dz = delta.z;
        switch (facing) {
            case NORTH -> { dz = -delta.y; dy = -delta.z; }
            case SOUTH -> { dz = delta.y; dy = delta.z; }
            case EAST -> { dx = -delta.y; dy = delta.x; }
            case WEST -> { dx = delta.y; dy = -delta.x; }
            case DOWN -> { dy = -dy; }
            case UP -> {}
        }
        
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        targetYRot = (float) -Math.atan2(dx, dz);
        targetXRot = (float) -Math.atan2(dy, horizontalDist);
        
        // UP、EAST、WEST 方向需要特殊角度处理。
        if (facing == Direction.UP || facing == Direction.EAST || facing == Direction.WEST) {
            targetYRot += (float) Math.PI;
        }
    }
    
    /**
     * 更新瞄准状态：检查当前角度是否接近目标角度。
     */
    private void updateAimedState() {
        // 计算角度差（弧度）。
        float yRotDiff = Math.abs(normalizeAngle(targetYRot - yRot0));
        float xRotDiff = Math.abs(normalizeAngle(targetXRot - xRot0));
        
        // 两个角度均低于阈值才算瞄准完成。
        isAimed = yRotDiff < getAimThreshold() && xRotDiff < getAimThreshold();
    }
    
    /**
     * 将角度归一化到 [-PI, PI] 范围内。
     */
    private float normalizeAngle(float angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }
    
    /**
     * 更新当前旋转角度（服务端旋转模拟）。
     */
    private void updateCurrentAngles() {
        // 使用与 GeoModel 相同的 lerp 逻辑实现平滑旋转。
        yRot0 = lerpAngle(yRot0, targetYRot);
        xRot0 = lerpAngle(xRot0, targetXRot);
    }
    
    /**
     * 固定速度的角度插值。
     * 每 tick 向目标角度移动 TURN_SPEED 弧度。
     */
    private float lerpAngle(float current, float target) {
        float diff = normalizeAngle(target - current);
        if (Math.abs(diff) < getTurnSpeed()) {
            return target; // 足够接近：直接对齐到目标角度。
        }
        return current + Math.signum(diff) * getTurnSpeed();
    }

    /**
     * 根据炮塔朝向计算炮口位置，用于射线检测。
     */
    public Vec3 calculateMuzzlePosition(BlockPos pos, Direction facing) {
        double outwardOffset = 0;
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

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
     * 在搜索半径内查找有效目标。
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
            setAnimData(BEAM_ACTIVE, false);
            isAimed = false;
            return null;
        }

        Vec3 turretPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        LivingEntity closest = enemies.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(turretPos)))
                .orElse(null);

        if (closest != null) {
            Vec3 visiblePoint = getVisibleTargetPoint(closest, level, pos);
            if (visiblePoint != null) {
                visibleTargetPoint = visiblePoint;
                setAnimData(TARGET_POS_X, visiblePoint.x);
                setAnimData(TARGET_POS_Y, visiblePoint.y);
                setAnimData(TARGET_POS_Z, visiblePoint.z);
                setAnimData(HAS_TARGET, true);
                // 新目标需要重新瞄准。
                isAimed = false;
                setAnimData(BEAM_ACTIVE, false);
            } else {
                setAnimData(HAS_TARGET, false);
                setAnimData(BEAM_ACTIVE, false);
                isAimed = false;
            }
        }

        return closest;
    }

    /**
     * 检查给定实体是否为激光炮塔的有效目标。
     */
    private boolean isValidTarget(LivingEntity entity, Level level, BlockPos pos) {
        TurretBaseBlockEntity base = getBaseEntity();
        if (base == null) {
            return false;
        }

        Direction facing = getBlockState().getValue(LaserTurretBlock.FACING);
        double searchRadius = base.getSearchRadiusForFace(facing, getSearchRadius());
        if (!TurretTargetFilterHelper.passesCommonChecks(entity, base, pos, searchRadius)) {
            return false;
        }

        // 获取可见瞄准点（优先：头部 > 身体 > 脚部）
        Vec3 visiblePoint = getVisibleTargetPoint(entity, level, pos);
        if (visiblePoint == null) {
            return false;
        }
        visibleTargetPoint = visiblePoint;

        return !TurretTargetFilterHelper.shouldSkipForThrifty(entity, base);
    }

    private boolean isTargetInRange(LivingEntity entity, BlockPos pos, double searchRadius) {
        return LinearTurretTargetingHelper.isTargetInRange(entity, pos, searchRadius);
    }

    private Vec3 getVisibleTargetPoint(LivingEntity entity, Level level, BlockPos pos) {
        Direction facing = getBlockState().getValue(LaserTurretBlock.FACING);
        Vec3 start = calculateMuzzlePosition(pos, facing);
        return LinearTurretTargetingHelper.findVisibleTargetPoint(level, pos, facing, start, entity);
    }

    private boolean canSeePoint(Level level, BlockPos pos, Vec3 start, Vec3 end) {
        Direction facing = getBlockState().getValue(LaserTurretBlock.FACING);
        return LinearTurretTargetingHelper.canSeePoint(level, pos, facing, start, end);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("YRot0", yRot0);
        tag.putFloat("XRot0", xRot0);
        tag.putFloat("TargetYRot", targetYRot);
        tag.putFloat("TargetXRot", targetXRot);
        tag.putBoolean("IsAimed", isAimed);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("YRot0")) yRot0 = tag.getFloat("YRot0");
        if (tag.contains("XRot0")) xRot0 = tag.getFloat("XRot0");
        if (tag.contains("TargetYRot")) targetYRot = tag.getFloat("TargetYRot");
        if (tag.contains("TargetXRot")) targetXRot = tag.getFloat("TargetXRot");
        if (tag.contains("IsAimed")) isAimed = tag.getBoolean("IsAimed");
    }

    /**
     * 统计指定面上已安装的射速升级组件数量。
     * 用于激光光束透明度效果。
     */
    private int countFireRateComponents(TurretBaseBlockEntity base, Direction facing) {
        net.minecraftforge.items.IItemHandler upgrades = base.getFaceUpgradeSlots(facing);
        int count = 0;
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack stack = upgrades.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(com.tian_nu.AdvancedTurret.items.ModItems.FIRE_RATE_COMPONENT.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
