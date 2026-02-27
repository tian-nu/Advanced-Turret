package com.tian_nu.AdvancedTurret.blocks.entitys;

import com.mojang.logging.LogUtils;
import com.tian_nu.AdvancedTurret.Config;
import com.tian_nu.AdvancedTurret.blocks.RailgunTurretBlock;
import com.tian_nu.AdvancedTurret.entity.RailgunBulletEntity;
import com.tian_nu.AdvancedTurret.entity.TurretBulletEntity;
import com.tian_nu.AdvancedTurret.items.SmartChipItem;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
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

public class RailgunTurretBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int FIRE_RATE = 30;
    public static final double SEARCH_RADIUS = 24.0;
    public static final double BULLET_SPEED = 6.0;
    public static final float BULLET_DAMAGE = 12.0F;

    public static SerializableDataTicket<Boolean> HAS_TARGET;
    public static SerializableDataTicket<Double> TARGET_POS_X;
    public static SerializableDataTicket<Double> TARGET_POS_Y;
    public static SerializableDataTicket<Double> TARGET_POS_Z;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int cooldown = 0;
    private LivingEntity target = null;
    private int targetLostTicks = 0;

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
                blockEntity.cooldown = base.getFireRateForFace(facing, FIRE_RATE);
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
                Vec3 targetPos = target.position().add(0, target.getEyeHeight() * 0.5, 0);
                setAnimData(TARGET_POS_X, targetPos.x);
                setAnimData(TARGET_POS_Y, targetPos.y);
                setAnimData(TARGET_POS_Z, targetPos.z);
                setAnimData(HAS_TARGET, true);
            }
        }
    }

    private boolean canShoot(TurretBaseBlockEntity base, Direction facing) {
        int energyCost = base.getEnergyCostForFace(facing, Config.railgunEnergyCost);
        return base.getEnergyStored() >= energyCost;
    }

    private void shoot(Level level, BlockPos pos, BlockState state, TurretBaseBlockEntity base, Direction facing) {
        int energyCost = base.getEnergyCostForFace(facing, Config.railgunEnergyCost);
        if (base.getEnergyStored() < energyCost) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        Vec3 muzzlePos = calculateMuzzlePosition(pos, facing);
        Vec3 targetPos = target.position().add(0, target.getEyeHeight() * 0.5, 0);

        if (base.isPredictiveAiming()) {
            double dist = muzzlePos.distanceTo(targetPos);
            double time = dist / BULLET_SPEED;
            targetPos = targetPos.add(target.getDeltaMovement().scale(time));
        }

        Vec3 direction = targetPos.subtract(muzzlePos).normalize();

        base.consumeEnergy(energyCost);

        float damage = base.getDamageForFace(facing, BULLET_DAMAGE);
        RailgunBulletEntity bullet = new RailgunBulletEntity(level, muzzlePos.x, muzzlePos.y, muzzlePos.z, damage);
        bullet.setOwner(null);
        bullet.setSourcePos(pos);
        bullet.setBasePos(pos.relative(facing.getOpposite())); // 设置基座位置
        bullet.setPenetrationCount(3); // 穿透3个目标
        bullet.shoot(direction, (float) BULLET_SPEED);

        boolean spawned = level.addFreshEntity(bullet);
        if (!spawned) {
            LOGGER.warn("Failed to spawn railgun bullet at {}", muzzlePos);
        }

        level.playSound(null, pos, SoundEvents.CROSSBOW_SHOOT, SoundSource.BLOCKS, 1.0F, 1.0F);

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
            Vec3 tpos = closest.position().add(0, closest.getEyeHeight() * 0.5, 0);
            setAnimData(TARGET_POS_X, tpos.x);
            setAnimData(TARGET_POS_Y, tpos.y);
            setAnimData(TARGET_POS_Z, tpos.z);
            setAnimData(HAS_TARGET, true);
        }

        return closest;
    }

    private boolean isValidTarget(LivingEntity entity, Level level, BlockPos pos) {
        if (!entity.isAlive()) {
            return false;
        }

        if (entity.isInvulnerable()) {
            return false;
        }

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

            if (flags == 0) {
            }

            if (!matched) return false;
        }

        if (base.isFriendlyFire()) {
            java.util.UUID ownerId = base.getOwner();
            if (ownerId != null) {
                if (entity.getUUID().equals(ownerId)) return false;

                if (entity instanceof net.minecraft.world.entity.TamableAnimal tameable) {
                    java.util.UUID tameOwner = tameable.getOwnerUUID();
                    if (tameOwner != null && tameOwner.equals(ownerId)) {
                        return false;
                    }
                }
            }
        }

        Direction facing = getBlockState().getValue(RailgunTurretBlock.FACING);
        double searchRadius = base.getSearchRadiusForFace(facing, SEARCH_RADIUS);
        if (!isTargetInRange(entity, pos, searchRadius)) {
            return false;
        }

        if (!hasLineOfSight(entity, level, pos)) {
            return false;
        }

        return true;
    }

    private boolean isTargetInRange(LivingEntity entity, BlockPos pos, double searchRadius) {
        Vec3 turretPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return entity.distanceToSqr(turretPos) <= searchRadius * searchRadius;
    }

    private boolean hasLineOfSight(LivingEntity entity, Level level, BlockPos pos) {
        Direction facing = getBlockState().getValue(RailgunTurretBlock.FACING);
        Vec3 start = calculateMuzzlePosition(pos, facing);
        Vec3[] targetPoints = new Vec3[] {
                entity.position().add(0, entity.getEyeHeight(), 0),
                entity.position().add(0, entity.getBbHeight() * 0.5, 0),
                entity.position()
        };
        for (Vec3 end : targetPoints) {
            if (canSeePoint(level, pos, start, end)) {
                return true;
            }
        }
        return false;
    }

    private boolean canSeePoint(Level level, BlockPos pos, Vec3 start, Vec3 end) {
        Vec3 outward = end.subtract(start).normalize();
        // 从炮塔外部开始检测，避免击中炮塔自身
        Vec3 adjustedStart = start.add(outward.scale(0.6));

        var hitResult = level.clip(new net.minecraft.world.level.ClipContext(
                adjustedStart, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                null
        ));

        if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
            return true;
        }
        
        BlockPos hitPos = hitResult.getBlockPos();
        
        // 如果被基座阻挡，返回 false（不能看到目标）
        Direction facing = getBlockState().getValue(RailgunTurretBlock.FACING);
        BlockPos basePos = pos.relative(facing.getOpposite());
        if (hitPos.equals(basePos)) {
            return false;
        }
        
        // 如果击中的是其他方块（非炮塔、非基座），也不能看到
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> PlayState.CONTINUE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
