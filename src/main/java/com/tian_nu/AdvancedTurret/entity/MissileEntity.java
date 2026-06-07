package com.tian_nu.AdvancedTurret.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 导弹实体
 *
 * <p>特点：</p>
 * <ul>
 *   <li>制导追踪：自动追踪目标移动</li>
 *   <li>越飞越快：每tick速度增加</li>
 *   <li>直击伤害：击中实体造成高额伤害</li>
 *   <li>爆炸伤害：范围内AOE伤害</li>
 *   <li>破坏插件：有破坏插件时破坏方块</li>
 *   <li>烟雾尾迹：飞行时产生烟雾粒子</li>
 * </ul>
 *
 * @author tian_nu
 */
public class MissileEntity extends TurretProjectileEntity {

    // ==================== 导弹属性 ====================

    /** 直击伤害 */
    private float directDamage = 10.0F;

    /** 爆炸伤害 */
    private float explosionDamage = 15.0F;

    /** 爆炸半径 */
    private float explosionRadius = 4.0F;

    /** 是否破坏方块（由破坏插件控制） */
    private boolean destroyBlocks = false;

    /** 加速度系数（指数增长：初始1.5速，20tick后达到5速，k≈0.062） */
    private double acceleration = 0.062;

    /** 转向速率 (每tick转向插值系数 0.0-1.0) */
    private double turnRate = 0.3;

    /** 追踪的目标实体 */
    private LivingEntity targetEntity = null;

    /** 标记：是否允许修改速度（内部使用） */
    private boolean allowDeltaMovementChange = false;

    // ==================== 构造函数 ====================

    public MissileEntity(EntityType<? extends MissileEntity> type, Level level) {
        super(type, level);
        this.lifetime = 300; // 较长生命周期（15秒）
        this.setInvulnerable(true);
        this.allowDeltaMovementChange = true;
    }

    public MissileEntity(Level level, double x, double y, double z, float damage) {
        super(ModEntities.MISSILE.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setDamage(damage);
        this.directDamage = damage;
        this.lifetime = 300;
        this.setInvulnerable(true);
        this.allowDeltaMovementChange = true;
    }

    // ==================== 拦截外部速度修改 ====================

    /**
     * 重写 setDeltaMovement，只允许内部修改
     * 防止爆炸冲击波修改导弹的飞行方向
     */
    @Override
    public void setDeltaMovement(Vec3 motion) {
        if (allowDeltaMovementChange) {
            super.setDeltaMovement(motion);
        }
    }

    @Override
    public void setDeltaMovement(double x, double y, double z) {
        if (allowDeltaMovementChange) {
            super.setDeltaMovement(x, y, z);
        }
    }

    // ==================== 属性访问器 ====================

    public void setDirectDamage(float damage) { this.directDamage = damage; }
    public float getDirectDamage() { return this.directDamage; }

    public void setExplosionDamage(float damage) { this.explosionDamage = damage; }
    public float getExplosionDamage() { return this.explosionDamage; }

    public void setExplosionRadius(float radius) { this.explosionRadius = radius; }
    public float getExplosionRadius() { return this.explosionRadius; }

    public void setDestroyBlocks(boolean destroy) { this.destroyBlocks = destroy; }
    public boolean shouldDestroyBlocks() { return this.destroyBlocks; }

    public void setAcceleration(double accel) { this.acceleration = accel; }
    public double getAcceleration() { return this.acceleration; }

    public void setTurnRate(double rate) { this.turnRate = Math.max(0.0, Math.min(1.0, rate)); }
    public double getTurnRate() { return this.turnRate; }

    public void setTargetEntity(LivingEntity entity) { this.targetEntity = entity; }
    public LivingEntity getTargetEntity() { return this.targetEntity; }

    // ==================== 击中处理 ====================

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity livingEntity) {
            // 白名单检查
            if (shouldIgnoreDamage(livingEntity)) {
                return;
            }

            // 直击伤害
            dealDamage(livingEntity, this.directDamage);
        }

        // 爆炸（以导弹位置为中心）
        explode(this.position());
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // 爆炸
        explode(result.getLocation());
        this.discard();
    }

    /**
     * 爆炸效果
     * @param pos 爆炸位置
     */
    private void explode(Vec3 pos) {
        Level level = this.level();

        // 创建爆炸效果（视觉+声音）
        if (destroyBlocks) {
            level.explode(this, pos.x, pos.y, pos.z, explosionRadius, false, Level.ExplosionInteraction.BLOCK);
        } else {
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7F, 1.0F);
        }

        spawnExplosionDust(pos);


        // 范围伤害（自定义计算，确保伤害准确）
        if (!level.isClientSide) {
            AABB area = new AABB(
                pos.x - explosionRadius, pos.y - explosionRadius, pos.z - explosionRadius,
                pos.x + explosionRadius, pos.y + explosionRadius, pos.z + explosionRadius
            );

            java.util.List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area);
            for (LivingEntity entity : entities) {
                // 跳过白名单中的实体
                if (shouldIgnoreDamage(entity)) continue;

                double distance = distanceToExplosion(entity, pos);
                if (distance <= explosionRadius) {
                    // 伤害随距离衰减
                    float damageMultiplier = 1.0F - (float)(distance / explosionRadius);
                    float finalDamage = explosionDamage * damageMultiplier;

                    if (finalDamage > 0) {
                        dealDamage(entity, finalDamage);
                    }
                }
            }
        }
    }

    private void spawnExplosionDust(Vec3 pos) {
        Level level = this.level();
        double visualRadius = this.explosionRadius + 0.5D;

        if (level instanceof ServerLevel serverLevel) {
            int explosionCount = Math.max(2, (int) Math.ceil(visualRadius * 1.2D));
            int poofCount = Math.max(18, (int) Math.ceil(visualRadius * 22.0D));
            int smokeCount = Math.max(10, (int) Math.ceil(visualRadius * 10.0D));

            serverLevel.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, explosionCount,
                    visualRadius * 0.12D, visualRadius * 0.12D, visualRadius * 0.12D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.POOF, pos.x, pos.y, pos.z, poofCount,
                    visualRadius * 0.55D, visualRadius * 0.35D, visualRadius * 0.55D, 0.07D);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, smokeCount,
                    visualRadius * 0.45D, visualRadius * 0.3D, visualRadius * 0.45D, 0.03D);
        } else if (level.isClientSide) {
            level.addParticle(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 0.0D, 0.0D, 0.0D);
        }
    }

    // ==================== 核心逻辑 ====================


    /**
     * 自定义碰撞检测：分离检测实体和方块 + 制导追踪 + 越飞越快 + 避障
     */
    @Override
    public void tick() {
        // 生命周期检查
        if (this.lifetime-- <= 0) {
            this.discard();
            return;
        }

        // 允许内部修改速度
        allowDeltaMovementChange = true;

        Vec3 movement = this.getDeltaMovement();
        Vec3 currentPos = this.position();
        double currentSpeed = movement.length();

        // ========== 制导追踪 + 避障 ==========
        if (targetEntity != null && targetEntity.isAlive()) {
            Vec3 toTarget = targetEntity.position().add(0, targetEntity.getBbHeight() * 0.5, 0).subtract(this.position());
            double distanceToTarget = toTarget.length();
            Vec3 targetDir = toTarget.normalize();

            // 检测前方障碍物（预测距离：当前速度 * 5 tick 或最小 3 格）
            double lookAheadDist = Math.max(currentSpeed * 5, 3.0);
            Vec3 avoidance = checkObstacleAvoidance(currentPos, movement.normalize(), lookAheadDist);

            Vec3 newDir;
            if (avoidance != null && distanceToTarget > 8.0D) {
                // 有障碍物，混合避障方向和目标方向
                // 距离目标越远，避障权重越高
                double avoidanceWeight = Math.min(0.45, distanceToTarget / 32.0);
                newDir = targetDir.scale(1.0 - avoidanceWeight).add(avoidance.scale(avoidanceWeight)).normalize();
            } else {
                // 无障碍物，正常追踪目标
                Vec3 currentDir = movement.normalize();
                // 转向插值：新方向 = 当前方向 + (目标方向 - 当前方向) * 转向速率
                newDir = currentDir.add(targetDir.subtract(currentDir).scale(turnRate)).normalize();
            }

            // 应用新方向
            this.setDeltaMovement(newDir.scale(currentSpeed));
            movement = this.getDeltaMovement();
        }

        // ========== 越飞越快 ==========
        currentSpeed = movement.length();
        if (currentSpeed > 0.001) {
            double newSpeed = currentSpeed * (1.0 + acceleration);
            Vec3 direction = movement.normalize();
            this.setDeltaMovement(direction.scale(newSpeed));
            movement = this.getDeltaMovement();
        }

        Vec3 nextPos = currentPos.add(movement);
        if (shouldDiscardForFlightLimits(nextPos)) {
            allowDeltaMovementChange = false;
            return;
        }

        // ========== 碰撞检测（分离检测） ==========
        if (!this.level().isClientSide) {
            // 1. 检测实体碰撞（优先）
            EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                this.level(), this, currentPos, nextPos,
                this.getBoundingBox().expandTowards(movement).inflate(1.0),
                this::canHitEntity
            );

            if (entityHit != null && entityHit.getType() == HitResult.Type.ENTITY) {
                this.onHit(entityHit);
                return;
            }

            // 2. 检测方块碰撞
            ClipContext context = new ClipContext(
                currentPos, nextPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
            );
            BlockHitResult blockHit = this.level().clip(context);

            if (blockHit.getType() == HitResult.Type.BLOCK) {
                BlockPos hitBlockPos = blockHit.getBlockPos();

                // 基座方块：必须销毁
                if (basePos != null && hitBlockPos.equals(basePos)) {
                    this.onHit(blockHit);
                    return;
                }

                // 炮塔自身：前几tick跳过
                if (ignoreSourceBlockTicks > 0 && sourcePos != null && hitBlockPos.equals(sourcePos)) {
                    ignoreSourceBlockTicks--;
                    this.setPos(nextPos);
                    allowDeltaMovementChange = false;
                    return;
                }

                // 其他方块：爆炸
                this.onHit(blockHit);
                return;
            }
        }

        // 无碰撞，继续移动
        this.setPos(nextPos);

        // 禁止外部修改速度
        allowDeltaMovementChange = false;

        // 客户端：烟雾尾迹粒子
        if (this.level().isClientSide) {
            spawnSmokeTrail();
        }
    }

    /**
     * 检测前方障碍物并返回避障方向
     * @param currentPos 当前位置
     * @param moveDir 当前移动方向
     * @param lookAheadDist 前瞻距离
     * @return 避障方向（归一化），null 表示无需避障
     */
    private Vec3 checkObstacleAvoidance(Vec3 currentPos, Vec3 moveDir, double lookAheadDist) {
        // 检测前方、左前方、右前方、上方、下方多个方向
        Vec3[] checkDirs = {
            moveDir,                           // 正前方
            rotateY(moveDir, 30),              // 右偏30°
            rotateY(moveDir, -30),             // 左偏30°
            rotateY(moveDir, 60),              // 右偏60°
            rotateY(moveDir, -60),             // 左偏60°
            moveDir.add(0, 0.35, 0).normalize(), // ?????????????
            moveDir.add(0, 0.7, 0).normalize()   // ?????????????
        };

        Vec3 bestAvoidanceDir = null;
        double bestClearDist = 0;

        for (Vec3 dir : checkDirs) {
            ClipContext context = new ClipContext(
                currentPos,
                currentPos.add(dir.scale(lookAheadDist)),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
            );
            BlockHitResult hit = this.level().clip(context);

            double clearDist;
            if (hit.getType() == HitResult.Type.MISS) {
                clearDist = lookAheadDist;
            } else {
                clearDist = hit.getLocation().distanceTo(currentPos);
            }

            // 如果正前方畅通，无需避障
            if (dir.equals(moveDir) && clearDist >= lookAheadDist) {
                return null;
            }

            // 记录最畅通的方向
            if (clearDist > bestClearDist) {
                bestClearDist = clearDist;
                bestAvoidanceDir = dir;
            }
        }

        // 如果正前方受阻但其他方向畅通，返回避障方向
        if (bestAvoidanceDir != null && bestClearDist > 2.0) {
            return bestAvoidanceDir;
        }

        return null;
    }

    /**
     * 绕Y轴旋转向量
     */
    private Vec3 rotateY(Vec3 vec, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(
            vec.x * cos - vec.z * sin,
            vec.y,
            vec.x * sin + vec.z * cos
        );
    }

    /**
     * 生成烟雾尾迹
     */
    private void spawnSmokeTrail() {
        Vec3 pos = this.position();
        for (int i = 0; i < 3; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 0.3;
            double offsetY = (this.random.nextDouble() - 0.5) * 0.3;
            double offsetZ = (this.random.nextDouble() - 0.5) * 0.3;
            this.level().addParticle(
                ParticleTypes.SMOKE,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                0, 0.05, 0
            );
            // 添加大型烟雾
            if (random.nextFloat() < 0.3F) {
                this.level().addParticle(
                    ParticleTypes.LARGE_SMOKE,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    0, 0.1, 0
                );
            }
        }
    }

    // ==================== NBT存储 ====================

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("DirectDamage", this.directDamage);
        tag.putFloat("ExplosionDamage", this.explosionDamage);
        tag.putFloat("ExplosionRadius", this.explosionRadius);
        tag.putBoolean("DestroyBlocks", this.destroyBlocks);
        tag.putDouble("Acceleration", this.acceleration);
        tag.putDouble("TurnRate", this.turnRate);
        // 存储目标实体ID（如果目标离线，目标将不会被追踪）
        if (targetEntity != null && targetEntity.isAlive()) {
            tag.putUUID("TargetEntity", targetEntity.getUUID());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("DirectDamage")) {
            this.directDamage = tag.getFloat("DirectDamage");
        }
        if (tag.contains("ExplosionDamage")) {
            this.explosionDamage = tag.getFloat("ExplosionDamage");
        }
        if (tag.contains("ExplosionRadius")) {
            this.explosionRadius = tag.getFloat("ExplosionRadius");
        }
        if (tag.contains("DestroyBlocks")) {
            this.destroyBlocks = tag.getBoolean("DestroyBlocks");
        }
        if (tag.contains("Acceleration")) {
            this.acceleration = tag.getDouble("Acceleration");
        }
        if (tag.contains("TurnRate")) {
            this.turnRate = tag.getDouble("TurnRate");
        }
        // 读取目标实体
        if (tag.contains("TargetEntity") && !this.level().isClientSide) {
            java.util.UUID targetUUID = tag.getUUID("TargetEntity");
            // 延迟寻找目标实体（因为可能在加载时目标还未加载）
            this.targetEntity = this.level().getEntitiesOfClass(LivingEntity.class, new AABB(this.blockPosition()).inflate(100))
                .stream()
                .filter(e -> e.getUUID().equals(targetUUID))
                .findFirst()
                .orElse(null);
        }
    }
}
