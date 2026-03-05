package com.tian_nu.AdvancedTurret.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
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

    /** 加速度 (每tick速度增加 - 线性增长) */
    private double acceleration = 0.03;

    /** 转向速率 (每tick转向插值系数 0.0-1.0) */
    private double turnRate = 0.3;

    /** 追踪的目标实体 */
    private LivingEntity targetEntity = null;

    // ==================== 构造函数 ====================

    public MissileEntity(EntityType<? extends MissileEntity> type, Level level) {
        super(type, level);
        this.lifetime = 300; // 较长生命周期（15秒）
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

        // 爆炸模式：有破坏插件时破坏方块
        Level.ExplosionInteraction interaction = destroyBlocks
            ? Level.ExplosionInteraction.BLOCK
            : Level.ExplosionInteraction.NONE;

        // 创建爆炸效果（视觉+声音）
        level.explode(null, pos.x, pos.y, pos.z, explosionRadius, false, interaction);

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

                double distance = entity.position().distanceTo(pos);
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

    // ==================== 核心逻辑 ====================

    /**
     * 自定义碰撞检测：分离检测实体和方块 + 制导追踪 + 越飞越快
     */
    @Override
    public void tick() {
        // 生命周期检查
        if (this.lifetime-- <= 0) {
            this.discard();
            return;
        }

        Vec3 movement = this.getDeltaMovement();
        Vec3 currentPos = this.position();
        Vec3 nextPos = currentPos.add(movement);

        // ========== 制导追踪 ==========
        // 目标存活且在范围内时进行制导
        if (targetEntity != null && targetEntity.isAlive()) {
            Vec3 currentDir = movement.normalize();
            Vec3 toTarget = targetEntity.position().subtract(this.position()).normalize();

            // 转向插值：新方向 = 当前方向 + (目标方向 - 当前方向) * 转向速率
            // turnRate 控制转向灵敏度（0.0-1.0）
            Vec3 newDir = currentDir.add(toTarget.subtract(currentDir).scale(turnRate)).normalize();

            // 应用新方向（保持当前速度）
            double currentSpeed = movement.length();
            this.setDeltaMovement(newDir.scale(currentSpeed));

            // 更新 nextPos 供碰撞检测使用
            nextPos = currentPos.add(this.getDeltaMovement());
            movement = this.getDeltaMovement();
        }

        // ========== 越飞越快 ==========
        // 线性加速度：每tick速度增加 acceleration
        double currentSpeed = movement.length();
        if (currentSpeed > 0.001) {
            double newSpeed = currentSpeed + acceleration;
            Vec3 direction = movement.normalize();
            this.setDeltaMovement(direction.scale(newSpeed));

            // 更新 nextPos
            nextPos = currentPos.add(this.getDeltaMovement());
            movement = this.getDeltaMovement();
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
                    return;
                }

                // 其他方块：爆炸
                this.onHit(blockHit);
                return;
            }
        }

        // 无碰撞，继续移动
        this.setPos(nextPos);

        // 客户端：烟雾尾迹粒子
        if (this.level().isClientSide) {
            spawnSmokeTrail();
        }
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
