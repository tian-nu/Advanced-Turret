package com.tian_nu.AdvancedTurret.blocks.entitys;

import com.tian_nu.AdvancedTurret.Config;
import com.tian_nu.AdvancedTurret.blocks.ModBlocks;
import com.tian_nu.AdvancedTurret.blocks.entitys.MachineGunTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.MissileTurretBlockEntity;
import com.tian_nu.AdvancedTurret.items.ModItems;
import com.tian_nu.AdvancedTurret.items.SmartChipItem;
import com.tian_nu.AdvancedTurret.gui.TurretMenu;
import com.tian_nu.AdvancedTurret.data.RemoteTerminalBaseIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.inventory.ContainerData;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Core block entity for the turret base.
 *
 * <p>Handles shared energy, ammo storage, plugin slots, per-face upgrades,
 * ownership, targeting preferences, and menu synchronization.</p>
 *
 * @author tian_nu
 */
public class TurretBaseBlockEntity extends BlockEntity implements MenuProvider {
    
    // ========== Mounted Turret Types ==========
    
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> TurretBaseBlockEntity.this.energyStorage.getEnergyStored();
                case 1 -> TurretBaseBlockEntity.this.getMaxEnergyStored();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // 菜单数据只读；值来源于方块实体状态。
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    // ========== Mounted Turret Types ==========
    
    /** Maximum energy by base tier (T1..T5). */
    public static final int[] MAX_ENERGIES = {10000, 40000, 100000, 250000, 500000};
    /** Default transfer rate before tier-specific overrides are applied. */
    public static final int MAX_TRANSFER_RATE = 1000;
    
    /** Shared ammo slots on the base. */
    public static final int AMMO_SLOTS = 9;
    /** Maximum plugin slots available on high-tier bases. */
    public static final int MAX_PLUGIN_SLOTS = 2;
    /** Maximum upgrade slots available per mounted face. */
    public static final int MAX_UPGRADE_SLOTS_PER_FACE = 3;
    
    // ========== Mounted Turret Types ==========
    
    public enum TurretType {
        NONE,
        MACHINE_GUN,
        RAILGUN
    }
    
    // ========== Base State ==========
    
    // 6个面（每位对应一个 Direction：0=下, 1=上, 2=北, 3=南, 4=西, 5=东）。
    // 0b111111 = 默认全部6个面启用。将该位设为0即可禁用对应面。
    // 禁用某个面后，该面上的炮塔将不会开火或搜索目标。
    
    private java.util.UUID owner;
    private String ownerName = "";
    private Component customName;
    /** 已启用的炮塔面位掩码（每个 Direction 占一位，0=下..5=东）。0b111111 = 全部启用。 */
    private byte enabledFacesMask = 0b111111;
    /** 手动射程限制（格，0 = 使用炮塔默认射程）。范围限制在 [0, 256] 内。 */
    private double manualRangeLimit = 0.0D;
    /** T5 基座的内置智能芯片（始终存在，始终位于槽位 0）。 */
    private ItemStack builtInSmartChip = ItemStack.EMPTY;

	// ========== Mounted Turret Types ==========
	
	/** 实体ID → 已预扣伤害量的映射。用于多个炮塔面之间的伤害协调。 */
	private final Map<Integer, Float> reservedDamage = new HashMap<>();
	/** 实体ID → 预扣发起时间的映射。用于超时追踪。 */
	private final Map<Integer, Long> reservationTime = new HashMap<>();
	/** 伤害预扣的超时时间（tick）。超过此时间后预扣将过期。200 tick = 10 秒。 */
	private static final long RESERVATION_TIMEOUT = 200; // 200 tick = 10 秒

	// ========== Mounted Turret Types ==========
    
    private int currentTransferRate = getMaxTransferRateForTier();
    private BaseEnergyStorage energyStorage = createEnergyStorage(getMaxEnergyForTier(), currentTransferRate);
    
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);
    
    // ========== Mounted Turret Types ==========
    
    private final ItemStackHandler ammoInventory = new ItemStackHandler(AMMO_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            syncToClient();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isValidAmmoForMountedTurrets(stack);
        }
    };
    
    // 基座的插件槽物品栏。用于存放智能芯片和功能插件。
    // 槽位数量取决于等级：T1=0, T2-T3=1, T4-T5=2。
    // 下方还有 isItemValid() 校验，通过 getPluginSlotCount() 控制。
    private final ItemStackHandler basePluginSlot = new ItemStackHandler(MAX_PLUGIN_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            checkCreativePowerComponent();
            syncToClient();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // 校验插件槽：先检查槽位数，再检查物品是否为可识别的插件。
            if (slot >= getPluginSlotCount()) return false;
            return hasPluginSlot() && isPluginItem(stack);
        }
    };

    private final ItemStackHandler[] faceUpgradeSlots = new ItemStackHandler[] {
            createFaceUpgradeHandler(Direction.DOWN),
            createFaceUpgradeHandler(Direction.UP),
            createFaceUpgradeHandler(Direction.NORTH),
            createFaceUpgradeHandler(Direction.SOUTH),
            createFaceUpgradeHandler(Direction.WEST),
            createFaceUpgradeHandler(Direction.EAST)
    };

    private ItemStackHandler createFaceUpgradeHandler(Direction face) {
        return new ItemStackHandler(MAX_UPGRADE_SLOTS_PER_FACE) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                syncToClient();
            }

            @Override
            public int getSlotLimit(int slot) {
                return 4;
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                if (slot >= getUpgradeSlotsPerFace()) return false;
                return isUpgradeComponent(stack);
            }
        };
    }
    
    private final IItemHandler combinedInventory = new IItemHandler() {
        @Override
        public int getSlots() {
            return AMMO_SLOTS + MAX_PLUGIN_SLOTS + (6 * MAX_UPGRADE_SLOTS_PER_FACE);
        }
        
        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if (slot < AMMO_SLOTS) return ammoInventory.getStackInSlot(slot);
            if (slot < AMMO_SLOTS + MAX_PLUGIN_SLOTS) return basePluginSlot.getStackInSlot(slot - AMMO_SLOTS);
            int faceSlot = slot - AMMO_SLOTS - MAX_PLUGIN_SLOTS;
            int faceIndex = faceSlot / MAX_UPGRADE_SLOTS_PER_FACE;
            int upgradeIndex = faceSlot % MAX_UPGRADE_SLOTS_PER_FACE;
            if (faceIndex < 0 || faceIndex >= 6) return ItemStack.EMPTY;
            return faceUpgradeSlots[faceIndex].getStackInSlot(upgradeIndex);
        }
        
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot < AMMO_SLOTS) return ammoInventory.insertItem(slot, stack, simulate);
            if (slot < AMMO_SLOTS + MAX_PLUGIN_SLOTS) return basePluginSlot.insertItem(slot - AMMO_SLOTS, stack, simulate);
            int faceSlot = slot - AMMO_SLOTS - MAX_PLUGIN_SLOTS;
            int faceIndex = faceSlot / MAX_UPGRADE_SLOTS_PER_FACE;
            int upgradeIndex = faceSlot % MAX_UPGRADE_SLOTS_PER_FACE;
            if (faceIndex < 0 || faceIndex >= 6) return stack;
            return faceUpgradeSlots[faceIndex].insertItem(upgradeIndex, stack, simulate);
        }
        
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < AMMO_SLOTS) return ammoInventory.extractItem(slot, amount, simulate);
            if (slot < AMMO_SLOTS + MAX_PLUGIN_SLOTS) return basePluginSlot.extractItem(slot - AMMO_SLOTS, amount, simulate);
            int faceSlot = slot - AMMO_SLOTS - MAX_PLUGIN_SLOTS;
            int faceIndex = faceSlot / MAX_UPGRADE_SLOTS_PER_FACE;
            int upgradeIndex = faceSlot % MAX_UPGRADE_SLOTS_PER_FACE;
            if (faceIndex < 0 || faceIndex >= 6) return ItemStack.EMPTY;
            return faceUpgradeSlots[faceIndex].extractItem(upgradeIndex, amount, simulate);
        }
        
        @Override
        public int getSlotLimit(int slot) {
            if (slot < AMMO_SLOTS) return ammoInventory.getSlotLimit(slot);
            if (slot < AMMO_SLOTS + MAX_PLUGIN_SLOTS) return basePluginSlot.getSlotLimit(slot - AMMO_SLOTS);
            int faceSlot = slot - AMMO_SLOTS - MAX_PLUGIN_SLOTS;
            int faceIndex = faceSlot / MAX_UPGRADE_SLOTS_PER_FACE;
            int upgradeIndex = faceSlot % MAX_UPGRADE_SLOTS_PER_FACE;
            if (faceIndex < 0 || faceIndex >= 6) return 0;
            return faceUpgradeSlots[faceIndex].getSlotLimit(upgradeIndex);
        }
        
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot < AMMO_SLOTS) return ammoInventory.isItemValid(slot, stack);
            if (slot < AMMO_SLOTS + MAX_PLUGIN_SLOTS) return basePluginSlot.isItemValid(slot - AMMO_SLOTS, stack);
            int faceSlot = slot - AMMO_SLOTS - MAX_PLUGIN_SLOTS;
            int faceIndex = faceSlot / MAX_UPGRADE_SLOTS_PER_FACE;
            int upgradeIndex = faceSlot % MAX_UPGRADE_SLOTS_PER_FACE;
            if (faceIndex < 0 || faceIndex >= 6) return false;
            return faceUpgradeSlots[faceIndex].isItemValid(upgradeIndex, stack);
        }
    };
    
    private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> combinedInventory);
    
    // ========== Mounted Turret Types ==========
    
    public TurretBaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_BASE.get(), pos, state);
        if (getTier() >= 5) {
            builtInSmartChip = new ItemStack(ModItems.SMART_CHIP.get());
        }
    }
    
    // ========== Mounted Turret Types ==========
    
    public int getTier() {
        BlockState state = getBlockState();
        if (state.is(ModBlocks.TURRET_BASE_T5.get())) return 5;
        if (state.is(ModBlocks.TURRET_BASE_T4.get())) return 4;
        if (state.is(ModBlocks.TURRET_BASE_T3.get())) return 3;
        if (state.is(ModBlocks.TURRET_BASE_T2.get())) return 2;
        if (state.is(ModBlocks.TURRET_BASE_T1.get())) return 1;
        return 1;
    }

    private int getMaxEnergyForTier() {
        int tier = getTier();
        int idx = Math.max(0, Math.min(MAX_ENERGIES.length - 1, tier - 1));
        return MAX_ENERGIES[idx];
    }

    private BaseEnergyStorage createEnergyStorage(int capacity, int transferRate) {
        return new BaseEnergyStorage(capacity, transferRate, transferRate);
    }

    private void ensureEnergyCapacity() {
        int desired = getMaxEnergyForTier();
        int transferRate = getMaxTransferRateForTier();
        if (energyStorage.getMaxEnergyStored() == desired && currentTransferRate == transferRate) return;
        int stored = energyStorage.getEnergyStored();
        BaseEnergyStorage next = createEnergyStorage(desired, transferRate);
        next.setEnergyStored(Math.min(stored, desired));
        energyStorage = next;
        currentTransferRate = transferRate;
    }

    private int getMaxTransferRateForTier() {
        return switch (getTier()) {
            case 1 -> Config.turretBaseMaxTransferRateT1 > 0 ? Config.turretBaseMaxTransferRateT1 : 100;
            case 2 -> Config.turretBaseMaxTransferRateT2 > 0 ? Config.turretBaseMaxTransferRateT2 : 200;
            case 3 -> Config.turretBaseMaxTransferRateT3 > 0 ? Config.turretBaseMaxTransferRateT3 : 600;
            case 4 -> Config.turretBaseMaxTransferRateT4 > 0 ? Config.turretBaseMaxTransferRateT4 : 4000;
            case 5 -> Config.turretBaseMaxTransferRateT5 > 0 ? Config.turretBaseMaxTransferRateT5 : 10000;
            default -> MAX_TRANSFER_RATE;
        };
    }

    public boolean hasBuiltInSmartChip() {
        return getTier() >= 5;
    }

    private ItemStack getOrCreateBuiltInSmartChip() {
        if (!hasBuiltInSmartChip()) {
            return ItemStack.EMPTY;
        }
        if (builtInSmartChip.isEmpty() || !(builtInSmartChip.getItem() instanceof SmartChipItem)) {
            builtInSmartChip = new ItemStack(ModItems.SMART_CHIP.get());
        }
        return builtInSmartChip;
    }

    private ItemStack getBuiltInSmartChipIfPresent() {
        if (!hasBuiltInSmartChip()) {
            return ItemStack.EMPTY;
        }
        if (builtInSmartChip.isEmpty() || !(builtInSmartChip.getItem() instanceof SmartChipItem)) {
            return ItemStack.EMPTY;
        }
        return builtInSmartChip;
    }

    public int getUpgradeSlotsPerFace() {
        return switch (getTier()) {
            case 1 -> 0;
            case 2 -> 1;
            case 3 -> 2;
            case 4 -> 2;
            case 5 -> 3;
            default -> 0;
        };
    }

    /**
     * 返回当前等级下的可用插件槽数量。
     * T1: 0 槽, T2-T3: 1 槽, T4-T5: 2 槽。
     */
    public int getPluginSlotCount() {
        return switch (getTier()) {
            case 1 -> 0;
            case 2, 3 -> 1;
            case 4, 5 -> 2;
            default -> 0;
        };
    }

    public boolean hasPluginSlot() {
        return getTier() >= 2;
    }

    public IItemHandler getAmmoInventory() {
        return ammoInventory;
    }

    public IItemHandler getBasePluginSlot() {
        return basePluginSlot;
    }

    public IItemHandler getFaceUpgradeSlots(Direction face) {
        return faceUpgradeSlots[face.get3DDataValue()];
    }
    
    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }
    
    /**
     * 返回当前能量值。调用前确保等级容量是最新的。
     */
     */
    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }
    
    /**
     * 返回最大能量容量，确保存储容量与当前等级匹配。
     */
     */
    public int getMaxEnergyStored() {
        ensureEnergyCapacity();
        return energyStorage.getMaxEnergyStored();
    }
    
    public void consumeEnergy(int amount) {
        ensureEnergyCapacity();
        int extracted = energyStorage.extractEnergy(amount, false);
        if (extracted > 0) {
            setChanged();
            syncToClient();
        }
    }
    
    public ItemStack getPluginStack() {
        // 在插件槽中找到第一个非空的 SmartChipItem，否则回退到内置芯片。
        int slotCount = Math.min(getPluginSlotCount(), basePluginSlot.getSlots());
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = basePluginSlot.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof SmartChipItem) {
                return stack;
            }
        }
        return getBuiltInSmartChipIfPresent();
    }
    
    /**
     * 返回所有插件物品堆（实体槽位 + 若无实体芯片则包含内置智能芯片）。
     */
     */
    public java.util.List<ItemStack> getAllPluginStacks() {
        java.util.List<ItemStack> plugins = new java.util.ArrayList<>();
        int slotCount = Math.min(getPluginSlotCount(), basePluginSlot.getSlots());
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = basePluginSlot.getStackInSlot(i);
            if (!stack.isEmpty()) {
                plugins.add(stack);
            }
        }
        ItemStack builtInChip = getBuiltInSmartChipIfPresent();
        if (!hasRealSmartChip() && !builtInChip.isEmpty()) {
            plugins.add(builtInChip);
        }
        return plugins;
    }

    private boolean hasRealSmartChip() {
        int slotCount = Math.min(getPluginSlotCount(), basePluginSlot.getSlots());
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = basePluginSlot.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof SmartChipItem) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isFriendlyFire() {
        ItemStack stack = getPluginStack();
        if (!stack.isEmpty()) {
            return com.tian_nu.AdvancedTurret.items.SmartChipItem.isFriendlyFire(stack);
        }
        return false;
    }

    public boolean isPredictiveAiming() {
        ItemStack stack = getPluginStack();
        if (!stack.isEmpty()) {
            return com.tian_nu.AdvancedTurret.items.SmartChipItem.isPredictiveAiming(stack);
        }
        return false;
    }

    public byte getEnabledFacesMask() {
        return enabledFacesMask;
    }
    
    public boolean isFaceEnabled(Direction face) {
        if (!hasTurretOnFace(face)) return false;
        return (getEnabledFacesMask() & (1 << face.get3DDataValue())) != 0;
    }

    public boolean hasTurretOnFace(Direction face) {
        Level level = getLevel();
        if (level == null) return false;
        BlockPos turretPos = getBlockPos().relative(face);
        BlockEntity be = level.getBlockEntity(turretPos);
        if (be instanceof MachineGunTurretBlockEntity turret) {
            TurretBaseBlockEntity base = turret.getBaseEntity();
            return base != null && base.getBlockPos().equals(getBlockPos());
        }
        if (be instanceof RailgunTurretBlockEntity turret) {
            TurretBaseBlockEntity base = turret.getBaseEntity();
            return base != null && base.getBlockPos().equals(getBlockPos());
        }
        if (be instanceof RocketTurretBlockEntity turret) {
            TurretBaseBlockEntity base = turret.getBaseEntity();
            return base != null && base.getBlockPos().equals(getBlockPos());
        }
        if (be instanceof MissileTurretBlockEntity turret) {
            TurretBaseBlockEntity base = turret.getBaseEntity();
            return base != null && base.getBlockPos().equals(getBlockPos());
        }
        if (be instanceof LaserTurretBlockEntity turret) {
            TurretBaseBlockEntity base = turret.getBaseEntity();
            return base != null && base.getBlockPos().equals(getBlockPos());
        }
        if (be instanceof GrenadeLauncherTurretBlockEntity turret) {
            TurretBaseBlockEntity base = turret.getBaseEntity();
            return base != null && base.getBlockPos().equals(getBlockPos());
        }
        if (be instanceof JunkTurretBlockEntity turret) {
            TurretBaseBlockEntity base = turret.getBaseEntity();
            return base != null && base.getBlockPos().equals(getBlockPos());
        }
        if (be instanceof PhaseFieldTurretBlockEntity turret) {
            TurretBaseBlockEntity base = turret.getBaseEntity();
            return base != null && base.getBlockPos().equals(getBlockPos());
        }
        if (be instanceof ResonanceFieldTurretBlockEntity turret) {
            TurretBaseBlockEntity base = turret.getBaseEntity();
            return base != null && base.getBlockPos().equals(getBlockPos());
        }
        return false;
    }
    
    // Setter 方法现在更新 ItemStack（更新找到的第一个芯片）
    public void setFriendlyFire(boolean friendlyFire) {
        ItemStack stack = getPluginStack();
        if (stack.isEmpty() && hasBuiltInSmartChip()) {
            stack = getOrCreateBuiltInSmartChip();
        }
        if (!stack.isEmpty()) {
            com.tian_nu.AdvancedTurret.items.SmartChipItem.setFriendlyFire(stack, friendlyFire);
            setChanged();
        }
    }

    public void setPredictiveAiming(boolean predictiveAiming) {
        ItemStack stack = getPluginStack();
        if (stack.isEmpty() && hasBuiltInSmartChip()) {
            stack = getOrCreateBuiltInSmartChip();
        }
        if (!stack.isEmpty()) {
            com.tian_nu.AdvancedTurret.items.SmartChipItem.setPredictiveAiming(stack, predictiveAiming);
            setChanged();
        }
    }

    public void setEnabledFacesMask(byte enabledFacesMask) {
        this.enabledFacesMask = enabledFacesMask;
        setChanged();
        syncToClient();
    }
    
    public void setFaceEnabled(Direction face, boolean enabled) {
        byte mask = enabledFacesMask;
        if (enabled) {
            mask |= (byte) (1 << face.get3DDataValue());
        } else {
            mask &= (byte) ~(1 << face.get3DDataValue());
        }
        setEnabledFacesMask(mask);
    }
    
    public java.util.UUID getOwner() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getResolvedOwnerName() {
        if (!ownerName.isBlank()) {
            return ownerName;
        }
        return resolveOwnerNameFromLevel();
    }

    public String getCachedOwnerName() {
        if (!ownerName.isBlank()) {
            return ownerName;
        }
        return resolveOwnerNameFromLevel();
    }

    private String resolveOwnerNameFromLevel() {
        if (owner != null && level != null) {
            Player player = level.getPlayerByUUID(owner);
            if (player != null) {
                return player.getName().getString();
            }
        }
        return "";
    }

    public void setOwner(java.util.UUID owner, @Nullable String ownerName) {
        this.owner = owner;
        this.ownerName = ownerName == null ? "" : ownerName;
        setChanged();
        syncToClient();
        syncRemoteTerminalIndex();
    }

    public void setCustomName(@Nullable Component customName) {
        this.customName = customName;
        setChanged();
        syncToClient();
        syncRemoteTerminalIndex();
    }

    @Nullable
    public Component getCustomName() {
        return customName;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        syncRemoteTerminalIndex();
    }

    private void syncRemoteTerminalIndex() {
        if (!(level instanceof ServerLevel serverLevel) || owner == null) {
            return;
        }
        RemoteTerminalBaseIndex.get(serverLevel.getServer()).upsert(
                serverLevel,
                worldPosition,
                owner,
                getTier(),
                getDisplayName().getString()
        );
    }

    public double getManualRangeLimit() {
        return manualRangeLimit;
    }

    public void setManualRangeLimit(double manualRangeLimit) {
        this.manualRangeLimit = manualRangeLimit <= 0.0D ? 0.0D : Math.min(256.0D, manualRangeLimit);
        setChanged();
        syncToClient();
    }
    
    /**
     * 请求客户端同步方块实体状态。syncToClient() 的别名。
     */
     */
    public void requestClientUpdate() {
        syncToClient();
    }
    
    // ========== Mounted Turret Types ==========
    
    public static void tick(Level level, BlockPos pos, BlockState state, TurretBaseBlockEntity blockEntity) {
        if (level.isClientSide) return;

        blockEntity.ensureEnergyCapacity();
        
        boolean changed = false;
        
        // === 能量管理 ===
        if (blockEntity.hasCreativePowerComponent()) {
            // 创造能量组件：始终保持能量满格。
            int maxEnergy = blockEntity.energyStorage.getMaxEnergyStored();
            if (blockEntity.energyStorage.getEnergyStored() < maxEnergy) {
                // 安装了创造能量组件时，直接将能量设为最大值。
                blockEntity.setEnergyFull();
                changed = true;
            }
        } else {
            // === 被动能量生成（太阳能） ===
            // 太阳能插件在白天且有天空视野时生成能量。
            // 检查是否为白天且方块是否能看见天空。
            if (blockEntity.hasSolarPlugin()) {
                boolean isDaytime = level.getDayTime() % 24000 < 12000; // 0-12000 tick = 白天
                // 太阳能插件检测：白天有天空视野时每 tick 生成能量。
                boolean canSeeSky = level.canSeeSky(pos.above());
                if (isDaytime && canSeeSky) {
                    int generated = com.tian_nu.AdvancedTurret.Config.solarEnergyGeneration;
                    int added = blockEntity.addEnergyDirectly(generated);
                    if (added > 0) {
                        changed = true;
                    }
                }
            }
            
            // === 红石转换插件 ===
            // 消耗弹药栏中的红石/红石块并将其转换为能量。
            // 优先级：红石块 (18000 RF) → 红石粉（比例可配置）。
            if (blockEntity.hasRedstoneConversionPlugin()) {
                int energyPerRedstone = com.tian_nu.AdvancedTurret.Config.redstoneToEnergyRatio;
                int energyPerRedstoneBlock = 18000; // 固定值：1 个红石块 = 18000 RF
                int maxEnergy = blockEntity.energyStorage.getMaxEnergyStored();
                int currentEnergy = blockEntity.energyStorage.getEnergyStored();
                int space = maxEnergy - currentEnergy;
                
                // 红石块：消耗弹药槽中的 1 个红石块以获取大量能量。
                if (space >= energyPerRedstoneBlock) {
                    for (int i = 0; i < blockEntity.ammoInventory.getSlots(); i++) {
                        net.minecraft.world.item.ItemStack stack = blockEntity.ammoInventory.getStackInSlot(i);
                        if (!stack.isEmpty() && stack.getItem() == net.minecraft.world.item.Items.REDSTONE_BLOCK) {
                            stack.shrink(1);
                            blockEntity.addEnergyDirectly(energyPerRedstoneBlock);
                            changed = true;
                            break;
                        }
                    }
                }
                
                // 红石粉：消耗弹药槽中的 1 个红石粉以获取少量能量。
                if (!changed && space >= energyPerRedstone) {
                    for (int i = 0; i < blockEntity.ammoInventory.getSlots(); i++) {
                        net.minecraft.world.item.ItemStack stack = blockEntity.ammoInventory.getStackInSlot(i);
                        if (!stack.isEmpty() && stack.getItem() == net.minecraft.world.item.Items.REDSTONE) {
                            stack.shrink(1);
                            blockEntity.addEnergyDirectly(energyPerRedstone);
                            changed = true;
                            break;
                        }
                    }
                }
            }
        }
        
        // 清除已超时（10 秒 / 200 tick）的伤害预扣记录。
        // 防止过期预扣阻止对同一实体的新攻击。
		blockEntity.clearExpiredReservations(level.getGameTime());

		if (changed) {
            blockEntity.setChanged();
            blockEntity.syncToClient();
        }
    }
    
    // ========== Mounted Turret Types ==========
    
    private boolean hasCreativePowerComponent() {
        int slotCount = Math.min(getPluginSlotCount(), basePluginSlot.getSlots());
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = basePluginSlot.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(ModItems.CREATIVE_POWER_COMPONENT.get())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查是否有插件槽包含太阳能插件（被动能量生成）。
     */
     */
    public boolean hasSolarPlugin() {
        int slotCount = Math.min(getPluginSlotCount(), basePluginSlot.getSlots());
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = basePluginSlot.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(ModItems.SOLAR_PLUGIN.get())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查是否有插件槽包含弹药回收插件（概率不消耗弹药）。
     */
     */
    public boolean hasAmmoRecyclingPlugin() {
        int slotCount = Math.min(getPluginSlotCount(), basePluginSlot.getSlots());
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = basePluginSlot.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(ModItems.AMMO_RECYCLING_PLUGIN.get())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查是否有插件槽包含红石转换插件（红石 → 能量）。
     */
    public boolean hasRedstoneConversionPlugin() {
        int slotCount = Math.min(getPluginSlotCount(), basePluginSlot.getSlots());
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = basePluginSlot.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(ModItems.REDSTONE_CONVERSION_PLUGIN.get())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查是否有插件槽包含破坏插件（启用方块破坏）。
     */
     */
public boolean hasDestructionPlugin() {
		int slotCount = Math.min(getPluginSlotCount(), basePluginSlot.getSlots());
		for (int i = 0; i < slotCount; i++) {
			ItemStack stack = basePluginSlot.getStackInSlot(i);
			if (!stack.isEmpty() && stack.is(ModItems.DESTRUCTION_PLUGIN.get())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 检查智能芯片是否启用了节约模式（避免对低血量目标过度击杀）。
	 */
	public boolean isThriftyMode() {
		ItemStack stack = getPluginStack();
		if (!stack.isEmpty()) {
			return com.tian_nu.AdvancedTurret.items.SmartChipItem.isThriftyMode(stack);
		}
		return false;
	}

	// ========== Mounted Turret Types ==========

	/**
	 * 为目标实体预扣伤害，防止多个炮塔过度击杀同一目标。
	 * @param entityId 目标实体的网络 ID
	 * @param damage 此炮塔计划造成的伤害量
	 * @param currentHealth 目标当前生命值
	 * @param gameTime 当前游戏 tick，用于超时追踪
	 * @return 本次预扣后的剩余生命值 (currentHealth - totalReserved)
	 */
	public float reserveDamage(int entityId, float damage, float currentHealth, long gameTime) {
		// 累积预扣伤害：将新的伤害累加到已有预扣记录中。
		// 这样不同面上的多个炮塔可以协调攻击。
		float existing = reservedDamage.getOrDefault(entityId, 0.0f);
		float totalReserved = existing + damage;
		
		reservedDamage.put(entityId, totalReserved);
		reservationTime.put(entityId, gameTime);

		// 计算扣除所有预扣伤害后的剩余生命值。
		float remainingHealth = currentHealth - totalReserved;
		return remainingHealth;
	}

	/**
	/**
	 * 尝试预扣伤害——如果目标未被已有的预扣记录判定为已死亡。
	 * @param entityId 目标实体的网络 ID
	 * @param damage 此炮塔计划造成的伤害量
	 * @param currentHealth 目标当前生命值
	 * @param gameTime 当前游戏 tick
	 * @return 预扣成功返回 true，若目标已被预扣标记为死亡则返回 false
	 */
	public boolean tryReserveDamage(int entityId, float damage, float currentHealth, long gameTime) {
		// 检查扣除已有预扣后目标是否还有剩余生命值。
		float existingReservation = reservedDamage.getOrDefault(entityId, 0.0f);
		float existingRemainingHealth = currentHealth - existingReservation;
		
		// 若已有预扣已覆盖目标全部生命值，跳过本炮塔。
		if (existingRemainingHealth <= 0) {
			return false;
		}
		
		// 预扣伤害：记录预扣量和时间戳。
		reservedDamage.put(entityId, damage);
		reservationTime.put(entityId, gameTime);
		return true;
	}

	/**
	/**
	 * 返回指定实体的已预扣伤害总量，若无预扣记录则返回 0。
	 * @param entityId 目标实体的网络 ID
	 * @return 已预扣伤害总量
	 */
	 */
	public float getReservedDamage(int entityId) {
		return reservedDamage.getOrDefault(entityId, 0.0f);
	}

	/**
	/**
	 * 取消对某实体的伤害预扣（例如：目标死亡或超出射程时）。
	 * @param entityId 目标实体的网络 ID
	 */
	public void cancelReservation(int entityId) {
		reservedDamage.remove(entityId);
		reservationTime.remove(entityId);
	}

	/**
	/**
	 * 确认已造成伤害，并从预扣记录中扣除对应量。
	 * 若确认伤害 >= 预扣伤害，则完全移除该预扣记录。
	 * @param entityId 目标实体的网络 ID
	 * @param damage 实际造成的伤害量
	 */
	 */
	public void confirmDamage(int entityId, float damage) {
		float reserved = reservedDamage.getOrDefault(entityId, 0.0f);
		if (reserved <= damage) {
		// 若确认的伤害已覆盖全部预扣量，清除该预扣记录。
			reservedDamage.remove(entityId);
			reservationTime.remove(entityId);
		} else {
		// 部分确认：从预扣量中扣除已造成的伤害。
			reservedDamage.put(entityId, reserved - damage);
		}
	}

	/**
	/**
	 * 移除已超过超时时间（200 tick / 10 秒）的伤害预扣记录。
	 * @param currentTime 当前游戏 tick
	 */
	 */
	private void clearExpiredReservations(long currentTime) {
		Iterator<Map.Entry<Integer, Long>> iterator = reservationTime.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, Long> entry = iterator.next();
			if (currentTime - entry.getValue() > RESERVATION_TIMEOUT) {
				int entityId = entry.getKey();
				iterator.remove();
				reservedDamage.remove(entityId);
			}
		}
	}

	/**
	/**
	 * 判断目标是否值得攻击（综合考虑已有伤害预扣）。
	 * 在节约模式下，跳过剩余生命值已被预扣完全覆盖的目标。
	 * @param entityId 目标实体的网络 ID
	 * @param currentHealth 目标当前生命值
	 * @return 炮塔是否应攻击此目标
	 */
	public boolean isTargetWorthAttacking(int entityId, float currentHealth) {
		if (!isThriftyMode()) {
			return true; // Reservation logic only applies while thrifty mode is active.
		}
		
		float reserved = getReservedDamage(entityId);
		float remainingHealth = currentHealth - reserved;
		
		// 若预扣伤害已覆盖剩余生命值，跳过该目标。
		return remainingHealth > 0;
	}

	private void checkCreativePowerComponent() {
        if (hasCreativePowerComponent() && level != null && !level.isClientSide) {
            setEnergyFull();
        }
    }
    
    /**
     * 将能量存储设为最大值。供创造能量组件使用。
     */
     */
    private void setEnergyFull() {
        if (energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored()) {
            energyStorage.setEnergyStored(energyStorage.getMaxEnergyStored());
            setChanged();
            syncToClient();
        }
    }
    
    /**
    /**
     * 直接向能量存储添加能量，不通过 capability 的 receiveEnergy 接口。
     * @param amount 要添加的能量值
     * @return 实际添加的能量值（受剩余空间限制）
     */
     */
    private int addEnergyDirectly(int amount) {
        int maxEnergy = energyStorage.getMaxEnergyStored();
        int currentEnergy = energyStorage.getEnergyStored();
        int space = maxEnergy - currentEnergy;
        int toAdd = Math.min(amount, space);
        if (toAdd > 0) {
            energyStorage.setEnergyStored(currentEnergy + toAdd);
            setChanged();
            syncToClient();
        }
        return toAdd;
    }
    
    // ========== Mounted Turret Types ==========
    
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCapability.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCapability.cast();
        }
        return super.getCapability(cap, side);
    }
    
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCapability.invalidate();
        itemCapability.invalidate();
    }

    private final class BaseEnergyStorage extends net.minecraftforge.energy.EnergyStorage {
        private BaseEnergyStorage(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }

        private void setEnergyStored(int energy) {
            this.energy = Math.max(0, Math.min(energy, getMaxEnergyStored()));
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
                syncToClient();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) {
                setChanged();
                syncToClient();
            }
            return extracted;
        }
    }
    
    // ========== Mounted Turret Types ==========
    
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        
        tag.put("Energy", energyStorage.serializeNBT());
        tag.put("AmmoInventory", ammoInventory.serializeNBT());
        tag.put("BasePluginSlot", basePluginSlot.serializeNBT());

        CompoundTag faces = new CompoundTag();
        for (Direction face : new Direction[] {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            faces.put(face.getName(), ((ItemStackHandler) getFaceUpgradeSlots(face)).serializeNBT());
        }
        tag.put("FaceUpgradeSlots", faces);
        
        // 将插件槽物品保存到 NBT。T5 基座的内置智能芯片也会在此保存。
        
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        String cachedOwnerName = getCachedOwnerName();
        if (!cachedOwnerName.isEmpty()) {
            tag.putString("OwnerName", cachedOwnerName);
        }
        if (customName != null) {
            tag.putString("CustomName", Component.Serializer.toJson(customName));
        }
        tag.putByte("EnabledFacesMask", enabledFacesMask);
        tag.putDouble("ManualRangeLimit", manualRangeLimit);
        if (hasBuiltInSmartChip() && !builtInSmartChip.isEmpty()) {
            tag.put("BuiltInSmartChip", builtInSmartChip.save(new CompoundTag()));
        }
    }
    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        ensureEnergyCapacity();
        
        if (tag.contains("Energy")) {
            energyStorage.deserializeNBT(tag.get("Energy"));
        }
        
        if (tag.contains("AmmoInventory")) {
            ammoInventory.deserializeNBT(tag.getCompound("AmmoInventory"));
        } else if (tag.contains("StorageInventory")) {
            ammoInventory.deserializeNBT(tag.getCompound("StorageInventory"));
        }

        if (tag.contains("BasePluginSlot")) {
            basePluginSlot.deserializeNBT(tag.getCompound("BasePluginSlot"));
        } else if (tag.contains("PluginSlot")) {
            ItemStackHandler legacy = new ItemStackHandler(3);
            legacy.deserializeNBT(tag.getCompound("PluginSlot"));
            if (basePluginSlot.getStackInSlot(0).isEmpty()) {
                for (int i = 0; i < legacy.getSlots(); i++) {
                    ItemStack s = legacy.getStackInSlot(i);
                    if (!s.isEmpty()) {
                        basePluginSlot.setStackInSlot(0, s);
                        break;
                    }
                }
            }
        }

        if (tag.contains("FaceUpgradeSlots")) {
            CompoundTag faces = tag.getCompound("FaceUpgradeSlots");
            for (Direction face : new Direction[] {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
                if (faces.contains(face.getName())) {
                    ((ItemStackHandler) getFaceUpgradeSlots(face)).deserializeNBT(faces.getCompound(face.getName()));
                }
            }
        } else if (tag.contains("UpgradeSlots")) {
            ItemStackHandler legacy = new ItemStackHandler(2);
            legacy.deserializeNBT(tag.getCompound("UpgradeSlots"));
            ItemStackHandler down = (ItemStackHandler) getFaceUpgradeSlots(Direction.DOWN);
            for (int i = 0; i < Math.min(legacy.getSlots(), down.getSlots()); i++) {
                if (down.getStackInSlot(i).isEmpty() && !legacy.getStackInSlot(i).isEmpty()) {
                    down.setStackInSlot(i, legacy.getStackInSlot(i));
                }
            }
        }
        if (tag.contains("Owner")) {
            owner = tag.getUUID("Owner");
        }
        if (tag.contains("OwnerName")) {
            ownerName = tag.getString("OwnerName");
        } else {
            ownerName = "";
        }
        if (tag.contains("CustomName", 8)) {
            customName = Component.Serializer.fromJson(tag.getString("CustomName"));
        } else {
            customName = null;
        }
        if (tag.contains("EnabledFacesMask")) {
            enabledFacesMask = tag.getByte("EnabledFacesMask");
        } else {
            enabledFacesMask = 0b111111;
        }
        if (tag.contains("ManualRangeLimit")) {
            manualRangeLimit = tag.getDouble("ManualRangeLimit");
        } else {
            manualRangeLimit = 0.0D;
        }
        if (tag.contains("BuiltInSmartChip")) {
            builtInSmartChip = ItemStack.of(tag.getCompound("BuiltInSmartChip"));
        } else if (hasBuiltInSmartChip()) {
            builtInSmartChip = new ItemStack(ModItems.SMART_CHIP.get());
        } else {
            builtInSmartChip = ItemStack.EMPTY;
        }
        currentTransferRate = getMaxTransferRateForTier();
    }

    private boolean isUpgradeComponent(ItemStack stack) {
        return stack.is(ModItems.ATTACK_BOOST_COMPONENT.get())
                || stack.is(ModItems.ENERGY_EFFICIENCY_COMPONENT.get())
                || stack.is(ModItems.RANGE_COMPONENT.get())
                || stack.is(ModItems.ACCURACY_COMPONENT.get())
                || stack.is(ModItems.FIRE_RATE_COMPONENT.get());
    }

    private boolean isPluginItem(ItemStack stack) {
        return stack.is(ModItems.CREATIVE_POWER_COMPONENT.get())
                || stack.is(ModItems.SMART_CHIP.get())
                || stack.is(ModItems.SOLAR_PLUGIN.get())
                || stack.is(ModItems.AMMO_RECYCLING_PLUGIN.get())
                || stack.is(ModItems.REDSTONE_CONVERSION_PLUGIN.get())
                || stack.is(ModItems.DESTRUCTION_PLUGIN.get());
    }

    private boolean isValidAmmoForMountedTurrets(ItemStack stack) {
        if (stack.isEmpty()) return false;

        Level level = getLevel();
        if (level == null) {
            return isGenericAmmo(stack);
        }

        boolean hasJunkTurret = false;
        boolean hasSpecificAmmoMatch = false;
        boolean hasMountedTurret = false;

        for (Direction face : Direction.values()) {
            BlockEntity be = level.getBlockEntity(getBlockPos().relative(face));

            if (be instanceof MachineGunTurretBlockEntity turret && turret.getBaseEntity() == this) {
                hasMountedTurret = true;
                hasSpecificAmmoMatch |= stack.is(ModItems.MACHINE_GUN_BULLET.get());
            } else if (be instanceof RailgunTurretBlockEntity turret && turret.getBaseEntity() == this) {
                hasMountedTurret = true;
                hasSpecificAmmoMatch |= stack.is(ModItems.RAILGUN_BULLET.get());
            } else if (be instanceof RocketTurretBlockEntity turret && turret.getBaseEntity() == this) {
                hasMountedTurret = true;
                hasSpecificAmmoMatch |= stack.is(ModItems.ROCKET.get());
            } else if (be instanceof MissileTurretBlockEntity turret && turret.getBaseEntity() == this) {
                hasMountedTurret = true;
                hasSpecificAmmoMatch |= stack.is(ModItems.MISSILE.get());
            } else if (be instanceof GrenadeLauncherTurretBlockEntity turret && turret.getBaseEntity() == this) {
                hasMountedTurret = true;
                hasSpecificAmmoMatch |= stack.is(ModItems.GRENADE.get());
            } else if (be instanceof JunkTurretBlockEntity turret && turret.getBaseEntity() == this) {
                hasMountedTurret = true;
                hasJunkTurret = true;
            } else if (be instanceof LaserTurretBlockEntity turret && turret.getBaseEntity() == this) {
                hasMountedTurret = true;
            } else if (be instanceof PhaseFieldTurretBlockEntity turret && turret.getBaseEntity() == this) {
                hasMountedTurret = true;
            } else if (be instanceof ResonanceFieldTurretBlockEntity turret && turret.getBaseEntity() == this) {
                hasMountedTurret = true;
            }
        }

        if (hasJunkTurret) return true;
        if (hasMountedTurret) return hasSpecificAmmoMatch;

        return isGenericAmmo(stack);
    }

    private boolean isGenericAmmo(ItemStack stack) {
        return stack.is(ModItems.MACHINE_GUN_BULLET.get())
                || stack.is(ModItems.RAILGUN_BULLET.get())
                || stack.is(ModItems.ROCKET.get())
                || stack.is(ModItems.MISSILE.get())
                || stack.is(ModItems.GRENADE.get());
    }

    public float getDamageForFace(Direction face, float baseDamage) {
        int count = countUpgradeItems(face, ModItems.ATTACK_BOOST_COMPONENT.get());
        double multiplier = Math.min(3.0, 1.0 + (count * 0.05));
        return (float) (baseDamage * multiplier);
    }

    public double getSearchRadiusForFace(Direction face, double baseRadius) {
        int count = countUpgradeItems(face, ModItems.RANGE_COMPONENT.get());
        // 射程升级每安装一个组件增加 1 格基础半径。
        double upgradedRadius = baseRadius + count;
        if (manualRangeLimit > 0.0D) {
            return Math.max(1.0D, Math.min(upgradedRadius, manualRangeLimit));
        }
        return upgradedRadius;
    }

    public int getFireRateForFace(Direction face, int baseFireRate) {
        int count = countUpgradeItems(face, ModItems.FIRE_RATE_COMPONENT.get());
        double factor = Math.max(0.20, 1.0 - (count * 0.05));
        return Math.max(1, (int) Math.round(baseFireRate * factor));
    }

    public int getEnergyCostForFace(Direction face, int baseEnergyCost) {
        int count = countUpgradeItems(face, ModItems.ENERGY_EFFICIENCY_COMPONENT.get());
        double factor = Math.max(0.20, 1.0 - (count * 0.05));
        return Math.max(1, (int) Math.ceil(baseEnergyCost * factor));
    }

    public int getUpgradeItemCountForFace(Direction face, net.minecraft.world.item.Item item) {
        return countUpgradeItems(face, item);
    }

    private int countUpgradeItems(Direction face, net.minecraft.world.item.Item item) {
        ItemStackHandler handler = (ItemStackHandler) getFaceUpgradeSlots(face);
        int total = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }
    
    // ========== Mounted Turret Types ==========
    
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }
    
    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }
    
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }
    
    /**
     * 向追踪此方块的所有客户端发送方块更新，同步全部 NBT 数据。
     */
    public void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void dropStoredItems(Level level, BlockPos pos) {
        dropHandlerItems(level, pos, ammoInventory);
        dropHandlerItems(level, pos, basePluginSlot);
        for (ItemStackHandler faceUpgradeSlot : faceUpgradeSlots) {
            dropHandlerItems(level, pos, faceUpgradeSlot);
        }
    }

    private void dropHandlerItems(Level level, BlockPos pos, ItemStackHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
            handler.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }
    
    // ========== Mounted Turret Types ==========
    
    @Override
    public Component getDisplayName() {
        if (customName != null) {
            return customName;
        }
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new TurretMenu(containerId, playerInventory, this, this.data);
    }
}