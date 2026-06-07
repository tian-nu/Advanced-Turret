package com.tian_nu.AdvancedTurret.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class GrenadeEntity extends TurretProjectileEntity {
    public static final String NEXT_FALL_DAMAGE_IMMUNE_TAG = "advanced_turret_grenade_next_fall_immune";
    private static final float FRIENDLY_EXPLOSION_DAMAGE_CAP = 4.0F;
    private static final double DEFAULT_BLAST_KNOCKBACK_HORIZONTAL = 0.24D;
    private static final double DEFAULT_BLAST_KNOCKBACK_VERTICAL = 0.18D;
    private static final int SLOWNESS_DURATION_TICKS = 60;
    private static final int SLOWNESS_AMPLIFIER = 3;

    private float directDamage = 5.0F;
    private float explosionDamage = 10.0F;
    private float explosionRadius = 3.0F;
    private boolean destroyBlocks = false;
    private double blastKnockbackHorizontal = DEFAULT_BLAST_KNOCKBACK_HORIZONTAL;
    private double blastKnockbackVertical = DEFAULT_BLAST_KNOCKBACK_VERTICAL;
    private boolean constantBlastKnockback = false;
    private double selfBlastKnockbackMultiplier = 1.0D;
    private boolean selfDamageDisabled = false;
    private boolean protectNextFallDamage = false;

    public GrenadeEntity(EntityType<? extends GrenadeEntity> type, Level level) {
        super(type, level);
        this.lifetime = 100;
    }

    public GrenadeEntity(Level level, double x, double y, double z, float damage) {
        this(ModEntities.GRENADE.get(), level, x, y, z, damage);
    }

    protected GrenadeEntity(EntityType<? extends GrenadeEntity> type, Level level, double x, double y, double z, float damage) {
        super(type, level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setDamage(damage);
        this.directDamage = damage;
        this.lifetime = 100;
    }

    public void setDirectDamage(float damage) {
        this.directDamage = damage;
    }

    public float getDirectDamage() {
        return this.directDamage;
    }

    public void setExplosionDamage(float damage) {
        this.explosionDamage = damage;
    }

    public float getExplosionDamage() {
        return this.explosionDamage;
    }

    public void setExplosionRadius(float radius) {
        this.explosionRadius = radius;
    }

    public float getExplosionRadius() {
        return this.explosionRadius;
    }

    public void setDestroyBlocks(boolean destroy) {
        this.destroyBlocks = destroy;
    }

    public boolean shouldDestroyBlocks() {
        return this.destroyBlocks;
    }

    public void setBlastKnockback(double horizontal, double vertical, boolean constantWithinRadius) {
        this.blastKnockbackHorizontal = horizontal;
        this.blastKnockbackVertical = vertical;
        this.constantBlastKnockback = constantWithinRadius;
    }

    public void setSelfBlastKnockbackMultiplier(double multiplier) {
        this.selfBlastKnockbackMultiplier = multiplier;
    }

    public void setSelfDamageDisabled(boolean disabled) {
        this.selfDamageDisabled = disabled;
    }

    public void setFallProtectionTicks(int ticks) {
        this.protectNextFallDamage = ticks > 0;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity livingEntity) {
            if (shouldIgnoreDamage(livingEntity)) {
                return;
            }
            dealDamage(livingEntity, this.directDamage);
            explode(this.position(), livingEntity);
            this.discard();
            return;
        }
        
        explode(this.position(), null);
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        explode(result.getLocation(), null);
        this.discard();
    }

    private void explode(Vec3 pos, LivingEntity directHitTarget) {
        Level level = this.level();
        if (destroyBlocks) {
            level.explode(this, pos.x, pos.y, pos.z, explosionRadius, false, Level.ExplosionInteraction.BLOCK);
        } else {
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7F, 1.0F);
        }

        spawnExplosionDust(pos);


        if (!level.isClientSide) {
            AABB area = new AABB(
                    pos.x - explosionRadius, pos.y - explosionRadius, pos.z - explosionRadius,
                    pos.x + explosionRadius, pos.y + explosionRadius, pos.z + explosionRadius
            );

            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area);
            for (LivingEntity entity : entities) {
                if (shouldIgnoreDamage(entity)) {
                    continue;
                }

                double distance = distanceToExplosion(entity, pos);
                if (distance > explosionRadius) {
                    continue;
                }

                applyBlastKnockback(entity, pos, distance);

                float damageMultiplier = entity == directHitTarget
                        ? 1.0F
                        : 1.0F - (float) (distance / explosionRadius);
                float finalDamage = explosionDamage * damageMultiplier;
                if (isSelfBlastTarget(entity) && selfDamageDisabled) {
                    finalDamage = 0.0F;
                } else if (isFriendlyBlastTarget(entity)) {
                    finalDamage = Math.min(finalDamage, FRIENDLY_EXPLOSION_DAMAGE_CAP);
                }

                if (finalDamage > 0.0F) {
                    dealDamage(entity, finalDamage);
                }
                applyGrenadeDebuff(entity);
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

    private void applyBlastKnockback(LivingEntity entity, Vec3 explosionPos, double distance) {

        if (distance >= explosionRadius) {
            return;
        }

        Vec3 push = entity.position().subtract(explosionPos);
        if (push.lengthSqr() < 1.0E-6D) {
            push = new Vec3(0.0D, 1.0D, 0.0D);
        }

        Vec3 direction = push.normalize();
        Vec3 currentVelocity = entity.getDeltaMovement();
        double multiplier = constantBlastKnockback ? 1.0D : (1.0D - (distance / explosionRadius));
        if (isSelfBlastTarget(entity)) {
            multiplier *= selfBlastKnockbackMultiplier;
        }
        Vec3 impulse = new Vec3(
                direction.x * blastKnockbackHorizontal * multiplier,
                Math.max(blastKnockbackVertical * 0.4D, direction.y * blastKnockbackVertical * multiplier),
                direction.z * blastKnockbackHorizontal * multiplier
        );
        entity.setDeltaMovement(currentVelocity.add(impulse));
        entity.fallDistance = 0.0F;
        if (protectNextFallDamage && isSelfBlastTarget(entity)) {
            entity.getPersistentData().putBoolean(NEXT_FALL_DAMAGE_IMMUNE_TAG, true);
        }
        entity.hurtMarked = true;
    }

    private boolean isFriendlyBlastTarget(LivingEntity entity) {
        Entity owner = this.getOwner();
        if (!(owner instanceof LivingEntity livingOwner)) {
            return false;
        }
        if (entity == livingOwner) {
            return true;
        }
        if (entity.isAlliedTo(livingOwner) || livingOwner.isAlliedTo(entity)) {
            return true;
        }
        if (entity instanceof TamableAnimal tameable) {
            return livingOwner.getUUID().equals(tameable.getOwnerUUID());
        }
        return false;
    }

    private boolean isSelfBlastTarget(LivingEntity entity) {
        return entity == this.getOwner();
    }

    private void applyGrenadeDebuff(LivingEntity entity) {
        if (isSelfBlastTarget(entity)) {
            return;
        }
        entity.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                SLOWNESS_DURATION_TICKS,
                SLOWNESS_AMPLIFIER,
                false,
                true
        ));
    }

    @Override
    public void tick() {
        if (this.lifetime-- <= 0) {
            this.discard();
            return;
        }

        Vec3 velocity = this.getDeltaMovement();
        this.setDeltaMovement(velocity.add(0.0D, -getGravityStrength(), 0.0D));

        Vec3 movement = this.getDeltaMovement();
        Vec3 currentPos = this.position();
        Vec3 nextPos = currentPos.add(movement);
        if (shouldDiscardForFlightLimits(nextPos)) {
            return;
        }

        if (!this.level().isClientSide) {
            EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                    this.level(), this, currentPos, nextPos,
                    this.getBoundingBox().expandTowards(movement).inflate(1.0D),
                    this::canHitEntity
            );

            if (entityHit != null && entityHit.getType() == HitResult.Type.ENTITY) {
                this.onHit(entityHit);
                return;
            }

            ClipContext context = new ClipContext(
                    currentPos, nextPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    this
            );
            BlockHitResult blockHit = this.level().clip(context);

            if (blockHit.getType() == HitResult.Type.BLOCK) {
                BlockPos hitBlockPos = blockHit.getBlockPos();

                if (basePos != null && hitBlockPos.equals(basePos)) {
                    this.onHit(blockHit);
                    return;
                }

                if (ignoreSourceBlockTicks > 0 && sourcePos != null && hitBlockPos.equals(sourcePos)) {
                    ignoreSourceBlockTicks--;
                    this.setPos(nextPos);
                    return;
                }

                this.onHit(blockHit);
                return;
            }
        }

        this.setPos(nextPos);

        if (this.level().isClientSide) {
            spawnSmokeTrail();
        }
    }

    private void spawnSmokeTrail() {
        Vec3 pos = this.position();
        for (int i = 0; i < 2; i++) {
            double offsetX = (this.random.nextDouble() - 0.5D) * 0.2D;
            double offsetY = (this.random.nextDouble() - 0.5D) * 0.2D;
            double offsetZ = (this.random.nextDouble() - 0.5D) * 0.2D;
            this.level().addParticle(
                    ParticleTypes.SMOKE,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    0.0D, 0.02D, 0.0D
            );
        }
    }

    protected double getGravityStrength() {
        return 0.05D;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("DirectDamage", this.directDamage);
        tag.putFloat("ExplosionDamage", this.explosionDamage);
        tag.putFloat("ExplosionRadius", this.explosionRadius);
        tag.putBoolean("DestroyBlocks", this.destroyBlocks);
        tag.putDouble("BlastKnockbackHorizontal", this.blastKnockbackHorizontal);
        tag.putDouble("BlastKnockbackVertical", this.blastKnockbackVertical);
        tag.putBoolean("ConstantBlastKnockback", this.constantBlastKnockback);
        tag.putDouble("SelfBlastKnockbackMultiplier", this.selfBlastKnockbackMultiplier);
        tag.putBoolean("SelfDamageDisabled", this.selfDamageDisabled);
        tag.putBoolean("ProtectNextFallDamage", this.protectNextFallDamage);
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
        if (tag.contains("BlastKnockbackHorizontal")) {
            this.blastKnockbackHorizontal = tag.getDouble("BlastKnockbackHorizontal");
        }
        if (tag.contains("BlastKnockbackVertical")) {
            this.blastKnockbackVertical = tag.getDouble("BlastKnockbackVertical");
        }
        if (tag.contains("ConstantBlastKnockback")) {
            this.constantBlastKnockback = tag.getBoolean("ConstantBlastKnockback");
        }
        if (tag.contains("SelfBlastKnockbackMultiplier")) {
            this.selfBlastKnockbackMultiplier = tag.getDouble("SelfBlastKnockbackMultiplier");
        }
        if (tag.contains("SelfDamageDisabled")) {
            this.selfDamageDisabled = tag.getBoolean("SelfDamageDisabled");
        }
        if (tag.contains("ProtectNextFallDamage")) {
            this.protectNextFallDamage = tag.getBoolean("ProtectNextFallDamage");
        } else if (tag.contains("FallProtectionTicks")) {
            this.protectNextFallDamage = tag.getInt("FallProtectionTicks") > 0;
        }
    }
}
