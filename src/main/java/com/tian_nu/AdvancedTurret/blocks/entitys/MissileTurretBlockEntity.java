package com.tian_nu.AdvancedTurret.blocks.entitys;

import com.tian_nu.AdvancedTurret.Config;
import com.tian_nu.AdvancedTurret.blocks.MissileTurretBlock;
import com.tian_nu.AdvancedTurret.entity.MissileEntity;
import com.tian_nu.AdvancedTurret.items.ModItems;
import com.tian_nu.AdvancedTurret.items.SmartChipItem;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
 * 导弹炮塔方块实体
 *
 * <p>特点：</p>
 * <ul>
 *   <li>远程攻击：64格搜索范围</li>
 *   <li>高伤害：直击10 + 爆炸15（半径4格）</li>
 *   <li>低射速：6.7秒/发</li>
 *   <li>高能耗：10000 FE/发</li>
 *   <li>破坏插件：可破坏方块</li>
 * </ul>
 *
 * <p>制导机制：</p>
 * <ul>
 *   <li>导弹自动追踪目标移动</li>
 *   <li>每tick调整朝向，使用转向插值</li>
 *   <li>越飞越快，增加打击距离</li>
 * </ul>
 *
 * @author tian_nu
 */
public class MissileTurretBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    // ========== 常量（来自炮塔数值与机制.md） ==========

    /** 射击间隔 (tick) - 6.7秒 */
    public static final int FIRE_RATE = 200;
    /** 搜索范围 */
    public static final double SEARCH_RADIUS = 64.0;
    /** 子弹初始速度 */
    public static final double BULLET_SPEED = 1.5;
    /** 加速度 (每tick速度增加) */
    public static final double ACCELERATION = 0.03;
    /** 转向速率 (每tick转向插值系数) */
    public static final double TURN_RATE = 0.3;
    /** 直击伤害 */
    public static final float DIRECT_DAMAGE = 10.0F;
    /** 爆炸伤害 */
    public static final float EXPLOSION_DAMAGE = 20.0F;
    /** 爆炸半径 */
    public static final float EXPLOSION_RADIUS = 4.0F;
    /** 能量消耗 */
    public static final int ENERGY_COST = 10000;

    public static int getFireRate() { return Config.missileFireRate; }
    public static double getSearchRadius() { return Config.missileRange; }
    public static float getDirectDamage() { return (float) Config.missileDirectDamage; }
    public static float getExplosionDamage() { return (float) Config.missileExplosionDamage; }
    public static float getExplosionRadius() { return (float) Config.missileExplosionRadius; }
    public static float getBulletSpeed() { return (float) Config.missileBulletSpeed; }
    public static double getAcceleration() { return Config.missileAcceleration; }
    public static double getTurnRate() { return Config.missileTurnRate; }

    // ========== GeckoLib数据同步票 ==========
    public static SerializableDataTicket<Boolean> HAS_TARGET;
    public static SerializableDataTicket<Double> TARGET_POS_X;
    public static SerializableDataTicket<Double> TARGET_POS_Y;
    public static SerializableDataTicket<Double> TARGET_POS_Z;

    // ========== GeckoLib动画缓存 ==========
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ========== 字段 ==========

    private int cooldown = 0;
    private LivingEntity target = null;
    private int targetLostTicks = 0;
    /** 当前目标的可见瞄准点（用于射击实际可见部位） */
    private Vec3 visibleTargetPoint = null;

    /** 当前 yaw 角度（弧度） */
    public float yRot0 = 0.0f;
    /** 当前 pitch 角度（弧度） */
    public float xRot0 = 0.0f;

    public MissileTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MISSILE_TURRET.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MissileTurretBlockEntity blockEntity) {
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
            return;
        }

        Direction facing = state.getValue(MissileTurretBlock.FACING);
        if (!base.isFaceEnabled(facing)) {
            blockEntity.target = null;
            blockEntity.setAnimData(HAS_TARGET, false);
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
        if (!(state.getBlock() instanceof MissileTurretBlock)) return null;

        Direction facing = state.getValue(MissileTurretBlock.FACING);
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
			// 取消旧目标的预约（如果有）
			if (target != null && base.isThriftyMode()) {
				base.cancelReservation(target.getId());
			}
			target = findTarget(level, pos, base.getSearchRadiusForFace(facing, getSearchRadius()));
			targetLostTicks = 0;
			
			// 新目标锁定时预约伤害
			if (target != null && base.isThriftyMode()) {
				float expectedDamage = getExpectedDamage(base);
				base.reserveDamage(target.getId(), expectedDamage, target.getHealth(), level.getGameTime());
			}
		} else {
			// 检查是否超出范围
			if (!isTargetInRange(target, pos, base.getSearchRadiusForFace(facing, getSearchRadius()))) {
				targetLostTicks++;
				if (targetLostTicks > 20) {
					// 目标丢失，取消预约
					if (base.isThriftyMode()) {
						base.cancelReservation(target.getId());
					}
					target = null;
					visibleTargetPoint = null;
					setAnimData(HAS_TARGET, false);
				}
			} else {
				// 目标有效且在范围内，重新计算可见瞄准点并更新动画
				Vec3 visiblePoint = getVisibleTargetPoint(target, level, pos);
				if (visiblePoint == null) {
					// 目标变得不可见，丢失计数
					targetLostTicks++;
					if (targetLostTicks > 20) {
						// 目标丢失，取消预约
						if (base.isThriftyMode()) {
							base.cancelReservation(target.getId());
						}
						target = null;
						visibleTargetPoint = null;
						setAnimData(HAS_TARGET, false);
					}
				} else {
					targetLostTicks = 0;
					visibleTargetPoint = visiblePoint;
					// 更新动画瞄准位置
					setAnimData(TARGET_POS_X, visiblePoint.x);
					setAnimData(TARGET_POS_Y, visiblePoint.y);
					setAnimData(TARGET_POS_Z, visiblePoint.z);
					setAnimData(HAS_TARGET, true);
				}
			}
		}
	}

    /**
     * 检查是否可以射击
     */
    private boolean canShoot(TurretBaseBlockEntity base) {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof MissileTurretBlock)) return false;
        Direction facing = state.getValue(MissileTurretBlock.FACING);
        int energyCost = base.getEnergyCostForFace(facing, com.tian_nu.AdvancedTurret.Config.missileEnergyCost);

        // 检查能量
        if (base.getEnergyStored() < energyCost) return false;

        // 检查弹药
        return hasAmmo(base);
    }

    /**
     * 检查弹药槽是否有导弹
     */
    private boolean hasAmmo(TurretBaseBlockEntity base) {
        net.minecraftforge.items.IItemHandler ammoInv = base.getAmmoInventory();
        for (int i = 0; i < ammoInv.getSlots(); i++) {
            ItemStack stack = ammoInv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(ModItems.MISSILE.get())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 消耗一枚导弹
     */
    private void consumeAmmo(TurretBaseBlockEntity base) {
        net.minecraftforge.items.IItemHandler ammoInv = base.getAmmoInventory();
        for (int i = 0; i < ammoInv.getSlots(); i++) {
            ItemStack stack = ammoInv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(ModItems.MISSILE.get())) {
                stack.shrink(1);
                return;
            }
        }
    }

/**
	 * 执行射击
	 */
	private void shoot(Level level, BlockPos pos, BlockState state, TurretBaseBlockEntity base) {
		Direction facing = state.getValue(MissileTurretBlock.FACING);
		int energyCost = base.getEnergyCostForFace(facing, com.tian_nu.AdvancedTurret.Config.missileEnergyCost);
		if (base.getEnergyStored() < energyCost) return;

		// 检查弹药
		if (!hasAmmo(base)) return;

		if (!(level instanceof ServerLevel serverLevel)) return;

		Vec3 muzzlePos = calculateMuzzlePosition(pos, facing);

        // 使用可见瞄准点（如果有的话），否则回退到目标中心
        Vec3 targetPos = (visibleTargetPoint != null)
            ? visibleTargetPoint
            : target.position().add(0, target.getEyeHeight() * 0.5, 0);

        // 预判瞄准（导弹有制导，预判不太重要，但保留选项）
        if (base.isPredictiveAiming()) {
            double dist = muzzlePos.distanceTo(targetPos);
            double time = dist / getBulletSpeed();
            targetPos = targetPos.add(target.getDeltaMovement().scale(time));
        }

        Vec3 direction = targetPos.subtract(muzzlePos).normalize();

        // 消耗能量
        base.consumeEnergy(energyCost);

        // 消耗弹药（弹药回收插件：20%概率不消耗）
        if (base.hasAmmoRecyclingPlugin()) {
            if (base.getLevel().random.nextFloat() >= com.tian_nu.AdvancedTurret.Config.ammoRecycleChance) {
                consumeAmmo(base);
            }
            // 20%概率不消耗弹药
        } else {
            consumeAmmo(base);
        }

        // 计算伤害（应用升级组件）
        float directDamage = base.getDamageForFace(facing, getDirectDamage());
        float explosionDamage = getExplosionDamage(); // 爆炸伤害不随升级组件变化

        // 创建导弹
        MissileEntity missile = new MissileEntity(level, muzzlePos.x, muzzlePos.y, muzzlePos.z, directDamage);
        missile.setOwner(null);
        missile.setSourcePos(pos);
        missile.setMaxTravelDistance(base.getSearchRadiusForFace(facing, getSearchRadius()) * 1.5D);
        missile.setBasePos(pos.relative(facing.getOpposite()));
        missile.setExplosionDamage(explosionDamage);
        missile.setExplosionRadius(getExplosionRadius());
        missile.setAcceleration(getAcceleration()); // 越飞越快
        missile.setTurnRate(getTurnRate()); // 制导追踪转向速率
        missile.setTargetEntity(target); // 设置追踪目标

        // 破坏插件：决定是否破坏方块
        missile.setDestroyBlocks(base.hasDestructionPlugin());

        missile.shoot(direction, getBulletSpeed());

        boolean spawned = level.addFreshEntity(missile);
        if (!spawned) {
            LOGGER.warn("Failed to spawn missile at {}", muzzlePos);
        }

// 播放射击音效
		level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.5F, 1.0F);
	}

	/**
	 * 获取预期伤害（用于厉行节约）- 直击 + 爆炸
	 */
	public float getExpectedDamage(TurretBaseBlockEntity base) {
		BlockState state = getBlockState();
		if (!(state.getBlock() instanceof MissileTurretBlock)) return getDirectDamage() + getExplosionDamage();
		Direction facing = state.getValue(MissileTurretBlock.FACING);
		float directDamage = base.getDamageForFace(facing, getDirectDamage());
		return directDamage + getExplosionDamage();
	}

    /**
     * 计算炮口位置
     */
    private Vec3 calculateMuzzlePosition(BlockPos pos, Direction facing) {
        double outwardOffset = 0;

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
            // 计算可见瞄准点
            Vec3 visiblePoint = getVisibleTargetPoint(closest, level, pos);
            if (visiblePoint != null) {
                visibleTargetPoint = visiblePoint;
                setAnimData(TARGET_POS_X, visiblePoint.x);
                setAnimData(TARGET_POS_Y, visiblePoint.y);
                setAnimData(TARGET_POS_Z, visiblePoint.z);
                setAnimData(HAS_TARGET, true);
            } else {
                // 理论上不会发生（isValidTarget 已经检查过）
                setAnimData(HAS_TARGET, false);
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

        Direction facing = getBlockState().getValue(MissileTurretBlock.FACING);
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

    /**
     * 获取目标的可见瞄准点
     * 检测目标头部、身体和脚部，返回第一个可见点的位置
     * 优先顺序：头部 > 身体 > 脚部（头部更致命）
     * @return 可见点位置，如果完全不可见返回 null
     */
    private Vec3 getVisibleTargetPoint(LivingEntity entity, Level level, BlockPos pos) {
        Direction facing = getBlockState().getValue(MissileTurretBlock.FACING);
        Vec3 start = calculateMuzzlePosition(pos, facing);
        return LinearTurretTargetingHelper.findVisibleTargetPoint(level, pos, facing, start, entity);
    }

    private boolean canSeePoint(Level level, BlockPos pos, Vec3 start, Vec3 end) {
        Direction facing = getBlockState().getValue(MissileTurretBlock.FACING);
        return LinearTurretTargetingHelper.canSeePoint(level, pos, facing, start, end);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Cooldown", cooldown);
        tag.putFloat("YRot0", yRot0);
        tag.putFloat("XRot0", xRot0);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Cooldown")) {
            cooldown = tag.getInt("Cooldown");
        }
        if (tag.contains("YRot0")) {
            yRot0 = tag.getFloat("YRot0");
        }
        if (tag.contains("XRot0")) {
            xRot0 = tag.getFloat("XRot0");
        }
    }
}
