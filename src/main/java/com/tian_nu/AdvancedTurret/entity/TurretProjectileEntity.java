package com.tian_nu.AdvancedTurret.entity;

import com.tian_nu.AdvancedTurret.blocks.entitys.TurretBaseBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.MachineGunTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.RailgunTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.RocketTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.MissileTurretBlockEntity;
import com.tian_nu.AdvancedTurret.items.SmartChipItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * 炮塔子弹抽象基类
 * 
 * <p>提供通用的子弹行为框架，子类需要实现：</p>
 * <ul>
 *   <li>{@link #onHitEntity} - 击中实体时的处理逻辑</li>
 *   <li>{@link #tick()} - 自定义碰撞检测（推荐使用分离检测）</li>
 * </ul>
 * 
 * <h3>碰撞检测关键点：</h3>
 * <ul>
 *   <li>基座方块(basePos)：必须销毁子弹</li>
 *   <li>炮塔自身(sourcePos)：前几tick跳过</li>
 *   <li>建议使用分离检测：先检测实体，再检测方块</li>
 * </ul>
 * 
 * @author tian_nu
 * @see TurretBulletEntity 机枪子弹实现
 * @see RailgunBulletEntity 磁轨炮子弹实现
 */
public abstract class TurretProjectileEntity extends Projectile {

    // ==================== 数据同步 ====================
    
    /** 伤害值（客户端同步用） */
    protected static final EntityDataAccessor<Float> DAMAGE = 
        SynchedEntityData.defineId(TurretProjectileEntity.class, EntityDataSerializers.FLOAT);
    
    // ==================== 基础属性 ====================
    
    /** 生命周期(tick)，默认100 */
    protected int lifetime = 100;
    
    /** 炮塔方块位置（用于跳过自身碰撞） */
    protected BlockPos sourcePos = null;
    
    /** 基座方块位置（基座必须销毁子弹） */
    protected BlockPos basePos = null;
    
    /** 跳过炮塔自身碰撞的tick数 */
    protected int ignoreSourceBlockTicks = 2;
    
    // ==================== 构造函数 ====================
    
    public TurretProjectileEntity(EntityType<? extends TurretProjectileEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }
    
    // ==================== 属性访问器 ====================
    
    public void setSourcePos(BlockPos pos) { this.sourcePos = pos; }
    public BlockPos getSourcePos() { return this.sourcePos; }
    
    public void setBasePos(BlockPos pos) { this.basePos = pos; }
    public BlockPos getBasePos() { return this.basePos; }
    
    public void setDamage(float damage) { this.entityData.set(DAMAGE, damage); }
    public float getDamage() { return this.entityData.get(DAMAGE); }
    
    public void setLifetime(int ticks) { this.lifetime = ticks; }
    public int getLifetime() { return this.lifetime; }
    
    // ==================== 数据同步 ====================
    
    @Override
    protected void defineSynchedData() {
        this.entityData.define(DAMAGE, 4.0F);
    }
    
    // ==================== 核心逻辑 ====================
    
    /**
     * 子弹每tick更新
     * 
     * <p><b>注意：子类应覆盖此方法，使用分离碰撞检测！</b></p>
     * <p>推荐的碰撞检测流程：</p>
     * <ol>
     *   <li>生命周期检查</li>
     *   <li>使用 ProjectileUtil.getEntityHitResult 检测实体碰撞</li>
     *   <li>使用 level.clip 检测方块碰撞</li>
     *   <li>特殊处理：基座销毁、炮塔自身跳过</li>
     * </ol>
     */
    @Override
    public void tick() {
        super.tick();
        
        if (this.lifetime-- <= 0) {
            this.discard();
            return;
        }

        Vec3 movement = this.getDeltaMovement();
        Vec3 nextPos = this.position().add(movement);

        // 基类提供基本碰撞检测，子类应覆盖使用分离检测
        if (!this.level().isClientSide) {
            net.minecraft.world.entity.projectile.ProjectileUtil.getHitResultOnMoveVector(
                this, this::canHitEntity
            );
        }

        this.setPos(nextPos);
    }
    
    /**
     * 判断是否可以击中实体
     * 子类可覆盖实现特殊过滤逻辑（如穿透子弹排除已击中的实体）
     */
    protected boolean canHitEntity(Entity entity) {
        return entity instanceof LivingEntity living
                && living.isAlive()
                && !entity.equals(this.getOwner());
    }
    
    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.ENTITY) {
            onHitEntity((EntityHitResult) result);
        } else if (result.getType() == HitResult.Type.BLOCK) {
            onHitBlock((net.minecraft.world.phys.BlockHitResult) result);
        }
    }
    
    /**
     * 击中实体时的处理
     * <p>子类必须实现此方法</p>
     */
    @Override
    protected abstract void onHitEntity(EntityHitResult result);
    
    /**
     * 击中方块时的处理
     * <p>默认行为：销毁子弹</p>
     */
    @Override
    protected void onHitBlock(net.minecraft.world.phys.BlockHitResult result) {
        this.discard();
    }
    
    // ==================== 伤害处理 ====================
    
    /**
     * 检查目标是否在白名单中（应该被忽略）
     */
    protected boolean shouldIgnoreDamage(LivingEntity entity) {
        if (sourcePos == null) return false;
        
        BlockEntity be = this.level().getBlockEntity(sourcePos);
        TurretBaseBlockEntity base = getBaseFromBlockEntity(be);
        
        if (base != null) {
            ItemStack pluginStack = base.getPluginStack();
            if (!pluginStack.isEmpty() && pluginStack.getItem() instanceof SmartChipItem) {
                String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
                List<String> whitelist = SmartChipItem.getWhitelist(pluginStack);
                return whitelist.contains(entityId);
            }
        }
        return false;
    }
    
    /**
     * 从BlockEntity获取基座（支持炮塔方块和基座方块）
     */
    protected TurretBaseBlockEntity getBaseFromBlockEntity(BlockEntity be) {
        if (be instanceof TurretBaseBlockEntity base) {
            return base;
        } else if (be instanceof MachineGunTurretBlockEntity turret) {
            return turret.getBaseEntity();
        } else if (be instanceof RailgunTurretBlockEntity turret) {
            return turret.getBaseEntity();
        } else if (be instanceof RocketTurretBlockEntity turret) {
            return turret.getBaseEntity();
        } else if (be instanceof MissileTurretBlockEntity turret) {
            return turret.getBaseEntity();
        }
        return null;
    }
    
    /**
     * 清除目标无敌帧并造成伤害
     * <p>解决多炮塔同时攻击时伤害丢失问题</p>
     */
    protected void dealDamage(LivingEntity target, float damage) {
        // 清除无敌帧
        target.invulnerableTime = 0;
        target.hurtTime = 0;
        
        // 造成伤害
        Entity owner = this.getOwner();
        if (owner instanceof LivingEntity livingOwner) {
            target.hurt(this.damageSources().mobProjectile(this, livingOwner), damage);
        } else {
            target.hurt(this.damageSources().mobProjectile(this, null), damage);
        }
    }
    
    // ==================== 发射 ====================
    
    /**
     * 发射子弹
     * @param direction 飞行方向（会被归一化）
     * @param speed 飞行速度
     */
    public void shoot(Vec3 direction, float speed) {
        this.setDeltaMovement(direction.normalize().scale(speed));
    }
    
    // ==================== NBT存储 ====================
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", this.getDamage());
        tag.putInt("Lifetime", this.lifetime);
        tag.putInt("IgnoreSourceTicks", this.ignoreSourceBlockTicks);
        if (sourcePos != null) {
            tag.putInt("SourceX", sourcePos.getX());
            tag.putInt("SourceY", sourcePos.getY());
            tag.putInt("SourceZ", sourcePos.getZ());
        }
        if (basePos != null) {
            tag.putInt("BaseX", basePos.getX());
            tag.putInt("BaseY", basePos.getY());
            tag.putInt("BaseZ", basePos.getZ());
        }
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Damage")) this.setDamage(tag.getFloat("Damage"));
        if (tag.contains("Lifetime")) this.lifetime = tag.getInt("Lifetime");
        if (tag.contains("IgnoreSourceTicks")) this.ignoreSourceBlockTicks = tag.getInt("IgnoreSourceTicks");
        if (tag.contains("SourceX")) {
            sourcePos = new BlockPos(tag.getInt("SourceX"), tag.getInt("SourceY"), tag.getInt("SourceZ"));
        }
        if (tag.contains("BaseX")) {
            basePos = new BlockPos(tag.getInt("BaseX"), tag.getInt("BaseY"), tag.getInt("BaseZ"));
        }
    }
    
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
