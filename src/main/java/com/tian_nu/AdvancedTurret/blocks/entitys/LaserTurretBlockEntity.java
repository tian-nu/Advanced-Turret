package com.tian_nu.AdvancedTurret.blocks.entitys;

import com.tian_nu.AdvancedTurret.Config;
import com.tian_nu.AdvancedTurret.blocks.LaserTurretBlock;
import com.tian_nu.AdvancedTurret.items.SmartChipItem;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
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

/**
 * 婵€鍏夌偖濉旀柟鍧楀疄浣?
 *
 * <p>鐗规€э細</p>
 * <ul>
 *   <li>鏃犲脊鑽秷鑰楋紝绾兘閲忛┍鍔?/li>
 *   <li>姣弔ick鎸佺画浼ゅ鐩爣</li>
 *   <li>鐐圭噧鏁堟灉</li>
 *   <li>鍏夋潫娓叉煋鍚屾</li>
 * </ul>
 *
 * @author tian_nu
 */
public class LaserTurretBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    // ========== 甯搁噺 ==========
    /** 姣弔ick浼ゅ */
    public static final float DAMAGE_PER_TICK = 2.0F;
    /** 鎼滅储鑼冨洿 */
    public static final double SEARCH_RADIUS = 32.0;
    /** 鐐圭噧鏃堕棿锛堢锛?*/
    public static final int FIRE_SECONDS = 3;

    public static float getDamagePerTick() { return (float) Config.laserDamagePerTick; }
    public static double getSearchRadius() { return Config.laserRange; }
    public static int getFireSeconds() { return Config.laserFireSeconds; }
    public static float getAimThreshold() { return (float) Config.laserAimThreshold; }
    public static float getTurnSpeed() { return (float) Config.laserTurnSpeed; }
    /** 鐬勫噯瑙掑害闃堝€硷紙寮у害锛夛紝绾?5搴?*/
    public static final float AIM_THRESHOLD = 0.26F;
    /** 杞悜閫熷害锛堝姬搴?tick锛夛紝绾?0搴?tick = 200搴?绉?*/
    public static final float TURN_SPEED = 0.18F;

    // ========== GeckoLib鏁版嵁鍚屾绁?==========
    public static SerializableDataTicket<Boolean> HAS_TARGET;
    public static SerializableDataTicket<Double> TARGET_POS_X;
    public static SerializableDataTicket<Double> TARGET_POS_Y;
    public static SerializableDataTicket<Double> TARGET_POS_Z;
    /** 鍏夋潫鏄惁婵€娲伙紙鐢ㄤ簬娓叉煋锛?*/
    public static SerializableDataTicket<Boolean> BEAM_ACTIVE;
    /** 灏勯€熷姞鎴愭暟閲忥紙鐢ㄤ簬婵€鍏夐€忔槑搴︼級 */
    public static SerializableDataTicket<Integer> FIRE_RATE_COUNT;

    // ========== GeckoLib鍔ㄧ敾缂撳瓨 ==========
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ========== 瀛楁 ==========
    private LivingEntity target = null;
    private int targetLostTicks = 0;
    private Vec3 visibleTargetPoint = null;

    public float yRot0 = 0.0f;
    public float xRot0 = 0.0f;
    
    /** 鐩爣瑙掑害锛堢敤浜庣瀯鍑嗗垽鏂級 */
    private float targetYRot = 0.0f;
    private float targetXRot = 0.0f;
    /** 鏄惁宸插畬鎴愮瀯鍑?*/
    private boolean isAimed = false;

    public LaserTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LASER_TURRET.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LaserTurretBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        TurretBaseBlockEntity base = blockEntity.getBaseEntity();
        if (base == null) return;

        // 鏃犵數閲忎綆澶村姩鐢?
        if (base.getEnergyStored() < Config.laserEnergyPerTick) {
            blockEntity.target = null;
            blockEntity.isAimed = false;
            blockEntity.setAnimData(HAS_TARGET, true);
            blockEntity.setAnimData(TARGET_POS_X, pos.getX() + 0.5);
            blockEntity.setAnimData(TARGET_POS_Y, pos.getY() - 2.0);
            blockEntity.setAnimData(TARGET_POS_Z, pos.getZ() + 0.5);
            blockEntity.setAnimData(BEAM_ACTIVE, false);
            // 杞悜褰掗浂
            blockEntity.yRot0 = blockEntity.lerpAngle(blockEntity.yRot0, 0);
            blockEntity.xRot0 = blockEntity.lerpAngle(blockEntity.xRot0, 0);
            return;
        }

        Direction facing = state.getValue(LaserTurretBlock.FACING);
        if (!base.isFaceEnabled(facing)) {
            blockEntity.target = null;
            blockEntity.isAimed = false;
            blockEntity.setAnimData(HAS_TARGET, false);
            blockEntity.setAnimData(BEAM_ACTIVE, false);
            // 杞悜褰掗浂
            blockEntity.yRot0 = blockEntity.lerpAngle(blockEntity.yRot0, 0);
            blockEntity.xRot0 = blockEntity.lerpAngle(blockEntity.xRot0, 0);
            return;
        }

        blockEntity.updateTarget(level, pos, base, facing);

        if (blockEntity.target != null && blockEntity.target.isAlive()) {
            // 鏇存柊鐩爣瑙掑害
            blockEntity.updateTargetAngles(pos);
            
            // 鏈嶅姟绔篃鏇存柊褰撳墠瑙掑害锛堟ā鎷熻浆鍚戣繃绋嬶級
            blockEntity.updateCurrentAngles();
            
            // 妫€鏌ユ槸鍚﹀凡鐬勫噯
            blockEntity.updateAimedState();
            
            if (blockEntity.isAimed && blockEntity.canHitTarget(blockEntity.target, level, pos)) {
                // 鎵ц浼ゅ
                blockEntity.dealDamageToTarget(blockEntity.target, base, level);

                // 娑堣€楄兘閲?
                base.consumeEnergy(Config.laserEnergyPerTick);

                // 鍚屾鍏夋潫浣嶇疆
                blockEntity.syncBeamPosition();
                
                // 鍚屾灏勯€熺粍浠舵暟閲忥紙鐢ㄤ簬閫忔槑搴︼級
                int fireRateCount = blockEntity.countFireRateComponents(base, facing);
                blockEntity.setAnimData(FIRE_RATE_COUNT, fireRateCount);
            } else {
                // 鏈瀯鍑嗘椂涓嶆樉绀哄厜鏉?
                blockEntity.setAnimData(BEAM_ACTIVE, false);
            }
            
            // 鍗充娇鏈瀯鍑嗕篃瑕佸悓姝ョ洰鏍囦綅缃紙璁╃偖濉旇浆鍚戯級
            if (blockEntity.visibleTargetPoint != null) {
                blockEntity.setAnimData(TARGET_POS_X, blockEntity.visibleTargetPoint.x);
                blockEntity.setAnimData(TARGET_POS_Y, blockEntity.visibleTargetPoint.y);
                blockEntity.setAnimData(TARGET_POS_Z, blockEntity.visibleTargetPoint.z);
                blockEntity.setAnimData(HAS_TARGET, true);
            }
            
            if (!blockEntity.isAimed || !blockEntity.canHitTarget(blockEntity.target, level, pos)) {
                // 鐩爣涓㈠け锛堟湭鐬勫噯浣嗙洰鏍囦笉鍙嚮涓椂锛?
                // 娉ㄦ剰锛氫笉鍙栨秷鐩爣锛岃鐐缁х画杞悜
            }
        } else {
            blockEntity.isAimed = false;
            blockEntity.setAnimData(BEAM_ACTIVE, false);
            // 娌℃湁鐩爣鏃讹紝杞悜褰掗浂
            blockEntity.yRot0 = blockEntity.lerpAngle(blockEntity.yRot0, 0);
            blockEntity.xRot0 = blockEntity.lerpAngle(blockEntity.xRot0, 0);
        }
    }

    // ========== GeckoLib鍔ㄧ敾鎺у埗 ==========

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> PlayState.CONTINUE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /**
     * 鑾峰彇杩炴帴鐨勭偖濉斿熀搴?
     */
    public TurretBaseBlockEntity getBaseEntity() {
        Level level = getLevel();
        if (level == null) return null;

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof LaserTurretBlock)) return null;

        Direction facing = state.getValue(LaserTurretBlock.FACING);
        BlockPos basePos = worldPosition.relative(facing.getOpposite());

        BlockEntity blockEntity = level.getBlockEntity(basePos);
        if (blockEntity instanceof TurretBaseBlockEntity base) {
            return base;
        }
        return null;
    }

    /**
     * 鏇存柊鐩爣
     */
    private void updateTarget(Level level, BlockPos pos, TurretBaseBlockEntity base, Direction facing) {
        if (target == null || !isValidTarget(target, level, pos)) {
            if (target != null && base.isThriftyMode()) {
                base.cancelReservation(target.getId());
            }
            target = findTarget(level, pos, base.getSearchRadiusForFace(facing, getSearchRadius()));
            targetLostTicks = 0;
            // 鏂扮洰鏍囬渶瑕侀噸鏂扮瀯鍑?
            isAimed = false;

            if (target != null && base.isThriftyMode()) {
                float expectedDamage = getExpectedDamage(base);
                base.reserveDamage(target.getId(), expectedDamage, target.getHealth(), level.getGameTime());
            }
        } else {
            if (!isTargetInRange(target, pos, base.getSearchRadiusForFace(facing, getSearchRadius()))) {
                targetLostTicks++;
                if (targetLostTicks > 20) {
                    if (base.isThriftyMode()) {
                        base.cancelReservation(target.getId());
                    }
                    target = null;
                    visibleTargetPoint = null;
                    isAimed = false;
                    setAnimData(HAS_TARGET, false);
                    setAnimData(BEAM_ACTIVE, false);
                }
            } else {
                Vec3 visiblePoint = getVisibleTargetPoint(target, level, pos);
                if (visiblePoint == null) {
                    targetLostTicks++;
                    if (targetLostTicks > 20) {
                        if (base.isThriftyMode()) {
                            base.cancelReservation(target.getId());
                        }
                        target = null;
                        visibleTargetPoint = null;
                        isAimed = false;
                        setAnimData(HAS_TARGET, false);
                        setAnimData(BEAM_ACTIVE, false);
                    }
                } else {
                    targetLostTicks = 0;
                    visibleTargetPoint = visiblePoint;
                    // 娉ㄦ剰锛氫笉鍦ㄨ繖閲岃缃瓸EAM_ACTIVE锛岀敱tick()涓殑isAimed鍒ゆ柇鍐冲畾
                }
            }
        }
    }

    /**
     * 妫€鏌ョ洰鏍囨槸鍚﹀彲琚嚮涓?
     */
    private boolean canHitTarget(LivingEntity target, Level level, BlockPos pos) {
        if (!target.isAlive()) return false;

        Direction facing = getBlockState().getValue(LaserTurretBlock.FACING);
        Vec3 start = calculateMuzzlePosition(pos, facing);
        Vec3 end = target.position().add(0, target.getEyeHeight() * 0.5, 0);

        Vec3 toTarget = end.subtract(start);
        if (toTarget.lengthSqr() < 1.0E-6) {
            return true;
        }

        Vec3 adjustedStart = start.add(toTarget.normalize().scale(0.6));
        net.minecraft.world.phys.BlockHitResult hitResult = level.clip(new net.minecraft.world.level.ClipContext(
                adjustedStart, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                null
        ));

        return hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }


    /**
     * 瀵圭洰鏍囬€犳垚浼ゅ
     */
    private void dealDamageToTarget(LivingEntity target, TurretBaseBlockEntity base, Level level) {
        // 娓呴櫎鏃犳晫甯?
        target.invulnerableTime = 0;
        target.hurtTime = 0;

        // 璁＄畻浼ゅ锛堝熀纭€浼ゅ + 闈㈤厤缃姞鎴愶級
        Direction facing = getBlockState().getValue(LaserTurretBlock.FACING);
        float damage = base.getDamageForFace(facing, getDamagePerTick());

        // 閫犳垚榄旀硶浼ゅ
        target.hurt(level.damageSources().magic(), damage);

        // 鐐圭噧鏁堟灉锛堟瘡20tick鍒锋柊涓€娆★級
        if (level.getGameTime() % 20 == 0) {
            target.setSecondsOnFire(getFireSeconds());
        }
    }

    /**
     * 鍚屾鍏夋潫浣嶇疆缁欏鎴风
     */
    private void syncBeamPosition() {
        setAnimData(BEAM_ACTIVE, true);
    }

    /**
     * 鑾峰彇棰勬湡浼ゅ锛堢敤浜庡帀琛岃妭绾︼級
     */
    public float getExpectedDamage(TurretBaseBlockEntity base) {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof LaserTurretBlock)) return getDamagePerTick();
        Direction facing = state.getValue(LaserTurretBlock.FACING);
        return base.getDamageForFace(facing, getDamagePerTick());
    }
    
    /**
     * 鏇存柊鐩爣瑙掑害锛堥渶瑕佽浆鍚戝埌鐨勮搴︼級
     */
    private void updateTargetAngles(BlockPos pos) {
        if (visibleTargetPoint == null) return;
        
        Direction facing = getBlockState().getValue(LaserTurretBlock.FACING);
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vec3 delta = new Vec3(visibleTargetPoint.x - center.x, visibleTargetPoint.y - center.y, visibleTargetPoint.z - center.z);
        
        // 鏍规嵁鏈濆悜杞崲鍧愭爣
        double dx = delta.x, dy = delta.y, dz = delta.z;
        switch (facing) {
            case NORTH -> { dz = -delta.y; dy = -delta.z; }
            case SOUTH -> { dz = delta.y; dy = delta.z; }
            case EAST -> { dx = -delta.y; dy = delta.x; }
            case WEST -> { dx = delta.y; dy = -delta.x; }
            case DOWN -> { dy = -dy; }
            case UP -> {}
        }
        
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        targetYRot = (float) -Math.atan2(dx, dz);
        targetXRot = (float) -Math.atan2(dy, horizontalDist);
        
        // UP鏈濆悜闇€瑕佺壒娈婂鐞?
        if (facing == Direction.UP || facing == Direction.EAST || facing == Direction.WEST) {
            targetYRot += (float) Math.PI;
        }
    }
    
    /**
     * 鏇存柊鐬勫噯鐘舵€侊紙妫€鏌ュ綋鍓嶈搴︽槸鍚︽帴杩戠洰鏍囪搴︼級
     */
    private void updateAimedState() {
        // 璁＄畻瑙掑害宸紙浣跨敤寮у害锛?
        float yRotDiff = Math.abs(normalizeAngle(targetYRot - yRot0));
        float xRotDiff = Math.abs(normalizeAngle(targetXRot - xRot0));
        
        // 涓や釜瑙掑害閮藉皬浜庨槇鍊兼墠绠楃瀯鍑嗗畬鎴?
        isAimed = yRotDiff < getAimThreshold() && xRotDiff < getAimThreshold();
    }
    
    /**
     * 褰掍竴鍖栬搴﹀埌 [-PI, PI]
     */
    private float normalizeAngle(float angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }
    
    /**
     * 鏇存柊褰撳墠瑙掑害锛堟湇鍔＄妯℃嫙杞悜杩囩▼锛?
     */
    private void updateCurrentAngles() {
        // 浣跨敤涓?GeoModel 鐩稿悓鐨?lerp 閫昏緫
        yRot0 = lerpAngle(yRot0, targetYRot);
        xRot0 = lerpAngle(xRot0, targetXRot);
    }
    
    /**
     * 瑙掑害鎻掑€硷紙鍥哄畾閫熷害杞悜锛?
     * 姣弔ick绉诲姩 TURN_SPEED 寮у害锛屾帴杩戠洰鏍囨椂鐩存帴瀵归綈
     */
    private float lerpAngle(float current, float target) {
        float diff = normalizeAngle(target - current);
        if (Math.abs(diff) < getTurnSpeed()) {
            return target; // 鎺ヨ繎鐩爣锛岀洿鎺ュ榻?
        }
        return current + Math.signum(diff) * getTurnSpeed();
    }

    /**
     * 璁＄畻鐐彛浣嶇疆
     */
    public Vec3 calculateMuzzlePosition(BlockPos pos, Direction facing) {
        double outwardOffset = 0;
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        if (facing == Direction.UP) {
            center = new Vec3(center.x, center.y + outwardOffset, center.z);
        } else if (facing == Direction.DOWN) {
            center = new Vec3(center.x, center.y - outwardOffset, center.z);
        } else {
            Vec3 outward = new Vec3(facing.getStepX(), 0, facing.getStepZ()).scale(outwardOffset);
            center = center.add(outward);
        }

        return center;
    }

    /**
     * 鎼滅储鐩爣
     */
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
            setAnimData(BEAM_ACTIVE, false);
            isAimed = false;
            return null;
        }

        Vec3 turretPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        LivingEntity closest = enemies.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(turretPos)))
                .orElse(null);

        if (closest != null) {
            Vec3 visiblePoint = getVisibleTargetPoint(closest, level, pos);
            if (visiblePoint != null) {
                visibleTargetPoint = visiblePoint;
                setAnimData(TARGET_POS_X, visiblePoint.x);
                setAnimData(TARGET_POS_Y, visiblePoint.y);
                setAnimData(TARGET_POS_Z, visiblePoint.z);
                setAnimData(HAS_TARGET, true);
                // 鏂扮洰鏍囬渶瑕侀噸鏂扮瀯鍑?
                isAimed = false;
                setAnimData(BEAM_ACTIVE, false);
            } else {
                setAnimData(HAS_TARGET, false);
                setAnimData(BEAM_ACTIVE, false);
                isAimed = false;
            }
        }

        return closest;
    }

    /**
     * 妫€鏌ユ槸鍚︿负鏈夋晥鐩爣
     */
    private boolean isValidTarget(LivingEntity entity, Level level, BlockPos pos) {
        if (!entity.isAlive()) return false;
        if (entity.isInvulnerable()) return false;

        TurretBaseBlockEntity base = getBaseEntity();
        if (base == null) return false;

        ItemStack pluginStack = base.getPluginStack();
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();

        List<String> blacklist = SmartChipItem.getBlacklist(pluginStack);
        boolean inBlacklist = blacklist.contains(entityId);

        List<String> whitelist = SmartChipItem.getWhitelist(pluginStack);
        if (whitelist.contains(entityId)) return false;

        if (!inBlacklist) {
            int flags = SmartChipItem.getTargetFlags(pluginStack);
            boolean matched = false;

            if ((flags & SmartChipItem.FLAG_HOSTILE) != 0 && entity instanceof Enemy) matched = true;
            if (!matched && (flags & SmartChipItem.FLAG_NEUTRAL) != 0 && entity instanceof NeutralMob) matched = true;
            if (!matched && (flags & SmartChipItem.FLAG_FRIENDLY) != 0 &&
                (entity instanceof Animal || entity instanceof AmbientCreature || entity instanceof WaterAnimal)) matched = true;
            if (!matched && (flags & SmartChipItem.FLAG_PLAYERS) != 0 &&
                entity instanceof Player p && !p.isCreative() && !p.isSpectator()) matched = true;

            if (!matched) return false;
        }

        // 鍙嬩激淇濇姢
        if (base.isFriendlyFire()) {
            java.util.UUID ownerId = base.getOwner();
            if (ownerId != null) {
                if (entity.getUUID().equals(ownerId)) return false;
                if (entity instanceof net.minecraft.world.entity.TamableAnimal tameable) {
                    java.util.UUID tameOwner = tameable.getOwnerUUID();
                    if (tameOwner != null && tameOwner.equals(ownerId)) return false;
                }
            }
        }

        Direction facing = getBlockState().getValue(LaserTurretBlock.FACING);
        double searchRadius = base.getSearchRadiusForFace(facing, getSearchRadius());
        if (!isTargetInRange(entity, pos, searchRadius)) return false;

        // 鍙鎬ф鏌?
        Vec3 visiblePoint = getVisibleTargetPoint(entity, level, pos);
        if (visiblePoint == null) return false;

        visibleTargetPoint = visiblePoint;

        // 鍘夎鑺傜害
        if (base.isThriftyMode()) {
            float expectedDamage = getExpectedDamage(base);
            float currentHealth = entity.getHealth();
            float reservedDamage = base.getReservedDamage(entity.getId());
            float remainingHealth = currentHealth - reservedDamage;
            if (remainingHealth <= 0) return false;
        }

        return true;
    }

    private boolean isTargetInRange(LivingEntity entity, BlockPos pos, double searchRadius) {
        return LinearTurretTargetingHelper.isTargetInRange(entity, pos, searchRadius);
    }

    private Vec3 getVisibleTargetPoint(LivingEntity entity, Level level, BlockPos pos) {
        Direction facing = getBlockState().getValue(LaserTurretBlock.FACING);
        Vec3 start = calculateMuzzlePosition(pos, facing);
        return LinearTurretTargetingHelper.findVisibleTargetPoint(level, pos, facing, start, entity);
    }

    private boolean canSeePoint(Level level, BlockPos pos, Vec3 start, Vec3 end) {
        Direction facing = getBlockState().getValue(LaserTurretBlock.FACING);
        return LinearTurretTargetingHelper.canSeePoint(level, pos, facing, start, end);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("YRot0", yRot0);
        tag.putFloat("XRot0", xRot0);
        tag.putFloat("TargetYRot", targetYRot);
        tag.putFloat("TargetXRot", targetXRot);
        tag.putBoolean("IsAimed", isAimed);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("YRot0")) yRot0 = tag.getFloat("YRot0");
        if (tag.contains("XRot0")) xRot0 = tag.getFloat("XRot0");
        if (tag.contains("TargetYRot")) targetYRot = tag.getFloat("TargetYRot");
        if (tag.contains("TargetXRot")) targetXRot = tag.getFloat("TargetXRot");
        if (tag.contains("IsAimed")) isAimed = tag.getBoolean("IsAimed");
    }

    /**
     * 璁＄畻灏勯€熺粍浠舵暟閲忥紙鐢ㄤ簬婵€鍏夐€忔槑搴︼級
     */
    private int countFireRateComponents(TurretBaseBlockEntity base, Direction facing) {
        net.minecraftforge.items.IItemHandler upgrades = base.getFaceUpgradeSlots(facing);
        int count = 0;
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack stack = upgrades.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(com.tian_nu.AdvancedTurret.items.ModItems.FIRE_RATE_COMPONENT.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
