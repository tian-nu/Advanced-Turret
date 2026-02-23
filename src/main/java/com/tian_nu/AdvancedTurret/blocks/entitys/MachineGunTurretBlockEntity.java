package com.tian_nu.AdvancedTurret.blocks.entitys;

import com.tian_nu.AdvancedTurret.Config;
import com.tian_nu.AdvancedTurret.blocks.MachineGunTurretBlock;
import com.tian_nu.AdvancedTurret.entity.TurretBulletEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.network.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.List;

/**
 * 机枪炮塔方块实体
 *
 * <p>放置在炮塔基座上，使用基座的能量进行射击</p>
 * <p>使用GeckoLib进行动画渲染</p>
 *
 * @author tian_nu
 */
public class MachineGunTurretBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    // ========== 常量 ==========

    /** 射击间隔 (tick) */
    public static final int FIRE_RATE = 5;
    /** 搜索范围 */
    public static final double SEARCH_RADIUS = 16.0;
    /** 子弹速度 */
    public static final double BULLET_SPEED = 3.0;
    /** 子弹伤害 */
    public static final float BULLET_DAMAGE = 4.0F;

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
    
    /** 当前yaw角度（弧度） */
    public float yRot0 = 0.0f;
    /** 当前pitch角度（弧度） */
    public float xRot0 = 0.0f;

    public MachineGunTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_GUN_TURRET.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineGunTurretBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        TurretBaseBlockEntity base = blockEntity.getBaseEntity();
        if (base == null) return;

        if (blockEntity.cooldown > 0) {
            blockEntity.cooldown--;
        }

        blockEntity.updateTarget(level, pos);

        if (blockEntity.target != null && blockEntity.cooldown <= 0) {
            if (blockEntity.canShoot(base)) {
                blockEntity.shoot(level, pos, state, base);
                blockEntity.cooldown = FIRE_RATE;
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
        if (!(state.getBlock() instanceof MachineGunTurretBlock)) return null;

        Direction facing = state.getValue(MachineGunTurretBlock.FACING);
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
    private void updateTarget(Level level, BlockPos pos) {
        if (target == null || !isValidTarget(target, level, pos)) {
            target = findTarget(level, pos);
            targetLostTicks = 0;
        } else {
            if (!isTargetInRange(target, pos)) {
                targetLostTicks++;
                if (targetLostTicks > 20) {
                    target = null;
                    setAnimData(HAS_TARGET, false);
                }
            } else {
                targetLostTicks = 0;
                if (target != null) {
                    Vec3 targetPos = target.position().add(0, target.getEyeHeight() * 0.5, 0);
                    setAnimData(TARGET_POS_X, targetPos.x);
                    setAnimData(TARGET_POS_Y, targetPos.y);
                    setAnimData(TARGET_POS_Z, targetPos.z);
                    setAnimData(HAS_TARGET, true);
                }
            }
        }
    }

    /**
     * 检查是否可以射击
     */
    private boolean canShoot(TurretBaseBlockEntity base) {
        int energyCost = Config.machineGunEnergyCost;
        return base.getEnergyStored() >= energyCost;
    }

    /**
     * 执行射击
     */
    private void shoot(Level level, BlockPos pos, BlockState state, TurretBaseBlockEntity base) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        Direction facing = state.getValue(MachineGunTurretBlock.FACING);

        Vec3 muzzlePos = calculateMuzzlePosition(pos, facing);

        Vec3 targetPos = target.position().add(0, target.getEyeHeight() * 0.5, 0);

        Vec3 direction = targetPos.subtract(muzzlePos).normalize();

        int energyCost = Config.machineGunEnergyCost;
        base.getEnergyStorage().extractEnergy(energyCost, false);
        base.setChanged();
        base.syncToClient();

        TurretBulletEntity bullet = new TurretBulletEntity(level, muzzlePos.x, muzzlePos.y, muzzlePos.z, BULLET_DAMAGE);
        bullet.setOwner(null);
        bullet.shoot(direction, (float) BULLET_SPEED);

        boolean spawned = level.addFreshEntity(bullet);
        if (!spawned) {
            LOGGER.warn("Failed to spawn turret bullet at {}", muzzlePos);
        }

        level.playSound(null, pos, SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 1.0F, 1.5F);

        serverLevel.sendParticles(
                ParticleTypes.SMOKE,
                muzzlePos.x, muzzlePos.y, muzzlePos.z,
                3,
                direction.x * 0.1, direction.y * 0.1, direction.z * 0.1,
                0.1
        );
    }

    /**
     * 计算炮口位置
     */
    private Vec3 calculateMuzzlePosition(BlockPos pos, Direction facing) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        return switch (facing) {
            case UP -> new Vec3(x, pos.getY() + 0.8, z);
            case DOWN -> new Vec3(x, pos.getY() + 0.2, z);
            case NORTH -> new Vec3(x, y, pos.getZ() + 0.2);
            case SOUTH -> new Vec3(x, y, pos.getZ() + 0.8);
            case EAST -> new Vec3(pos.getX() + 0.8, y, z);
            case WEST -> new Vec3(pos.getX() + 0.2, y, z);
        };
    }

    /**
     * 搜索目标
     */
    private LivingEntity findTarget(Level level, BlockPos pos) {
        AABB searchArea = new AABB(
                pos.getX() - SEARCH_RADIUS, pos.getY() - SEARCH_RADIUS, pos.getZ() - SEARCH_RADIUS,
                pos.getX() + SEARCH_RADIUS, pos.getY() + SEARCH_RADIUS, pos.getZ() + SEARCH_RADIUS
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
            Vec3 targetPos = closest.position().add(0, closest.getEyeHeight() * 0.5, 0);
            setAnimData(TARGET_POS_X, targetPos.x);
            setAnimData(TARGET_POS_Y, targetPos.y);
            setAnimData(TARGET_POS_Z, targetPos.z);
            setAnimData(HAS_TARGET, true);
        }
        
        return closest;
    }

    /**
     * 检查是否为有效目标
     */
    private boolean isValidTarget(LivingEntity entity, Level level, BlockPos pos) {
        if (!(entity instanceof Enemy) && !(entity instanceof Mob mob && mob.isAggressive())) {
            return false;
        }

        if (!entity.isAlive()) {
            return false;
        }

        if (!isTargetInRange(entity, pos)) {
            return false;
        }

        if (!hasLineOfSight(entity, level, pos)) {
            return false;
        }

        return true;
    }

    /**
     * 检查目标是否在范围内
     */
    private boolean isTargetInRange(LivingEntity entity, BlockPos pos) {
        Vec3 turretPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return entity.distanceToSqr(turretPos) <= SEARCH_RADIUS * SEARCH_RADIUS;
    }

    /**
     * 检查是否有视线
     */
    private boolean hasLineOfSight(LivingEntity entity, Level level, BlockPos pos) {
        Direction facing = getBlockState().getValue(MachineGunTurretBlock.FACING);
        Vec3 start = calculateMuzzlePosition(pos, facing);
        Vec3 end = entity.position().add(0, entity.getEyeHeight() * 0.5, 0);

        return level.clip(new net.minecraft.world.level.ClipContext(
                start, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                null
        )).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
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
