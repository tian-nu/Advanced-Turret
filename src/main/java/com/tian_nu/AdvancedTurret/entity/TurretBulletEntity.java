package com.tian_nu.AdvancedTurret.entity;

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
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class TurretBulletEntity extends Projectile {

    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(TurretBulletEntity.class, EntityDataSerializers.FLOAT);
    private int lifetime = 100;
    
    public TurretBulletEntity(EntityType<? extends TurretBulletEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }
    
    public TurretBulletEntity(Level level, double x, double y, double z, float damage) {
        super(ModEntities.TURRET_BULLET.get(), level);
        this.setPos(x, y, z);
        this.setDamage(damage);
        this.setNoGravity(true);
    }
    
    public void setDamage(float damage) {
        this.entityData.set(DAMAGE, damage);
    }
    
    public float getDamage() {
        return this.entityData.get(DAMAGE);
    }
    
    @Override
    protected void defineSynchedData() {
        this.entityData.define(DAMAGE, 4.0F);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (this.lifetime-- <= 0) {
            this.discard();
            return;
        }
        
        Vec3 movement = this.getDeltaMovement();
        Vec3 nextPos = this.position().add(movement);
        
        if (!this.level().isClientSide) {
            HitResult hitResult = this.level().clip(new net.minecraft.world.level.ClipContext(
                this.position(), nextPos,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                this
            ));
            
            if (hitResult.getType() != HitResult.Type.MISS) {
                this.onHit(hitResult);
                return;
            }
            
            EntityHitResult entityHitResult = this.findHitEntity(this.position(), nextPos);
            if (entityHitResult != null) {
                this.onHitEntity(entityHitResult);
                return;
            }
        }
        
        this.setPos(nextPos);
    }
    
    protected EntityHitResult findHitEntity(Vec3 start, Vec3 end) {
        return ProjectileUtil.getEntityHitResult(
            this.level(),
            this,
            start,
            end,
            this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0),
            entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.equals(this.getOwner())
        );
    }
    
    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();
        if (entity instanceof LivingEntity livingEntity) {
            Entity owner = this.getOwner();
            if (owner instanceof LivingEntity livingOwner) {
                livingEntity.hurt(this.damageSources().mobProjectile(this, livingOwner), this.getDamage());
            } else {
                livingEntity.hurt(this.damageSources().mobProjectile(this, null), this.getDamage());
            }
        }
        this.discard();
    }
    
    @Override
    protected void onHitBlock(net.minecraft.world.phys.BlockHitResult result) {
        this.discard();
    }
    
    public void shoot(Vec3 direction, float speed) {
        this.setDeltaMovement(direction.normalize().scale(speed));
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", this.getDamage());
        tag.putInt("Lifetime", this.lifetime);
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Damage")) {
            this.setDamage(tag.getFloat("Damage"));
        }
        if (tag.contains("Lifetime")) {
            this.lifetime = tag.getInt("Lifetime");
        }
    }
    
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
