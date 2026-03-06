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
 * 激光炮塔方块实体
 *
 * <p>特性：</p>
 * <ul>
 *   <li>无弹药消耗，纯能量驱动</li>
 *   <li>每tick持续伤害目标</li>
 *   <li>点燃效果</li>
 *   <li>光束渲染同步</li>
 * </ul>
 *
 * @author tian_nu
 */
public class LaserTurretBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    // ========== 常量 ==========
    /** 每tick伤害 */
    public static final float DAMAGE_PER_TICK = 2.0F;
    /** 搜索范围 */
    public static final double SEARCH_RADIUS = 32.0;
    /** 点燃时间（秒） */
    public static final int FIRE_SECONDS = 2;

    // ========== GeckoLib数据同步票 ==========
    public static SerializableDataTicket<Boolean> HAS_TARGET;
    public static SerializableDataTicket<Double> TARGET_POS_X;
    public static SerializableDataTicket<Double> TARGET_POS_Y;
    public static SerializableDataTicket<Double> TARGET_POS_Z;
    /** 光束是否激活（用于渲染） */
    public static SerializableDataTicket<Boolean> BEAM_ACTIVE;
    /** 射速加成数量（用于激光透明度） */
    public static SerializableDataTicket<Integer> FIRE_RATE_COUNT;

    // ========== GeckoLib动画缓存 ==========
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ========== 字段 ==========
    private LivingEntity target = null;
    private int targetLostTicks = 0;
    private Vec3 visibleTargetPoint = null;

    public float yRot0 = 0.0f;
    public float xRot0 = 0.0f;

    public LaserTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LASER_TURRET.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LaserTurretBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        TurretBaseBlockEntity base = blockEntity.getBaseEntity();
        if (base == null) return;

        // 无电量低头动画
        if (base.getEnergyStored() < Config.laserEnergyPerTick) {
            blockEntity.target = null;
            blockEntity.setAnimData(HAS_TARGET, true);
            blockEntity.setAnimData(TARGET_POS_X, pos.getX() + 0.5);
            blockEntity.setAnimData(TARGET_POS_Y, pos.getY() - 2.0);
            blockEntity.setAnimData(TARGET_POS_Z, pos.getZ() + 0.5);
            blockEntity.setAnimData(BEAM_ACTIVE, false);
            return;
        }

        Direction facing = state.getValue(LaserTurretBlock.FACING);
        if (!base.isFaceEnabled(facing)) {
            blockEntity.target = null;
            blockEntity.setAnimData(HAS_TARGET, false);
            blockEntity.setAnimData(BEAM_ACTIVE, false);
            return;
        }

        blockEntity.updateTarget(level, pos, base, facing);

        if (blockEntity.target != null && blockEntity.target.isAlive()) {
            if (blockEntity.canHitTarget(blockEntity.target, level, pos)) {
                // 执行伤害
                blockEntity.dealDamageToTarget(blockEntity.target, base, level);

                // 消耗能量
                base.consumeEnergy(Config.laserEnergyPerTick);

                // 同步光束位置
                blockEntity.syncBeamPosition();
                
                // 同步射速组件数量（用于透明度）
                int fireRateCount = blockEntity.countFireRateComponents(base, facing);
                blockEntity.setAnimData(FIRE_RATE_COUNT, fireRateCount);
            } else {
                // 目标丢失
                if (base.isThriftyMode()) {
                    base.cancelReservation(blockEntity.target.getId());
                }
                blockEntity.target = null;
                blockEntity.visibleTargetPoint = null;
                blockEntity.setAnimData(BEAM_ACTIVE, false);
            }
        } else {
            blockEntity.setAnimData(BEAM_ACTIVE, false);
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
     * 更新目标
     */
    private void updateTarget(Level level, BlockPos pos, TurretBaseBlockEntity base, Direction facing) {
        if (target == null || !isValidTarget(target, level, pos)) {
            if (target != null && base.isThriftyMode()) {
                base.cancelReservation(target.getId());
            }
            target = findTarget(level, pos, base.getSearchRadiusForFace(facing, SEARCH_RADIUS));
            targetLostTicks = 0;

            if (target != null && base.isThriftyMode()) {
                float expectedDamage = getExpectedDamage(base);
                base.reserveDamage(target.getId(), expectedDamage, target.getHealth(), level.getGameTime());
            }
        } else {
            if (!isTargetInRange(target, pos, base.getSearchRadiusForFace(facing, SEARCH_RADIUS))) {
                targetLostTicks++;
                if (targetLostTicks > 20) {
                    if (base.isThriftyMode()) {
                        base.cancelReservation(target.getId());
                    }
                    target = null;
                    visibleTargetPoint = null;
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
                        setAnimData(HAS_TARGET, false);
                        setAnimData(BEAM_ACTIVE, false);
                    }
                } else {
                    targetLostTicks = 0;
                    visibleTargetPoint = visiblePoint;
                    setAnimData(TARGET_POS_X, visiblePoint.x);
                    setAnimData(TARGET_POS_Y, visiblePoint.y);
                    setAnimData(TARGET_POS_Z, visiblePoint.z);
                    setAnimData(HAS_TARGET, true);
                    // 目标可见时激活光束
                    setAnimData(BEAM_ACTIVE, true);
                }
            }
        }
    }

    /**
     * 检查目标是否可被击中
     */
    private boolean canHitTarget(LivingEntity target, Level level, BlockPos pos) {
        if (!target.isAlive()) return false;

        Direction facing = getBlockState().getValue(LaserTurretBlock.FACING);
        Vec3 start = calculateMuzzlePosition(pos, facing);
        Vec3 end = target.position().add(0, target.getEyeHeight() * 0.5, 0);

        // 射线检测
        Vec3 outward = end.subtract(start).normalize();
        Vec3 adjustedStart = start.add(outward.scale(0.6));

        net.minecraft.world.phys.BlockHitResult hitResult = level.clip(new net.minecraft.world.level.ClipContext(
                adjustedStart, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                null
        ));

        if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
            return true;
        }

        // 检查是否被基座阻挡
        BlockPos basePos = pos.relative(facing.getOpposite());
        return !hitResult.getBlockPos().equals(basePos);
    }

    /**
     * 对目标造成伤害
     */
    private void dealDamageToTarget(LivingEntity target, TurretBaseBlockEntity base, Level level) {
        // 清除无敌帧
        target.invulnerableTime = 0;
        target.hurtTime = 0;

        // 计算伤害（基础伤害 + 面配置加成）
        Direction facing = getBlockState().getValue(LaserTurretBlock.FACING);
        float damage = base.getDamageForFace(facing, DAMAGE_PER_TICK);

        // 造成魔法伤害
        target.hurt(level.damageSources().magic(), damage);

        // 点燃效果（每20tick刷新一次）
        if (level.getGameTime() % 20 == 0) {
            target.setSecondsOnFire(FIRE_SECONDS);
        }
    }

    /**
     * 同步光束位置给客户端
     */
    private void syncBeamPosition() {
        setAnimData(BEAM_ACTIVE, true);
    }

    /**
     * 获取预期伤害（用于厉行节约）
     */
    public float getExpectedDamage(TurretBaseBlockEntity base) {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof LaserTurretBlock)) return DAMAGE_PER_TICK;
        Direction facing = state.getValue(LaserTurretBlock.FACING);
        return base.getDamageForFace(facing, DAMAGE_PER_TICK);
    }

    /**
     * 计算炮口位置
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
            setAnimData(BEAM_ACTIVE, false);
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
                setAnimData(BEAM_ACTIVE, true);
            } else {
                setAnimData(HAS_TARGET, false);
                setAnimData(BEAM_ACTIVE, false);
            }
        }

        return closest;
    }

    /**
     * 检查是否为有效目标
     */
    private boolean isValidTarget(LivingEntity entity, Level level, BlockPos pos) {
        if (!entity.isAlive()) return false;
        if (entity.isInvulnerable()) return false;

        TurretBaseBlockEntity base = getBaseEntity();
        if (base == null) return false;

        ItemStack pluginStack = base.getPluginStack();
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();

        List<String> blacklist = SmartChipItem.getBlacklist(pluginStack);
        boolean inBlacklist = blacklist.contains(entityId);

        List<String> whitelist = SmartChipItem.getWhitelist(pluginStack);
        if (whitelist.contains(entityId)) return false;

        if (!inBlacklist) {
            int flags = SmartChipItem.getTargetFlags(pluginStack);
            boolean matched = false;

            if ((flags & SmartChipItem.FLAG_HOSTILE) != 0 && entity instanceof Enemy) matched = true;
            if (!matched && (flags & SmartChipItem.FLAG_NEUTRAL) != 0 && entity instanceof NeutralMob) matched = true;
            if (!matched && (flags & SmartChipItem.FLAG_FRIENDLY) != 0 &&
                (entity instanceof Animal || entity instanceof AmbientCreature || entity instanceof WaterAnimal)) matched = true;
            if (!matched && (flags & SmartChipItem.FLAG_PLAYERS) != 0 &&
                entity instanceof Player p && !p.isCreative() && !p.isSpectator()) matched = true;

            if (!matched) return false;
        }

        // 友伤保护
        if (base.isFriendlyFire()) {
            java.util.UUID ownerId = base.getOwner();
            if (ownerId != null) {
                if (entity.getUUID().equals(ownerId)) return false;
                if (entity instanceof net.minecraft.world.entity.TamableAnimal tameable) {
                    java.util.UUID tameOwner = tameable.getOwnerUUID();
                    if (tameOwner != null && tameOwner.equals(ownerId)) return false;
                }
            }
        }

        Direction facing = getBlockState().getValue(LaserTurretBlock.FACING);
        double searchRadius = base.getSearchRadiusForFace(facing, SEARCH_RADIUS);
        if (!isTargetInRange(entity, pos, searchRadius)) return false;

        // 可见性检查
        Vec3 visiblePoint = getVisibleTargetPoint(entity, level, pos);
        if (visiblePoint == null) return false;

        visibleTargetPoint = visiblePoint;

        // 厉行节约
        if (base.isThriftyMode()) {
            float expectedDamage = getExpectedDamage(base);
            float currentHealth = entity.getHealth();
            float reservedDamage = base.getReservedDamage(entity.getId());
            float remainingHealth = currentHealth - reservedDamage;
            if (remainingHealth <= 0) return false;
        }

        return true;
    }

    private boolean isTargetInRange(LivingEntity entity, BlockPos pos, double searchRadius) {
        Vec3 turretPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return entity.distanceToSqr(turretPos) <= searchRadius * searchRadius;
    }

    private Vec3 getVisibleTargetPoint(LivingEntity entity, Level level, BlockPos pos) {
        Direction facing = getBlockState().getValue(LaserTurretBlock.FACING);
        Vec3 start = calculateMuzzlePosition(pos, facing);

        Vec3 headPoint = entity.position().add(0, entity.getEyeHeight(), 0);
        Vec3 bodyPoint = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
        Vec3 feetPoint = entity.position();

        if (canSeePoint(level, pos, start, headPoint)) return headPoint;
        if (canSeePoint(level, pos, start, bodyPoint)) return bodyPoint;
        if (canSeePoint(level, pos, start, feetPoint)) return feetPoint;

        return null;
    }

    private boolean canSeePoint(Level level, BlockPos pos, Vec3 start, Vec3 end) {
        Vec3 outward = end.subtract(start).normalize();
        Vec3 adjustedStart = start.add(outward.scale(0.6));

        net.minecraft.world.phys.BlockHitResult hitResult = level.clip(new net.minecraft.world.level.ClipContext(
                adjustedStart, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                null
        ));

        if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return true;

        BlockPos hitPos = hitResult.getBlockPos();
        Direction facing = getBlockState().getValue(LaserTurretBlock.FACING);
        BlockPos basePos = pos.relative(facing.getOpposite());

        return !hitPos.equals(basePos);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("YRot0", yRot0);
        tag.putFloat("XRot0", xRot0);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("YRot0")) yRot0 = tag.getFloat("YRot0");
        if (tag.contains("XRot0")) xRot0 = tag.getFloat("XRot0");
    }

    /**
     * 计算射速组件数量（用于激光透明度）
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
