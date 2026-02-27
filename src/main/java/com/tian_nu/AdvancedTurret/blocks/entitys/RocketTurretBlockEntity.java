package com.tian_nu.AdvancedTurret.blocks.entitys;

import com.tian_nu.AdvancedTurret.blocks.RocketTurretBlock;
import com.tian_nu.AdvancedTurret.entity.RocketEntity;
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
import net.minecraft.world.entity.Mob;
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
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.network.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.List;

/**
 * 火箭炮塔方块实体
 * 
 * <p>特点：</p>
 * <ul>
 *   <li>远程攻击：48格搜索范围</li>
 *   <li>高伤害：直击10 + 爆炸10（半径4格）</li>
 *   <li>低射速：5秒/发</li>
 *   <li>高能耗：5000 FE/发</li>
 *   <li>破坏插件：可破坏方块</li>
 * </ul>
 * 
 * @author tian_nu
 */
public class RocketTurretBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    // ========== 常量（来自路线图） ==========

    /** 射击间隔 (tick) - 5秒 */
    public static final int FIRE_RATE = 100;
    /** 搜索范围 */
    public static final double SEARCH_RADIUS = 48.0;
    /** 子弹速度 */
    public static final double BULLET_SPEED = 2.0;
    /** 直击伤害 */
    public static final float DIRECT_DAMAGE = 10.0F;
    /** 爆炸伤害 */
    public static final float EXPLOSION_DAMAGE = 10.0F;
    /** 爆炸半径 */
    public static final float EXPLOSION_RADIUS = 4.0F;
    /** 能量消耗 */
    public static final int ENERGY_COST = 5000;

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

    public RocketTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROCKET_TURRET.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RocketTurretBlockEntity blockEntity) {
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

        Direction facing = state.getValue(RocketTurretBlock.FACING);
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
        if (!(state.getBlock() instanceof RocketTurretBlock)) return null;

        Direction facing = state.getValue(RocketTurretBlock.FACING);
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
            target = findTarget(level, pos, base.getSearchRadiusForFace(facing, SEARCH_RADIUS));
            targetLostTicks = 0;
        } else {
            if (!isTargetInRange(target, pos, base.getSearchRadiusForFace(facing, SEARCH_RADIUS))) {
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
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof RocketTurretBlock)) return false;
        Direction facing = state.getValue(RocketTurretBlock.FACING);
        int energyCost = base.getEnergyCostForFace(facing, ENERGY_COST);

        // 检查能量
        if (base.getEnergyStored() < energyCost) return false;
        
        // 检查弹药
        return hasAmmo(base);
    }
    
    /**
     * 检查弹药槽是否有火箭弹
     */
    private boolean hasAmmo(TurretBaseBlockEntity base) {
        net.minecraftforge.items.IItemHandler ammoInv = base.getAmmoInventory();
        for (int i = 0; i < ammoInv.getSlots(); i++) {
            ItemStack stack = ammoInv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(ModItems.ROCKET.get())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 消耗一枚火箭弹
     */
    private void consumeAmmo(TurretBaseBlockEntity base) {
        net.minecraftforge.items.IItemHandler ammoInv = base.getAmmoInventory();
        for (int i = 0; i < ammoInv.getSlots(); i++) {
            ItemStack stack = ammoInv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(ModItems.ROCKET.get())) {
                stack.shrink(1);
                return;
            }
        }
    }

    /**
     * 执行射击
     */
    private void shoot(Level level, BlockPos pos, BlockState state, TurretBaseBlockEntity base) {
        Direction facing = state.getValue(RocketTurretBlock.FACING);
        int energyCost = base.getEnergyCostForFace(facing, ENERGY_COST);
        if (base.getEnergyStored() < energyCost) return;
        
        // 检查弹药
        if (!hasAmmo(base)) return;

        if (!(level instanceof ServerLevel serverLevel)) return;

        Vec3 muzzlePos = calculateMuzzlePosition(pos, facing);

        Vec3 targetPos = target.position().add(0, target.getEyeHeight() * 0.5, 0);

        // 预判瞄准
        if (base.isPredictiveAiming()) {
            double dist = muzzlePos.distanceTo(targetPos);
            double time = dist / BULLET_SPEED;
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
        float directDamage = base.getDamageForFace(facing, DIRECT_DAMAGE);
        float explosionDamage = EXPLOSION_DAMAGE; // 爆炸伤害不随升级组件变化

        // 创建火箭弹
        RocketEntity rocket = new RocketEntity(level, muzzlePos.x, muzzlePos.y, muzzlePos.z, directDamage);
        rocket.setOwner(null);
        rocket.setSourcePos(pos);
        rocket.setBasePos(pos.relative(facing.getOpposite()));
        rocket.setExplosionDamage(explosionDamage);
        rocket.setExplosionRadius(EXPLOSION_RADIUS);
        
        // 破坏插件：决定是否破坏方块
        rocket.setDestroyBlocks(base.hasDestructionPlugin());

        rocket.shoot(direction, (float) BULLET_SPEED);

        boolean spawned = level.addFreshEntity(rocket);
        if (!spawned) {
            LOGGER.warn("Failed to spawn rocket at {}", muzzlePos);
        }

        // 播放射击音效
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.5F, 1.0F);
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
        if (whitelist.contains(entityId)) return false;

        // 3. 目标模式检查
        if (!inBlacklist) {
            int flags = SmartChipItem.getTargetFlags(pluginStack);
            boolean matched = false;

            if ((flags & SmartChipItem.FLAG_HOSTILE) != 0) {
                if (entity instanceof Enemy) matched = true;
            }

            if (!matched && (flags & SmartChipItem.FLAG_NEUTRAL) != 0) {
                if (entity instanceof NeutralMob) matched = true;
            }

            if (!matched && (flags & SmartChipItem.FLAG_FRIENDLY) != 0) {
                if (entity instanceof Animal || entity instanceof AmbientCreature || entity instanceof WaterAnimal) matched = true;
            }

            if (!matched && (flags & SmartChipItem.FLAG_PLAYERS) != 0) {
                if (entity instanceof Player p && !p.isCreative() && !p.isSpectator()) matched = true;
            }

            if (!matched) return false;
        }

        // 4. 友伤保护检查
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

        Direction facing = getBlockState().getValue(RocketTurretBlock.FACING);
        double searchRadius = base.getSearchRadiusForFace(facing, SEARCH_RADIUS);
        if (!isTargetInRange(entity, pos, searchRadius)) return false;

        if (!hasLineOfSight(entity, level, pos)) return false;

        return true;
    }

    /**
     * 检查目标是否在范围内
     */
    private boolean isTargetInRange(LivingEntity entity, BlockPos pos, double searchRadius) {
        Vec3 turretPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return entity.distanceToSqr(turretPos) <= searchRadius * searchRadius;
    }

    /**
     * 检查是否有视线
     */
    private boolean hasLineOfSight(LivingEntity entity, Level level, BlockPos pos) {
        Direction facing = getBlockState().getValue(RocketTurretBlock.FACING);
        Vec3 start = calculateMuzzlePosition(pos, facing);

        Vec3[] targetPoints = new Vec3[] {
            entity.position().add(0, entity.getEyeHeight(), 0),
            entity.position().add(0, entity.getBbHeight() * 0.5, 0),
            entity.position()
        };

        for (Vec3 end : targetPoints) {
            if (canSeePoint(level, pos, start, end)) return true;
        }

        return false;
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

        Direction facing = getBlockState().getValue(RocketTurretBlock.FACING);
        BlockPos basePos = pos.relative(facing.getOpposite());
        if (hitPos.equals(basePos)) return false;

        return false;
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
