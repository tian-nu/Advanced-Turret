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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
    public static final double BULLET_SPEED = 1.2;
    /** 子弹伤害 */
    public static final float BULLET_DAMAGE = 2.0F;
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
            return;
        }

        Direction facing = state.getValue(JunkTurretBlock.FACING);
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
    private void consumeAmmo(TurretBaseBlockEntity base, ItemStack ammoStack) {
        ammoStack.shrink(1);
    }

    /**
     * 执行射击
     */
    private void shoot(Level level, BlockPos pos, BlockState state, TurretBaseBlockEntity base) {
        Direction facing = state.getValue(JunkTurretBlock.FACING);
        int energyCost = base.getEnergyCostForFace(facing, Config.junkTurretEnergyCost);
        if (base.getEnergyStored() < energyCost) return;

        // 获取弹药物品
        ItemStack ammoStack = findAnyAmmo(base);
        if (ammoStack.isEmpty()) return;

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

        // 消耗弹药（弹药回收插件：20%概率不消耗）
        Level baseLevel = base.getLevel();
        if (baseLevel != null && base.hasAmmoRecyclingPlugin()) {
            if (baseLevel.random.nextFloat() >= Config.ammoRecycleChance) {
                consumeAmmo(base, ammoStack);
            }
        } else {
            consumeAmmo(base, ammoStack);
        }

        // 创建垃圾投射物
        float damage = base.getDamageForFace(facing, BULLET_DAMAGE);
        
        JunkProjectileEntity junk = new JunkProjectileEntity(level, muzzlePos.x, muzzlePos.y, muzzlePos.z, damage);
        junk.setOwner(null);
        junk.setSourcePos(pos);
        junk.setBasePos(pos.relative(facing.getOpposite()));
        junk.setAmmoItem(ammoStack.copy()); // 传递弹药物品用于渲染
        junk.setDeltaMovement(velocity);

        boolean spawned = level.addFreshEntity(junk);
        if (!spawned) {
            LOGGER.warn("Failed to spawn junk projectile at {}", muzzlePos);
        }

        level.playSound(null, pos, SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 0.5F, 0.8F);
    }

    /**
     * 计算抛物线初速度
     */
    private Vec3 calculateParabolicVelocity(Vec3 start, Vec3 end, double speed) {
        Vec3 diff = end.subtract(start);
        double horizontalDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        double heightDiff = diff.y;
        
        double time = horizontalDist / speed;
        if (time < 1) time = 1;
        
        double vx = diff.x / time;
        double vz = diff.z / time;
        
        // 增加额外高度（基于距离的20%），让抛物线弧度更明显
        double extraHeight = horizontalDist * 0.20;
        double totalHeightDiff = heightDiff + extraHeight;
        
        double vy = (totalHeightDiff + 0.5 * GRAVITY * time * time) / time;
        
        return new Vec3(vx, vy, vz);
    }

    /**
     * 获取预期伤害（用于厉行节约）
     */
    public float getExpectedDamage(TurretBaseBlockEntity base) {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof JunkTurretBlock)) return BULLET_DAMAGE;
        Direction facing = state.getValue(JunkTurretBlock.FACING);
        return base.getDamageForFace(facing, BULLET_DAMAGE);
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
        
        List<String> blacklist = SmartChipItem.getBlacklist(pluginStack);
        boolean inBlacklist = blacklist.contains(entityId);
        
        List<String> whitelist = SmartChipItem.getWhitelist(pluginStack);
        if (whitelist.contains(entityId)) {
            return false;
        }
        
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
        
        double dist = entity.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (dist > SEARCH_RADIUS * SEARCH_RADIUS) return false;
        
        return canSeeTarget(entity, level, pos);
    }

    private boolean isTargetInRange(LivingEntity target, BlockPos pos, double range) {
        double dist = target.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return dist <= range * range;
    }

    private boolean canSeeTarget(LivingEntity target, Level level, BlockPos pos) {
        Vec3 start = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        
        Vec3[] points = {
            target.position().add(0, target.getBbHeight() * 0.1, 0),
            target.position().add(0, target.getBbHeight() * 0.5, 0),
            target.position().add(0, target.getBbHeight() * 0.9, 0)
        };
        
        for (Vec3 point : points) {
            if (canSeePoint(start, point, level, pos)) {
                return true;
            }
        }
        return false;
    }

    private boolean canSeePoint(Vec3 start, Vec3 end, Level level, BlockPos pos) {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof JunkTurretBlock)) return false;
        Direction facing = state.getValue(JunkTurretBlock.FACING);
        
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
