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
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * 炮塔基座方块实体
 * 
 * <p>管理炮塔的能量存储、物品库存和菜单交互</p>
 * 
 * @author tian_nu
 */
public class TurretBaseBlockEntity extends BlockEntity implements MenuProvider {
    
    // ========== ContainerData 实现 ==========
    
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> TurretBaseBlockEntity.this.energyStorage.getEnergyStored();
                case 1 -> TurretBaseBlockEntity.this.energyStorage.getMaxEnergyStored();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // 客户端通常不需要设置能量，但如果是从网络包同步可能需要
            // 这里主要用于Menu同步
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

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
    public static final int PLUGIN_SLOTS = 3;
    
    // ========== 炮塔类型枚举 ==========
    
    public enum TurretType {
        NONE,
        MACHINE_GUN,
        RAILGUN
    }
    
    // ========== 字段 ==========
    
    private TurretType installedTurret = TurretType.NONE;
    private int turretLevel = 1;
    
    // ========== 插件配置 ==========
    
    // 使用位掩码存储启用的面 (0-63)，默认全开
    // 这个保留在基座上，因为是基座的硬件开关
    // 如果用户希望这也随插件移动，则应移至插件NBT
    // 根据需求6：配置需要保存在插件里。所以这里也应该改为从插件读取。
    // 但是，如果没有插件，默认行为是什么？
    // 假设没有插件时，默认全开。
    
    private java.util.UUID owner;
    
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

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof com.tian_nu.AdvancedTurret.items.SmartChipItem || 
                   stack.is(ModItems.CREATIVE_POWER_COMPONENT.get());
        }
        
        @Override
        public void deserializeNBT(CompoundTag nbt) {
            super.deserializeNBT(nbt);
            // 如果加载后槽位数量小于当前设定的数量 (例如旧存档只有1个槽)，强制扩容并保留原有物品
            if (this.getSlots() < PLUGIN_SLOTS) {
                NonNullList<ItemStack> oldStacks = NonNullList.create();
                for (int i = 0; i < this.getSlots(); i++) {
                    oldStacks.add(this.getStackInSlot(i));
                }
                
                this.setSize(PLUGIN_SLOTS);
                
                for (int i = 0; i < oldStacks.size(); i++) {
                    this.setStackInSlot(i, oldStacks.get(i));
                }
            }
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
    
    public void consumeEnergy(int amount) {
        int extracted = energyStorage.extractEnergy(amount, false);
        if (extracted > 0) {
            setChanged();
            syncToClient();
        }
    }
    
    public int getTurretLevel() {
        return turretLevel;
    }
    
    public void setTurretLevel(int level) {
        this.turretLevel = Math.max(1, Math.min(3, level));
        setChanged();
    }

    public ItemStack getPluginStack() {
        // 返回第一个有效的智能芯片
        for (int i = 0; i < pluginSlot.getSlots(); i++) {
            ItemStack stack = pluginSlot.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof com.tian_nu.AdvancedTurret.items.SmartChipItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    // 获取所有有效的插件（如果需要多插件叠加逻辑）
    // 目前逻辑是：只要有一个插件开启了某功能，该功能就生效？
    // 或者只读取第一个？
    // 根据用户反馈 "3个插件槽实际上只有最后一个可以生效"，说明用户期望所有插件都生效，或者至少能用。
    // 这里的逻辑：getPluginStack 返回第一个找到的。
    // 如果用户想要多插件叠加（例如一个负责友伤，一个负责黑名单），我们需要合并逻辑。
    // 但目前 NBT 都在一个芯片上。
    // 简单起见，我们优先使用第一个找到的芯片。
    // 如果用户想让3个槽都生效，可能意图是放不同的芯片？
    // 假设目前只有 SmartChipItem。
    
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
        ItemStack stack = getPluginStack();
        if (!stack.isEmpty()) {
            return com.tian_nu.AdvancedTurret.items.SmartChipItem.getEnabledFaces(stack);
        }
        return 0b111111; // 默认全开
    }
    
    public boolean isFaceEnabled(Direction face) {
        return (getEnabledFacesMask() & (1 << face.get3DDataValue())) != 0;
    }
    
    // Setters now update the ItemStack (Update the FIRST found chip)
    public void setFriendlyFire(boolean friendlyFire) {
        ItemStack stack = getPluginStack();
        if (!stack.isEmpty()) {
            com.tian_nu.AdvancedTurret.items.SmartChipItem.setFriendlyFire(stack, friendlyFire);
            setChanged();
        }
    }

    public void setPredictiveAiming(boolean predictiveAiming) {
        ItemStack stack = getPluginStack();
        if (!stack.isEmpty()) {
            com.tian_nu.AdvancedTurret.items.SmartChipItem.setPredictiveAiming(stack, predictiveAiming);
            setChanged();
        }
    }

    public void setEnabledFacesMask(byte enabledFacesMask) {
        ItemStack stack = getPluginStack();
        if (!stack.isEmpty()) {
            com.tian_nu.AdvancedTurret.items.SmartChipItem.setEnabledFaces(stack, enabledFacesMask);
            setChanged();
        }
    }
    
    public void setFaceEnabled(Direction face, boolean enabled) {
        ItemStack stack = getPluginStack();
        if (!stack.isEmpty()) {
            byte mask = com.tian_nu.AdvancedTurret.items.SmartChipItem.getEnabledFaces(stack);
            if (enabled) {
                mask |= (1 << face.get3DDataValue());
            } else {
                mask &= ~(1 << face.get3DDataValue());
            }
            com.tian_nu.AdvancedTurret.items.SmartChipItem.setEnabledFaces(stack, mask);
            setChanged();
        }
    }
    
    public void setOwner(java.util.UUID owner) {
        this.owner = owner;
        setChanged();
    }
    
    public java.util.UUID getOwner() {
        return owner;
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
        for (int i = 0; i < PLUGIN_SLOTS; i++) {
            ItemStack plugin = pluginSlot.getStackInSlot(i);
            if (!plugin.isEmpty() && plugin.is(ModItems.CREATIVE_POWER_COMPONENT.get())) {
                return true;
            }
        }
        return false;
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
        
        // 插件配置现在存储在 PluginSlot 的物品 NBT 中，不需要单独保存
        
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
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
        
        // 插件配置现在存储在 PluginSlot 的物品 NBT 中，不需要单独读取

        if (tag.contains("Owner")) {
            owner = tag.getUUID("Owner");
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
        return new TurretMenu(containerId, playerInventory, this, this.data);
    }
}
