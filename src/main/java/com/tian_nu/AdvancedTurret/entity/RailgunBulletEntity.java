package com.tian_nu.AdvancedTurret.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * 磁轨炮子弹
 * 
 * <p>穿透子弹类型，特点：</p>
 * <ul>
 *   <li>可穿透多个目标（默认3个）</li>
 *   <li>高速飞行（速度6.0，机枪是3.0）</li>
 *   <li>前几tick忽略方块碰撞（避免高速子弹边缘碰撞问题）</li>
 *   <li>击中方块后销毁</li>
 * </ul>
 * 
 * <h3>碰撞检测流程：</h3>
 * <ol>
 *   <li>生命周期检查</li>
 *   <li>检测实体碰撞 → 伤害后穿透继续飞行（记录已击中实体）</li>
 *   <li>穿透次数用完 → 销毁</li>
 *   <li>检测方块碰撞</li>
 *   <li>基座方块 → 销毁子弹</li>
 *   <li>炮塔自身 → 跳过（前2tick）</li>
 *   <li>前3tick → 跳过（高速子弹特殊处理）</li>
 *   <li>其他方块 → 销毁子弹</li>
 * </ol>
 * 
 * @author tian_nu
 * @see TurretProjectileEntity 父类
 */
public class RailgunBulletEntity extends TurretProjectileEntity {

    // ==================== 穿透属性 ====================
    
    /** 穿透次数（击中实体后减少，到0时销毁） */
    private int penetrationCount = 3;
    
    /** 已击中的实体ID集合（防止重复伤害同一实体） */
    private Set<Integer> hitEntities = new HashSet<>();
    
    /** 忽略方块碰撞的tick数（高速子弹在发射初期可能边缘碰撞方块） */
    private int ignoreBlockCollisionTicks = 3;
    
    // ==================== 构造函数 ====================
    
    public RailgunBulletEntity(EntityType<? extends RailgunBulletEntity> type, Level level) {
        super(type, level);
        this.lifetime = 120; // 更长生命周期，匹配高速和射程
    }
    
    public RailgunBulletEntity(Level level, double x, double y, double z, float damage) {
        super(ModEntities.RAILGUN_BULLET.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setDamage(damage);
        this.lifetime = 120;
    }
    
    // ==================== 属性访问器 ====================
    
    public void setPenetrationCount(int count) { this.penetrationCount = count; }
    public int getPenetrationCount() { return this.penetrationCount; }
    
    // ==================== 碰撞过滤 ====================
    
    /**
     * 覆盖：排除已击中的实体
     */
    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity entity) {
        return super.canHitEntity(entity) && !hitEntities.contains(entity.getId());
    }
    
    // ==================== 击中处理 ====================
    
    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity livingEntity) {
            // 白名单检查：穿透但不造成伤害
            if (shouldIgnoreDamage(livingEntity)) {
                return;
            }
            
            // 记录已击中的实体（防止重复伤害）
            hitEntities.add(livingEntity.getId());
            
            // 造成伤害
            dealDamage(livingEntity, this.getDamage());
            
            // 减少穿透次数
            penetrationCount--;
            
            // 穿透次数用完时销毁
            if (penetrationCount <= 0) {
                this.discard();
            }
            // 否则继续飞行，可以击中下一个目标
        }
    }
    
    // ==================== 碰撞检测 ====================
    
    /**
     * 自定义碰撞检测：分离检测实体和方块，支持穿透
     */
    @Override
    public void tick() {
        // 生命周期检查
        if (this.lifetime-- <= 0) {
            this.discard();
            return;
        }

        Vec3 currentPos = this.position();
        Vec3 movement = this.getDeltaMovement();
        Vec3 nextPos = currentPos.add(movement);

        if (!this.level().isClientSide) {
            // 1. 检测实体碰撞（优先）
            EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                this.level(), this, currentPos, nextPos, 
                this.getBoundingBox().expandTowards(movement).inflate(1.0),
                this::canHitEntity
            );
            
            if (entityHit != null && entityHit.getType() == HitResult.Type.ENTITY) {
                this.onHit(entityHit);
                // 穿透子弹：如果没被销毁，继续移动
                if (!this.isRemoved()) {
                    this.setPos(nextPos);
                }
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
                
                // 高速子弹特殊处理：前几tick忽略方块碰撞
                // 原因：高速子弹在发射初期，边缘可能先碰到附近的方块边缘
                if (ignoreBlockCollisionTicks > 0) {
                    ignoreBlockCollisionTicks--;
                    this.setPos(nextPos);
                    return;
                }
                
                // 其他方块：销毁
                this.onHit(blockHit);
                return;
            }
        }

        // 无碰撞，继续移动
        this.setPos(nextPos);
        
        // 客户端：电弧粒子效果
        if (this.level().isClientSide) {
            spawnElectricParticles();
        }
    }
    
    /**
     * 生成电弧粒子效果
     */
    private void spawnElectricParticles() {
        Vec3 pos = this.position();
        for (int i = 0; i < 2; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 0.3;
            double offsetY = (this.random.nextDouble() - 0.5) * 0.3;
            double offsetZ = (this.random.nextDouble() - 0.5) * 0.3;
            this.level().addParticle(
                ParticleTypes.ELECTRIC_SPARK,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                0, 0, 0
            );
        }
    }
    
    // ==================== NBT存储 ====================
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("PenetrationCount", this.penetrationCount);
        tag.putIntArray("HitEntities", hitEntities.stream().toList());
        tag.putInt("IgnoreBlockCollisionTicks", this.ignoreBlockCollisionTicks);
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("PenetrationCount")) {
            this.penetrationCount = tag.getInt("PenetrationCount");
        }
        if (tag.contains("HitEntities")) {
            for (int id : tag.getIntArray("HitEntities")) {
                hitEntities.add(id);
            }
        }
        if (tag.contains("IgnoreBlockCollisionTicks")) {
            this.ignoreBlockCollisionTicks = tag.getInt("IgnoreBlockCollisionTicks");
        }
    }
}
