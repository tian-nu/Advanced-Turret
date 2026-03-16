package com.tian_nu.AdvancedTurret.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 垃圾投射物实体
 * 
 * <p>特点：</p>
 * <ul>
 *   <li>抛物线弹道：受重力影响</li>
 *   <li>任意物品作弹药：渲染为使用的弹药物品</li>
 *   <li>低伤害低成本</li>
 * </ul>
 * 
 * @author tian_nu
 */
public class JunkProjectileEntity extends TurretProjectileEntity {

    // ==================== 投射物属性 ====================
    
    /** 使用的弹药物品 */
    private ItemStack ammoItem = ItemStack.EMPTY;
    
    /** 重力常数 */
    private static final double GRAVITY = 0.05;
    
    // ==================== 构造函数 ====================
    
    public JunkProjectileEntity(EntityType<? extends JunkProjectileEntity> type, Level level) {
        super(type, level);
        this.lifetime = 60; // 3秒
    }
    
    public JunkProjectileEntity(Level level, double x, double y, double z, float damage) {
        super(ModEntities.JUNK_PROJECTILE.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setDamage(damage);
        this.lifetime = 60;
    }
    
    // ==================== 属性访问器 ====================
    
    public void setAmmoItem(ItemStack stack) { 
        this.ammoItem = stack.copy(); 
    }
    public ItemStack getAmmoItem() { return this.ammoItem; }
    
    // ==================== 击中处理 ====================
    
    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity livingEntity) {
            // 白名单检查
            if (shouldIgnoreDamage(livingEntity)) {
                return;
            }
            
            // 造成伤害
            dealDamage(livingEntity, this.getDamage());
        }
        
        this.discard();
    }
    
    @Override
    protected void onHitBlock(BlockHitResult result) {
        // 击中方块后销毁
        this.discard();
    }
    
    // ==================== 碰撞检测 ====================
    
    /**
     * 自定义碰撞检测：分离检测实体和方块 + 重力
     */
    @Override
    public void tick() {
        // 生命周期检查
        if (this.lifetime-- <= 0) {
            this.discard();
            return;
        }

        // 应用重力（抛物线弹道）
        Vec3 velocity = this.getDeltaMovement();
        this.setDeltaMovement(velocity.add(0, -GRAVITY, 0));

        Vec3 movement = this.getDeltaMovement();
        Vec3 currentPos = this.position();
        Vec3 nextPos = currentPos.add(movement);
        if (shouldDiscardForFlightLimits(nextPos)) {
            return;
        }

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
    
    // ==================== NBT存储 ====================
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!ammoItem.isEmpty()) {
            tag.put("AmmoItem", ammoItem.save(new CompoundTag()));
        }
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AmmoItem")) {
            ammoItem = ItemStack.of(tag.getCompound("AmmoItem"));
        }
    }
}
