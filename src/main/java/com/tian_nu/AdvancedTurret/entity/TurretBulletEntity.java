package com.tian_nu.AdvancedTurret.entity;

import com.tian_nu.AdvancedTurret.blocks.entitys.TurretBaseBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.MachineGunTurretBlockEntity;
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
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class TurretBulletEntity extends Projectile {

    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(TurretBulletEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SOURCE_POS = SynchedEntityData.defineId(TurretBulletEntity.class, EntityDataSerializers.INT);
    
    private int lifetime = 100;
    private BlockPos sourcePos = null;
    private int ignoreSourceBlockTicks = 2;
    
    public TurretBulletEntity(EntityType<? extends TurretBulletEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }
    
    public TurretBulletEntity(Level level, double x, double y, double z, float damage) {
        super(ModEntities.TURRET_BULLET.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setDamage(damage);
        this.setNoGravity(true);
    }
    
    public void setSourcePos(BlockPos pos) {
        this.sourcePos = pos;
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
        this.entityData.define(SOURCE_POS, 0);
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
            HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitLivingEntity);
            if (hitResult.getType() != HitResult.Type.MISS) {
                if (hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit
                        && sourcePos != null
                        && blockHit.getBlockPos().equals(sourcePos)
                        && ignoreSourceBlockTicks > 0) {
                    ignoreSourceBlockTicks--;
                    this.setPos(nextPos);
                    return;
                }
                this.onHit(hitResult);
                return;
            }
        }

        this.setPos(nextPos);
    }

    private boolean canHitLivingEntity(Entity entity) {
        return entity instanceof LivingEntity living
                && living.isAlive()
                && !entity.equals(this.getOwner());
    }
    
    protected EntityHitResult findHitEntity(Vec3 start, Vec3 end) {
        return ProjectileUtil.getEntityHitResult(
            this.level(),
            this,
            start,
            end,
            this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0),
            entity -> entity instanceof LivingEntity
                    && entity.isAlive()
                    && !entity.equals(this.getOwner())
        );
    }
    
    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();
        if (entity instanceof LivingEntity livingEntity) {
            
            // 检查白名单免伤
            if (shouldIgnoreDamage(livingEntity)) {
                this.discard();
                return;
            }
            
            Entity owner = this.getOwner();
            livingEntity.invulnerableTime = 0;
            livingEntity.hurtTime = 0;
            if (owner instanceof LivingEntity livingOwner) {
                livingEntity.hurt(this.damageSources().mobProjectile(this, livingOwner), this.getDamage());
            } else {
                livingEntity.hurt(this.damageSources().mobProjectile(this, null), this.getDamage());
            }
        }
        this.discard();
    }
    
    private boolean shouldIgnoreDamage(LivingEntity entity) {
        if (sourcePos == null) return false;
        
        BlockEntity be = this.level().getBlockEntity(sourcePos);
        TurretBaseBlockEntity base = null;
        
        if (be instanceof TurretBaseBlockEntity) {
            base = (TurretBaseBlockEntity) be;
        } else if (be instanceof MachineGunTurretBlockEntity turret) {
            base = turret.getBaseEntity();
        } else if (be instanceof com.tian_nu.AdvancedTurret.blocks.entitys.RailgunTurretBlockEntity turret) {
            base = turret.getBaseEntity();
        }
        
        if (base != null) {
            ItemStack pluginStack = base.getPluginStack();
            if (!pluginStack.isEmpty() && pluginStack.getItem() instanceof SmartChipItem) {
                String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
                List<String> whitelist = SmartChipItem.getWhitelist(pluginStack);
                if (whitelist.contains(entityId)) {
                    return true;
                }
            }
        }
        return false;
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
        if (sourcePos != null) {
            tag.putInt("SourceX", sourcePos.getX());
            tag.putInt("SourceY", sourcePos.getY());
            tag.putInt("SourceZ", sourcePos.getZ());
        }
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
        if (tag.contains("SourceX")) {
            sourcePos = new BlockPos(
                tag.getInt("SourceX"),
                tag.getInt("SourceY"),
                tag.getInt("SourceZ")
            );
        }
    }
    
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
