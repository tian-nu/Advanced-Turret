package com.tian_nu.AdvancedTurret.blocks.entitys;

import com.tian_nu.AdvancedTurret.items.ModItems;
import com.tian_nu.AdvancedTurret.gui.TurretMenu;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 炮塔基座方块实体
 * 
 * <p>管理炮塔的能量存储、物品库存和菜单交互</p>
 * 
 * @author tian_nu
 */
public class TurretBaseBlockEntity extends BlockEntity implements MenuProvider {
    
    // ========== 常量定义 ==========
    
    /** 各等级炮塔能量容量 (T1, T2, T3) */
    public static final int[] MAX_ENERGIES = {10000, 40000, 100000};
    /** 最大能量传输速率 */
    public static final int MAX_TRANSFER_RATE = 1000;
    
    /** 存储槽位数量 */
    public static final int STORAGE_SLOTS = 9;
    /** 升级槽位数量 */
    public static final int UPGRADE_SLOTS = 2;
    /** 插件槽位数量 */
    public static final int PLUGIN_SLOTS = 1;
    
    // ========== 炮塔类型枚举 ==========
    
    public enum TurretType {
        NONE,
        MACHINE_GUN,
        RAILGUN
    }
    
    // ========== 字段 ==========
    
    private TurretType installedTurret = TurretType.NONE;
    private int turretLevel = 1;
    
    // ========== 能量存储 ==========
    
    private final EnergyStorage energyStorage = new EnergyStorage(MAX_ENERGIES[0], MAX_TRANSFER_RATE, MAX_TRANSFER_RATE) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }
        
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) {
                setChanged();
            }
            return extracted;
        }
        
        @Override
        public boolean canExtract() {
            return true;
        }
        
        @Override
        public boolean canReceive() {
            return getEnergyStored() < getMaxEnergyStored();
        }
    };
    
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);
    
    // ========== 物品存储 ==========
    
    private final ItemStackHandler storageInventory = new ItemStackHandler(STORAGE_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    
    private final ItemStackHandler upgradeSlots = new ItemStackHandler(UPGRADE_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    
    private final ItemStackHandler pluginSlot = new ItemStackHandler(PLUGIN_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // 插件改变时检查创造能量组件
            checkCreativePowerComponent();
        }
    };
    
    private final IItemHandler combinedInventory = new IItemHandler() {
        @Override
        public int getSlots() {
            return STORAGE_SLOTS + UPGRADE_SLOTS + PLUGIN_SLOTS;
        }
        
        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if (slot < STORAGE_SLOTS) return storageInventory.getStackInSlot(slot);
            if (slot < STORAGE_SLOTS + UPGRADE_SLOTS) return upgradeSlots.getStackInSlot(slot - STORAGE_SLOTS);
            return pluginSlot.getStackInSlot(slot - STORAGE_SLOTS - UPGRADE_SLOTS);
        }
        
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot < STORAGE_SLOTS) return storageInventory.insertItem(slot, stack, simulate);
            if (slot < STORAGE_SLOTS + UPGRADE_SLOTS) return upgradeSlots.insertItem(slot - STORAGE_SLOTS, stack, simulate);
            return pluginSlot.insertItem(slot - STORAGE_SLOTS - UPGRADE_SLOTS, stack, simulate);
        }
        
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < STORAGE_SLOTS) return storageInventory.extractItem(slot, amount, simulate);
            if (slot < STORAGE_SLOTS + UPGRADE_SLOTS) return upgradeSlots.extractItem(slot - STORAGE_SLOTS, amount, simulate);
            return pluginSlot.extractItem(slot - STORAGE_SLOTS - UPGRADE_SLOTS, amount, simulate);
        }
        
        @Override
        public int getSlotLimit(int slot) {
            if (slot < STORAGE_SLOTS) return storageInventory.getSlotLimit(slot);
            if (slot < STORAGE_SLOTS + UPGRADE_SLOTS) return upgradeSlots.getSlotLimit(slot - STORAGE_SLOTS);
            return pluginSlot.getSlotLimit(slot - STORAGE_SLOTS - UPGRADE_SLOTS);
        }
        
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot < STORAGE_SLOTS) return storageInventory.isItemValid(slot, stack);
            if (slot < STORAGE_SLOTS + UPGRADE_SLOTS) return upgradeSlots.isItemValid(slot - STORAGE_SLOTS, stack);
            return pluginSlot.isItemValid(slot - STORAGE_SLOTS - UPGRADE_SLOTS, stack);
        }
    };
    
    private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> combinedInventory);
    
    // ========== 构造函数 ==========
    
    public TurretBaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_BASE.get(), pos, state);
    }
    
    // ========== 公共方法 ==========
    
    public IItemHandler getStorageInventory() {
        return storageInventory;
    }
    
    public IItemHandler getUpgradeSlots() {
        return upgradeSlots;
    }
    
    public IItemHandler getPluginSlot() {
        return pluginSlot;
    }
    
    public TurretType getInstalledTurret() {
        return installedTurret;
    }
    
    public void setInstalledTurret(TurretType type) {
        this.installedTurret = type;
        setChanged();
    }
    
    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }
    
    /**
     * 获取当前存储的能量
     */
    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }
    
    /**
     * 获取最大能量存储
     */
    public int getMaxEnergyStored() {
        return energyStorage.getMaxEnergyStored();
    }
    
    public int getTurretLevel() {
        return turretLevel;
    }
    
    public void setTurretLevel(int level) {
        this.turretLevel = Math.max(1, Math.min(3, level));
        setChanged();
    }
    
    /**
     * 请求客户端更新
     */
    public void requestClientUpdate() {
        syncToClient();
    }
    
    // ========== 游戏逻辑 ==========
    
    public static void tick(Level level, BlockPos pos, BlockState state, TurretBaseBlockEntity blockEntity) {
        if (level.isClientSide) return;
        
        boolean changed = false;
        
        // 检查创造能量组件
        if (blockEntity.hasCreativePowerComponent()) {
            int maxEnergy = blockEntity.energyStorage.getMaxEnergyStored();
            if (blockEntity.energyStorage.getEnergyStored() < maxEnergy) {
                // 直接填满能量
                blockEntity.energyStorage.receiveEnergy(maxEnergy, false);
                changed = true;
            }
        }
        
        // 炮塔逻辑现在由独立的炮塔方块实体处理
        // 基座只负责提供能量
        
        if (changed) {
            blockEntity.setChanged();
            blockEntity.syncToClient();
        }
    }
    
    private boolean hasCreativePowerComponent() {
        ItemStack plugin = pluginSlot.getStackInSlot(0);
        return !plugin.isEmpty() && plugin.is(ModItems.CREATIVE_POWER_COMPONENT.get());
    }
    
    private void checkCreativePowerComponent() {
        if (hasCreativePowerComponent() && level != null && !level.isClientSide) {
            int maxEnergy = energyStorage.getMaxEnergyStored();
            if (energyStorage.getEnergyStored() < maxEnergy) {
                energyStorage.receiveEnergy(maxEnergy, false);
                setChanged();
                syncToClient();
            }
        }
    }
    
    // ========== 能力系统 ==========
    
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
    
    // ========== 数据持久化 ==========
    
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        
        tag.putInt("TurretLevel", turretLevel);
        tag.putString("InstalledTurret", installedTurret.name());
        tag.put("Energy", energyStorage.serializeNBT());
        tag.put("StorageInventory", storageInventory.serializeNBT());
        tag.put("UpgradeSlots", upgradeSlots.serializeNBT());
        tag.put("PluginSlot", pluginSlot.serializeNBT());
    }
    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        
        if (tag.contains("TurretLevel")) {
            turretLevel = tag.getInt("TurretLevel");
        }
        
        if (tag.contains("InstalledTurret")) {
            try {
                installedTurret = TurretType.valueOf(tag.getString("InstalledTurret"));
            } catch (IllegalArgumentException e) {
                installedTurret = TurretType.NONE;
            }
        }
        
        if (tag.contains("Energy")) {
            energyStorage.deserializeNBT(tag.get("Energy"));
        }
        
        if (tag.contains("StorageInventory")) {
            storageInventory.deserializeNBT(tag.getCompound("StorageInventory"));
        }
        if (tag.contains("UpgradeSlots")) {
            upgradeSlots.deserializeNBT(tag.getCompound("UpgradeSlots"));
        }
        if (tag.contains("PluginSlot")) {
            pluginSlot.deserializeNBT(tag.getCompound("PluginSlot"));
        }
    }
    
    // ========== 网络同步 ==========
    
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
     * 同步数据到客户端
     */
    public void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    // ========== MenuProvider ==========
    
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.turret_base");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new TurretMenu(containerId, playerInventory, this);
    }
}
