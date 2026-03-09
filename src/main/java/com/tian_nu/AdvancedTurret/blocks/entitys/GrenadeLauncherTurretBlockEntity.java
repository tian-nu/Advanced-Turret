package com.tian_nu.AdvancedTurret.blocks.entitys;

import com.tian_nu.AdvancedTurret.Config;
import com.tian_nu.AdvancedTurret.blocks.GrenadeLauncherTurretBlock;
import com.tian_nu.AdvancedTurret.entity.GrenadeEntity;
import com.tian_nu.AdvancedTurret.items.ModItems;
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
 * 榴弹发射器炮塔方块实体
 * 
 * <p>特点：</p>
 * <ul>
 *   <li>抛物线弹道：受重力影响</li>
 *   <li>爆炸伤害：直击+范围</li>
 *   <li>专用弹药：榴弹物品</li>
 * </ul>
 * 
 * @author tian_nu
 */
public class GrenadeLauncherTurretBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    // ========== 常量 ==========
    
    /** 射击间隔 (tick) - 0.5发/秒 */
    public static final int FIRE_RATE = 40;
    /** 搜索范围 */
    public static final double SEARCH_RADIUS = 32.0;
    /** 子弹速度 */
    public static final double BULLET_SPEED = 1.5;
    /** 直击伤害 */
    public static final float DIRECT_DAMAGE = 5.0F;
    /** 爆炸伤害 */
    public static final float EXPLOSION_DAMAGE = 10.0F;
    /** 爆炸半径 */
    public static final float EXPLOSION_RADIUS = 3.0F;
    /** 重力常数 */
    public static final double GRAVITY = 0.05;

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
    private Vec3 visibleTargetPoint = null;
    
    public float yRot0 = 0.0f;
    public float xRot0 = 0.0f;

    public GrenadeLauncherTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRENADE_LAUNCHER_TURRET.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GrenadeLauncherTurretBlockEntity blockEntity) {
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

        Direction facing = state.getValue(GrenadeLauncherTurretBlock.FACING);
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
                blockEntity.cooldown = base.getFireRateForFace(facing, FIRE_RATE);
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
        if (!(state.getBlock() instanceof GrenadeLauncherTurretBlock)) return null;

        Direction facing = state.getValue(GrenadeLauncherTurretBlock.FACING);
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
                    }
                } else {
                    targetLostTicks = 0;
                    visibleTargetPoint = visiblePoint;
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
        if (!(state.getBlock() instanceof GrenadeLauncherTurretBlock)) return false;
        Direction facing = state.getValue(GrenadeLauncherTurretBlock.FACING);
        int energyCost = base.getEnergyCostForFace(facing, Config.grenadeLauncherEnergyCost);
        
        if (base.getEnergyStored() < energyCost) return false;
        
        return hasAmmo(base);
    }
    
    /**
     * 检查弹药槽是否有榴弹
     */
    private boolean hasAmmo(TurretBaseBlockEntity base) {
        net.minecraftforge.items.IItemHandler ammoInv = base.getAmmoInventory();
        for (int i = 0; i < ammoInv.getSlots(); i++) {
            ItemStack stack = ammoInv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(ModItems.GRENADE.get())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 消耗一颗榴弹
     */
    private void consumeAmmo(TurretBaseBlockEntity base) {
        net.minecraftforge.items.IItemHandler ammoInv = base.getAmmoInventory();
        for (int i = 0; i < ammoInv.getSlots(); i++) {
            ItemStack stack = ammoInv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(ModItems.GRENADE.get())) {
                stack.shrink(1);
                return;
            }
        }
    }

    /**
     * 执行射击
     */
    private void shoot(Level level, BlockPos pos, BlockState state, TurretBaseBlockEntity base) {
        Direction facing = state.getValue(GrenadeLauncherTurretBlock.FACING);
        int energyCost = base.getEnergyCostForFace(facing, Config.grenadeLauncherEnergyCost);
        if (base.getEnergyStored() < energyCost) return;

        if (!hasAmmo(base)) return;

        if (!(level instanceof ServerLevel)) return;

        Vec3 muzzlePos = calculateMuzzlePosition(pos, facing);

        Vec3 targetPos = (visibleTargetPoint != null)
            ? visibleTargetPoint
            : target.position().add(0, target.getBbHeight() * 0.5, 0);

        // 预判瞄准
        if (base.isPredictiveAiming()) {
            double dist = muzzlePos.distanceTo(targetPos);
            double time = dist / BULLET_SPEED;
            targetPos = targetPos.add(target.getDeltaMovement().scale(time));
        }

        // 计算抛物线初速度
        Vec3 velocity = calculateParabolicVelocity(muzzlePos, targetPos, BULLET_SPEED);

        // 消耗能量
        base.consumeEnergy(energyCost);

        // 消耗弹药
        Level baseLevel = base.getLevel();
        if (baseLevel != null && base.hasAmmoRecyclingPlugin()) {
            if (baseLevel.random.nextFloat() >= Config.ammoRecycleChance) {
                consumeAmmo(base);
            }
        } else {
            consumeAmmo(base);
        }

        // 创建榴弹
        float directDamage = base.getDamageForFace(facing, DIRECT_DAMAGE);
        float explosionDamage = base.getDamageForFace(facing, EXPLOSION_DAMAGE);
        
        GrenadeEntity grenade = new GrenadeEntity(level, muzzlePos.x, muzzlePos.y, muzzlePos.z, directDamage);
        grenade.setOwner(null);
        grenade.setSourcePos(pos);
        grenade.setBasePos(pos.relative(facing.getOpposite()));
        grenade.setExplosionDamage(explosionDamage);
        grenade.setExplosionRadius(EXPLOSION_RADIUS);
        grenade.setDestroyBlocks(base.hasDestructionPlugin());
        grenade.setDeltaMovement(velocity);

        boolean spawned = level.addFreshEntity(grenade);
        if (!spawned) {
            LOGGER.warn("Failed to spawn grenade at {}", muzzlePos);
        }

        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.5F, 1.2F);
    }

    /**
     * 计算抛物线初速度
     */
    private Vec3 calculateParabolicVelocity(Vec3 start, Vec3 end, double speed) {
        Vec3 diff = end.subtract(start);
        double horizontalDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        double heightDiff = diff.y;
        
        // 计算飞行时间
        double time = horizontalDist / speed;
        if (time < 1) time = 1;
        
        // 计算水平速度分量
        double vx = diff.x / time;
        double vz = diff.z / time;
        
        // 计算垂直速度分量（考虑重力）
        // y = vy*t - 0.5*g*t^2
        // vy = (y + 0.5*g*t^2) / t
        double vy = (heightDiff + 0.5 * GRAVITY * time * time) / time;
        
        return new Vec3(vx, vy, vz);
    }

    /**
     * 获取预期伤害（用于厉行节约）
     */
    public float getExpectedDamage(TurretBaseBlockEntity base) {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof GrenadeLauncherTurretBlock)) return DIRECT_DAMAGE + EXPLOSION_DAMAGE;
        Direction facing = state.getValue(GrenadeLauncherTurretBlock.FACING);
        return base.getDamageForFace(facing, DIRECT_DAMAGE) + base.getDamageForFace(facing, EXPLOSION_DAMAGE);
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
            Vec3 visiblePoint = getVisibleTargetPoint(closest, level, pos);
            if (visiblePoint != null) {
                visibleTargetPoint = visiblePoint;
                setAnimData(TARGET_POS_X, visiblePoint.x);
                setAnimData(TARGET_POS_Y, visiblePoint.y);
                setAnimData(TARGET_POS_Z, visiblePoint.z);
                setAnimData(HAS_TARGET, true);
            } else {
                setAnimData(HAS_TARGET, false);
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
        
        // 1. 黑名单检查 (强制攻击)
        List<String> blacklist = SmartChipItem.getBlacklist(pluginStack);
        boolean inBlacklist = blacklist.contains(entityId);
        
        // 2. 白名单检查 (强制排除)
        List<String> whitelist = SmartChipItem.getWhitelist(pluginStack);
        if (whitelist.contains(entityId)) {
            return false;
        }
        
        // 3. 目标模式检查
        if (!inBlacklist) {
            int flags = SmartChipItem.getTargetFlags(pluginStack);
            boolean matched = false;
            
            if ((flags & SmartChipItem.FLAG_HOSTILE) != 0) {
                if (entity instanceof net.minecraft.world.entity.monster.Enemy) matched = true;
            }
            
            if (!matched && (flags & SmartChipItem.FLAG_NEUTRAL) != 0) {
                if (entity instanceof net.minecraft.world.entity.NeutralMob) matched = true;
            }
            
            if (!matched && (flags & SmartChipItem.FLAG_FRIENDLY) != 0) {
                if (entity instanceof net.minecraft.world.entity.animal.Animal ||
                    entity instanceof net.minecraft.world.entity.ambient.AmbientCreature ||
                    entity instanceof net.minecraft.world.entity.animal.WaterAnimal) {
                    matched = true;
                }
            }
            
            if (!matched && (flags & SmartChipItem.FLAG_PLAYERS) != 0) {
                if (entity instanceof Player) matched = true;
            }
            
            if (!matched) return false;
        }
        
        // 4. 友伤保护检查
        if (SmartChipItem.isFriendlyFire(pluginStack)) {
            return true;
        }
        
        if (entity instanceof Player player) {
            if (player.getUUID().equals(base.getOwner())) return false;
        }
        
        if (entity instanceof net.minecraft.world.entity.TamableAnimal tameable) {
            if (base.getOwner() != null && base.getOwner().equals(tameable.getOwnerUUID())) {
                return false;
            }
        }
        
        // 5. 范围检查
        double dist = entity.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (dist > SEARCH_RADIUS * SEARCH_RADIUS) return false;
        
        // 6. 视线检测
        return canSeeTarget(entity, level, pos);
    }

    /**
     * 检查目标是否在范围内
     */
    private boolean isTargetInRange(LivingEntity target, BlockPos pos, double range) {
        double dist = target.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return dist <= range * range;
    }

    /**
     * 视线检测（三点）
     */
    private boolean canSeeTarget(LivingEntity target, Level level, BlockPos pos) {
        Vec3 start = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        
        Vec3[] points = {
            target.position().add(0, target.getBbHeight() * 0.1, 0),  // 脚
            target.position().add(0, target.getBbHeight() * 0.5, 0),  // 中
            target.position().add(0, target.getBbHeight() * 0.9, 0)   // 头
        };
        
        for (Vec3 point : points) {
            if (canSeePoint(start, point, level, pos)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检测点到点是否可见
     */
    private boolean canSeePoint(Vec3 start, Vec3 end, Level level, BlockPos pos) {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof GrenadeLauncherTurretBlock)) return false;
        Direction facing = state.getValue(GrenadeLauncherTurretBlock.FACING);
        
        Vec3 outward = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        Vec3 adjustedStart = start.add(outward.scale(0.6));
        
        net.minecraft.world.level.ClipContext context = new net.minecraft.world.level.ClipContext(
            adjustedStart, end,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            null
        );
        
        net.minecraft.world.phys.BlockHitResult hitResult = level.clip(context);
        
        if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
            return true;
        }
        
        BlockPos basePos = pos.relative(facing.getOpposite());
        if (hitResult.getBlockPos().equals(basePos)) {
            return false;
        }
        
        return false;
    }

    /**
     * 获取可见瞄准点
     */
    private Vec3 getVisibleTargetPoint(LivingEntity target, Level level, BlockPos pos) {
        Vec3 start = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        
        Vec3[] points = {
            target.position().add(0, target.getBbHeight() * 0.1, 0),
            target.position().add(0, target.getBbHeight() * 0.5, 0),
            target.position().add(0, target.getBbHeight() * 0.9, 0)
        };
        
        for (Vec3 point : points) {
            if (canSeePoint(start, point, level, pos)) {
                return point;
            }
        }
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Cooldown", cooldown);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        cooldown = tag.getInt("Cooldown");
    }
}
