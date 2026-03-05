package com.tian_nu.AdvancedTurret.blocks.entitys;

import com.tian_nu.AdvancedTurret.blocks.ModBlocks;
import com.tian_nu.AdvancedTurret.blocks.entitys.MachineGunTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.MissileTurretBlockEntity;
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
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.inventory.ContainerData;

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
                case 1 -> TurretBaseBlockEntity.this.getMaxEnergyStored();
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
    
    /** 各等级炮塔能量容量 (T1..T5) */
    public static final int[] MAX_ENERGIES = {10000, 40000, 100000, 250000, 500000};
    /** 最大能量传输速率 */
    public static final int MAX_TRANSFER_RATE = 1000;
    
    /** 弹药槽位数量（全等级一致） */
    public static final int AMMO_SLOTS = 9;
    /** 最大功能插件槽位数量（T4/T5支持双槽） */
    public static final int MAX_PLUGIN_SLOTS = 2;
    /** 每个面的最大升级槽位数量（为未来 T5 预留） */
    public static final int MAX_UPGRADE_SLOTS_PER_FACE = 3;
    
    // ========== 炮塔类型枚举 ==========
    
    public enum TurretType {
        NONE,
        MACHINE_GUN,
        RAILGUN
    }
    
    // ========== 字段 ==========
    
    // ========== 插件配置 ==========
    
    // 使用位掩码存储启用的面 (0-63)，默认全开
    // 这个保留在基座上，因为是基座的硬件开关
    // 如果用户希望这也随插件移动，则应移至插件NBT
    // 根据需求6：配置需要保存在插件里。所以这里也应该改为从插件读取。
    // 但是，如果没有插件，默认行为是什么？
    // 假设没有插件时，默认全开。
    
    private java.util.UUID owner;
    
    // ========== 能量存储 ==========
    
    private BaseEnergyStorage energyStorage = createEnergyStorage(getMaxEnergyForTier());
    
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);
    
    // ========== 物品存储 ==========
    
    private final ItemStackHandler ammoInventory = new ItemStackHandler(AMMO_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            syncToClient();
        }
    };
    
    // 使用最大槽位数量，实际可用槽位由getPluginSlotCount()决定
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
            // 只允许在等级允许的槽位范围内放入物品
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
    
    // ========== 构造函数 ==========
    
    public TurretBaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_BASE.get(), pos, state);
    }
    
    // ========== 公共方法 ==========
    
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

    private BaseEnergyStorage createEnergyStorage(int capacity) {
        return new BaseEnergyStorage(capacity, MAX_TRANSFER_RATE, MAX_TRANSFER_RATE);
    }

    private void ensureEnergyCapacity() {
        int desired = getMaxEnergyForTier();
        if (energyStorage.getMaxEnergyStored() == desired) return;
        int stored = energyStorage.getEnergyStored();
        BaseEnergyStorage next = createEnergyStorage(desired);
        next.setEnergyStored(Math.min(stored, desired));
        energyStorage = next;
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
     * 获取插件槽位数量
     * T1: 无插件槽
     * T2-T3: 1个插件槽
     * T4-T5: 2个插件槽
     */
    public int getPluginSlotCount() {
        return switch (getTier()) {
            case 1 -> 0;
            case 2, 3 -> 1;
            case 4, 5 -> 2;  // T4/T5双槽
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
     * 获取当前存储的能量
     */
    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }
    
    /**
     * 获取最大能量存储
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
        // 遍历所有可用的插件槽，返回第一个智能芯片
        // 使用实际槽数量兼容旧存档
        int slotCount = Math.min(getPluginSlotCount(), basePluginSlot.getSlots());
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = basePluginSlot.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof com.tian_nu.AdvancedTurret.items.SmartChipItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * 获取所有插件（用于多插件叠加功能）
     */
    public java.util.List<ItemStack> getAllPluginStacks() {
        java.util.List<ItemStack> plugins = new java.util.ArrayList<>();
        // 使用实际槽数量兼容旧存档
        int slotCount = Math.min(getPluginSlotCount(), basePluginSlot.getSlots());
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = basePluginSlot.getStackInSlot(i);
            if (!stack.isEmpty()) {
                plugins.add(stack);
            }
        }
        return plugins;
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
        return false;
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

        blockEntity.ensureEnergyCapacity();
        
        boolean changed = false;
        
        // 检查创造能量组件
        if (blockEntity.hasCreativePowerComponent()) {
            int maxEnergy = blockEntity.energyStorage.getMaxEnergyStored();
            if (blockEntity.energyStorage.getEnergyStored() < maxEnergy) {
                // 直接设置满电（绕过receiveEnergy的maxReceive限制）
                blockEntity.setEnergyFull();
                changed = true;
            }
        } else {
            // 太阳能插件发电
            // 条件：白天 且 炮塔位置上方可以看到天空
            if (blockEntity.hasSolarPlugin()) {
                boolean isDaytime = level.getDayTime() % 24000 < 12000; // 0-12000是白天
                // 检查基座上方一格是否能看到天空（因为基座上方可能有炮塔）
                boolean canSeeSky = level.canSeeSky(pos.above());
                if (isDaytime && canSeeSky) {
                    int generated = com.tian_nu.AdvancedTurret.Config.solarEnergyGeneration;
                    int added = blockEntity.addEnergyDirectly(generated);
                    if (added > 0) {
                        changed = true;
                    }
                }
            }
            
            // 红石转化插件：从弹药槽消耗红石/红石块转换为能量
            if (blockEntity.hasRedstoneConversionPlugin()) {
                int energyPerRedstone = com.tian_nu.AdvancedTurret.Config.redstoneToEnergyRatio;
                int energyPerRedstoneBlock = 18000; // 红石块转化能量
                int maxEnergy = blockEntity.energyStorage.getMaxEnergyStored();
                int currentEnergy = blockEntity.energyStorage.getEnergyStored();
                int space = maxEnergy - currentEnergy;
                
                // 优先消耗红石块（能量更多）
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
                
                // 再消耗红石粉
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
        
        // 炮塔逻辑现在由独立的炮塔方块实体处理
        // 基座只负责提供能量
        
        if (changed) {
            blockEntity.setChanged();
            blockEntity.syncToClient();
        }
    }
    
    // ========== 插件检查方法 ==========
    
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
     * 检查是否有太阳能插件
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
     * 检查是否有弹药回收插件
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
     * 检查是否有红石转化插件
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
     * 检查是否有破坏插件
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
    
    private void checkCreativePowerComponent() {
        if (hasCreativePowerComponent() && level != null && !level.isClientSide) {
            setEnergyFull();
        }
    }
    
    /**
     * 直接设置满电（用于创造能量组件）
     */
    private void setEnergyFull() {
        if (energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored()) {
            energyStorage.setEnergyStored(energyStorage.getMaxEnergyStored());
            setChanged();
            syncToClient();
        }
    }
    
    /**
     * 直接增加能量（绕过maxReceive限制）
     * @param amount 增加的能量值
     * @return 实际增加的能量
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
    
    // ========== 数据持久化 ==========
    
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
        
        // 插件配置现在存储在 PluginSlot 的物品 NBT 中，不需要单独保存
        
        if (owner != null) {
            tag.putUUID("Owner", owner);
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
        
        // 插件配置现在存储在 PluginSlot 的物品 NBT 中，不需要单独读取

        if (tag.contains("Owner")) {
            owner = tag.getUUID("Owner");
        }
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

    public float getDamageForFace(Direction face, float baseDamage) {
        int count = countUpgradeItems(face, ModItems.ATTACK_BOOST_COMPONENT.get());
        double multiplier = Math.min(3.0, 1.0 + (count * 0.10));
        return (float) (baseDamage * multiplier);
    }

    public double getSearchRadiusForFace(Direction face, double baseRadius) {
        int count = countUpgradeItems(face, ModItems.RANGE_COMPONENT.get());
        // 每个范围组件增加8格，上限为基础范围+32格
        return Math.min(baseRadius + 32.0, baseRadius + count * 8);
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
