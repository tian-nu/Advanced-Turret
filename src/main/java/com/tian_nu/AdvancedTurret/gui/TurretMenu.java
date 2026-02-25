package com.tian_nu.AdvancedTurret.gui;

import com.tian_nu.AdvancedTurret.blocks.ModBlocks;
import com.tian_nu.AdvancedTurret.blocks.entitys.TurretBaseBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * 炮塔基座菜单
 * 
 * <p>管理存储槽位、升级槽位、插件槽位和玩家物品栏</p>
 * 
 * @author tian_nu
 */
public class TurretMenu extends AbstractContainerMenu {
    
    private final TurretBaseBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final ContainerData data;
    
    // 槽位索引常量
    private static final int STORAGE_START = 0;
    private static final int STORAGE_END = STORAGE_START + TurretBaseBlockEntity.STORAGE_SLOTS;
    private static final int UPGRADE_START = STORAGE_END;
    private static final int UPGRADE_END = UPGRADE_START + TurretBaseBlockEntity.UPGRADE_SLOTS;
    private static final int PLUGIN_START = UPGRADE_END;
    private static final int PLUGIN_END = PLUGIN_START + TurretBaseBlockEntity.PLUGIN_SLOTS;
    private static final int PLAYER_INV_START = PLUGIN_END;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int PLAYER_HOTBAR_END = PLAYER_INV_END + 9;
    
    // 客户端构造函数
    public TurretMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData), new SimpleContainerData(2));
    }
    
    private static BlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf buf) {
        if (buf == null) {
            throw new IllegalArgumentException("Extra data cannot be null");
        }
        return playerInventory.player.level().getBlockEntity(buf.readBlockPos());
    }
    
    // 服务端构造函数
    public TurretMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.TURRET_BASE.get(), containerId);
        
        if (!(blockEntity instanceof TurretBaseBlockEntity turretBase)) {
            throw new IllegalStateException("Invalid block entity type: " + 
                    (blockEntity != null ? blockEntity.getClass().getSimpleName() : "null"));
        }
        
        this.blockEntity = turretBase;
        this.levelAccess = ContainerLevelAccess.create(turretBase.getLevel(), turretBase.getBlockPos());
        this.data = data;
        
        addStorageSlots();
        addUpgradeSlots();
        addPluginSlots();
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        
        addDataSlots(data);
    }
    
    // UI 重构：调整槽位坐标
    // 背景图尺寸 194x166
    // 存储槽位 (3x3) 放在左侧
    private void addStorageSlots() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                int x = 8 + col * 18;
                int y = 18 + row * 18;
                addSlot(new SlotItemHandler(blockEntity.getStorageInventory(), index, x, y));
            }
        }
    }
    
    // 升级槽位 (2个) 放在中间
    private void addUpgradeSlots() {
        // x = 8 + 3*18 + 10 = 72
        addSlot(new SlotItemHandler(blockEntity.getUpgradeSlots(), 0, 80, 18));
        addSlot(new SlotItemHandler(blockEntity.getUpgradeSlots(), 1, 80, 36));
    }
    
    // 插件槽位 (3个) 放在中间下方
    private void addPluginSlots() {
        for (int i = 0; i < 3; i++) {
            addSlot(new SlotItemHandler(blockEntity.getPluginSlot(), i, 80 + i * 18, 60));
        }
    }
    
    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = 8 + col * 18;
                int y = 84 + row * 18;
                int index = col + row * 9 + 9;
                addSlot(new Slot(playerInventory, index, x, y));
            }
        }
    }
    
    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; col++) {
            int x = 8 + col * 18;
            int y = 142;
            addSlot(new Slot(playerInventory, col, x, y));
        }
    }
    
    public TurretBaseBlockEntity getBlockEntity() {
        return this.blockEntity;
    }
    
    public int getEnergy() {
        return this.data.get(0);
    }
    
    public int getMaxEnergy() {
        return this.data.get(1);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            
            // 槽位范围：
            // Storage: 0-8
            // Upgrade: 9-10
            // Plugin: 11-13
            // Player Inv: 14-40
            // Hotbar: 41-49
            
            if (index < PLAYER_INV_START) {
                // 从容器移动到玩家背包
                if (!this.moveItemStackTo(itemstack1, PLAYER_INV_START, PLAYER_HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家背包移动到容器
                // 优先尝试插件
                if (itemstack1.getItem() instanceof com.tian_nu.AdvancedTurret.items.SmartChipItem) {
                    if (!this.moveItemStackTo(itemstack1, PLUGIN_START, PLUGIN_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } 
                // 其次尝试升级组件 (暂无具体类，假设所有其他都去存储)
                else if (!this.moveItemStackTo(itemstack1, STORAGE_START, STORAGE_END, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.levelAccess, player, ModBlocks.TURRET_BASE_T1.get()) ||
               stillValid(this.levelAccess, player, ModBlocks.TURRET_BASE_T2.get()) ||
               stillValid(this.levelAccess, player, ModBlocks.TURRET_BASE_T3.get());
    }
}
