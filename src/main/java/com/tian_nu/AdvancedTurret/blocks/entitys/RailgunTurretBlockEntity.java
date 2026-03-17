package com.tian_nu.AdvancedTurret.blocks.entitys;

import com.mojang.logging.LogUtils;
import com.tian_nu.AdvancedTurret.Config;
import com.tian_nu.AdvancedTurret.blocks.RailgunTurretBlock;
import com.tian_nu.AdvancedTurret.entity.RailgunBulletEntity;
import com.tian_nu.AdvancedTurret.items.ModItems;
import com.tian_nu.AdvancedTurret.items.SmartChipItem;

import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

public class RailgunTurretBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int FIRE_RATE = 100;
    public static final double SEARCH_RADIUS = 64.0;
    public static final double BULLET_SPEED = 6.0;
    public static final float BULLET_DAMAGE = 60.0F;
    public static final int ENERGY_COST = 10000;

    public static int getFireRate() { return Config.railgunFireRate; }
    public static double getSearchRadius() { return Config.railgunRange; }
    public static float getBulletDamage() { return (float) Config.railgunDamage; }
    public static int getPenetrationCount() { return Config.railgunPenetrationCount; }
    public static float getBulletSpeed() { return (float) Config.railgunBulletSpeed; }

    public static SerializableDataTicket<Boolean> HAS_TARGET;
    public static SerializableDataTicket<Double> TARGET_POS_X;
    public static SerializableDataTicket<Double> TARGET_POS_Y;
    public static SerializableDataTicket<Double> TARGET_POS_Z;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int cooldown = 0;
    private LivingEntity target = null;
    private int targetLostTicks = 0;
    /** 当前目标的可见瞄准点（用于射击实际可见部位） */
    private Vec3 visibleTargetPoint = null;

    public float yRot0 = 0.0f;
    public float xRot0 = 0.0f;

    public RailgunTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RAILGUN_TURRET.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RailgunTurretBlockEntity blockEntity) {
        if (level.isClientSide) return;

        TurretBaseBlockEntity base = blockEntity.getBaseEntity();
        if (base == null) return;

        // 无电量低头动画提示
        if (base.getEnergyStored() == 0) {
            blockEntity.target = null;
            // 设置低头动画（目标位置在炮塔下方）
            blockEntity.setAnimData(HAS_TARGET, true);
            blockEntity.setAnimData(TARGET_POS_X, pos.getX() + 0.5);
            blockEntity.setAnimData(TARGET_POS_Y, pos.getY() - 2.0);  // 向下看
            blockEntity.setAnimData(TARGET_POS_Z, pos.getZ() + 0.5);
            return;
        }

        Direction facing = state.getValue(RailgunTurretBlock.FACING);
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
            if (blockEntity.canShoot(base, facing)) {
                blockEntity.shoot(level, pos, state, base, facing);
                blockEntity.cooldown = base.getFireRateForFace(facing, getFireRate());
            }
        }
    }

    public TurretBaseBlockEntity getBaseEntity() {
        Level level = getLevel();
        if (level == null) return null;

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof RailgunTurretBlock)) return null;

        Direction facing = state.getValue(RailgunTurretBlock.FACING);
        BlockPos basePos = worldPosition.relative(facing.getOpposite());

        BlockEntity blockEntity = level.getBlockEntity(basePos);
        if (blockEntity instanceof TurretBaseBlockEntity base) {
            return base;
        }
        return null;
    }

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

	private boolean canShoot(TurretBaseBlockEntity base, Direction facing) {
		int energyCost = base.getEnergyCostForFace(facing, Config.railgunEnergyCost);
		if (base.getEnergyStored() < energyCost) {
			return false;
		}
		return hasAmmo(base);
	}

	private boolean hasAmmo(TurretBaseBlockEntity base) {
		net.minecraftforge.items.IItemHandler ammoInv = base.getAmmoInventory();
		for (int i = 0; i < ammoInv.getSlots(); i++) {
			ItemStack stack = ammoInv.getStackInSlot(i);
			if (!stack.isEmpty() && stack.is(ModItems.RAILGUN_BULLET.get())) {
				return true;
			}
		}
		return false;
	}

	private void consumeAmmo(TurretBaseBlockEntity base) {
		net.minecraftforge.items.IItemHandler ammoInv = base.getAmmoInventory();
		for (int i = 0; i < ammoInv.getSlots(); i++) {
			ItemStack stack = ammoInv.getStackInSlot(i);
			if (!stack.isEmpty() && stack.is(ModItems.RAILGUN_BULLET.get())) {
				stack.shrink(1);
				return;
			}
		}
	}

	private void shoot(Level level, BlockPos pos, BlockState state, TurretBaseBlockEntity base, Direction facing) {
		int energyCost = base.getEnergyCostForFace(facing, Config.railgunEnergyCost);
		if (base.getEnergyStored() < energyCost) return;
		if (!hasAmmo(base)) return;
		if (!(level instanceof ServerLevel)) return;


		Vec3 muzzlePos = calculateMuzzlePosition(pos, facing);

        // 使用可见瞄准点（如果有的话），否则回退到目标中心
        Vec3 targetPos = (visibleTargetPoint != null) 
            ? visibleTargetPoint 
            : target.position().add(0, target.getEyeHeight() * 0.5, 0);

        if (base.isPredictiveAiming()) {
            double dist = muzzlePos.distanceTo(targetPos);
            double time = dist / getBulletSpeed();
            targetPos = targetPos.add(target.getDeltaMovement().scale(time));
        }

        Vec3 direction = targetPos.subtract(muzzlePos).normalize();

		// 消耗能量
		base.consumeEnergy(energyCost);

		// 消耗弹药（弹药回收插件：按配置概率不消耗）
		Level baseLevel = base.getLevel();
		if (baseLevel != null && base.hasAmmoRecyclingPlugin()) {
			if (baseLevel.random.nextFloat() >= Config.ammoRecycleChance) {
				consumeAmmo(base);
			}
		} else {
			consumeAmmo(base);
		}

		float damage = base.getDamageForFace(facing, getBulletDamage());
		RailgunBulletEntity bullet = new RailgunBulletEntity(level, muzzlePos.x, muzzlePos.y, muzzlePos.z, damage);
        bullet.setOwner(null);
        bullet.setSourcePos(pos);
        bullet.setMaxTravelDistance(base.getSearchRadiusForFace(facing, getSearchRadius()) * 1.5D);
        bullet.setBasePos(pos.relative(facing.getOpposite())); // 设置基座位置
        bullet.setPenetrationCount(getPenetrationCount()); // 穿透目标数量可配置
        bullet.shoot(direction, getBulletSpeed());

        boolean spawned = level.addFreshEntity(bullet);
        if (!spawned) {
            LOGGER.warn("Failed to spawn railgun bullet at {}", muzzlePos);
        }

level.playSound(null, pos, SoundEvents.CROSSBOW_SHOOT, SoundSource.BLOCKS, 1.0F, 1.0F);

	}

	/**
	 * 获取预期伤害（用于厉行节约）
	 */
	public float getExpectedDamage(TurretBaseBlockEntity base) {
		BlockState state = getBlockState();
		if (!(state.getBlock() instanceof RailgunTurretBlock)) return getBulletDamage();
		Direction facing = state.getValue(RailgunTurretBlock.FACING);
		return base.getDamageForFace(facing, getBulletDamage());
	}

    /**
     * 计算炮口位置
     * pos是炮塔方块位置
     * 炮塔模型高度约0.5方块，需要根据朝向计算正确位置
     */
    private Vec3 calculateMuzzlePosition(BlockPos pos, Direction facing) {
        double outwardOffset = 0; // 向外延伸距离（设置为0避免炮口位置偏差）
        
        Vec3 center = new Vec3(
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5
        );
        
        // 对于上下朝向，需要调整Y坐标到模型实际位置
        if (facing == Direction.UP) {
            // 朝上：模型在方块下半部(0-0.5)，炮口在顶部向外延伸
            center = new Vec3(center.x, center.y + outwardOffset, center.z);
        } else if (facing == Direction.DOWN) {
            // 朝下：模型在方块上半部(0.5-1.0)，炮口在底部向外延伸
            center = new Vec3(center.x, center.y - outwardOffset, center.z);
        } else {
            // 水平朝向：模型中心在y=0.5，向面向方向延伸
            Vec3 outward = new Vec3(facing.getStepX(), 0, facing.getStepZ()).scale(outwardOffset);
            center = center.add(outward);
        }
        
        return center;
    }

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

    private boolean isValidTarget(LivingEntity entity, Level level, BlockPos pos) {
        TurretBaseBlockEntity base = getBaseEntity();
        if (base == null) {
            return false;
        }

        Direction facing = getBlockState().getValue(RailgunTurretBlock.FACING);
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
        Direction facing = getBlockState().getValue(RailgunTurretBlock.FACING);
        Vec3 start = calculateMuzzlePosition(pos, facing);
        return LinearTurretTargetingHelper.findVisibleTargetPoint(level, pos, facing, start, entity);
    }

    private boolean canSeePoint(Level level, BlockPos pos, Vec3 start, Vec3 end) {
        Direction facing = getBlockState().getValue(RailgunTurretBlock.FACING);
        return LinearTurretTargetingHelper.canSeePoint(level, pos, facing, start, end);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> PlayState.CONTINUE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
    }
}
