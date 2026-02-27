package com.tian_nu.AdvancedTurret.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 机枪炮塔子弹
 * 
 * <p>基础子弹类型，特点：</p>
 * <ul>
 *   <li>击中实体后销毁</li>
 *   <li>击中方块后销毁</li>
 *   <li>使用分离碰撞检测，实体优先</li>
 * </ul>
 * 
 * <h3>碰撞检测流程：</h3>
 * <ol>
 *   <li>生命周期检查</li>
 *   <li>检测实体碰撞 → 击中则销毁</li>
 *   <li>检测方块碰撞</li>
 *   <li>基座方块 → 销毁子弹</li>
 *   <li>炮塔自身 → 跳过（前2tick）</li>
 *   <li>其他方块 → 销毁子弹</li>
 * </ol>
 * 
 * @author tian_nu
 * @see TurretProjectileEntity 父类
 */
public class TurretBulletEntity extends TurretProjectileEntity {

    public TurretBulletEntity(EntityType<? extends TurretBulletEntity> type, Level level) {
        super(type, level);
    }
    
    public TurretBulletEntity(Level level, double x, double y, double z, float damage) {
        super(ModEntities.TURRET_BULLET.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setDamage(damage);
    }
    
    // ==================== 击中处理 ====================
    
    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity livingEntity) {
            // 白名单检查：击中白名单实体则销毁但不造成伤害
            if (shouldIgnoreDamage(livingEntity)) {
                this.discard();
                return;
            }
            
            // 造成伤害（基类方法会清除无敌帧）
            dealDamage(livingEntity, this.getDamage());
        }
        
        // 击中后销毁
        this.discard();
    }
    
    // ==================== 碰撞检测 ====================
    
    /**
     * 自定义碰撞检测：分离检测实体和方块，实体优先
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
                
                // 其他方块：销毁
                this.onHit(blockHit);
                return;
            }
        }

        // 无碰撞，继续移动
        this.setPos(nextPos);
    }
}
