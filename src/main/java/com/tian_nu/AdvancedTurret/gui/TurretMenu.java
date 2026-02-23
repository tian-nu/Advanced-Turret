package com.tian_nu.AdvancedTurret.gui;

import com.tian_nu.AdvancedTurret.blocks.ModBlocks;
import com.tian_nu.AdvancedTurret.blocks.entitys.TurretBaseBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
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
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData));
    }
    
    private static BlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf buf) {
        if (buf == null) {
            throw new IllegalArgumentException("Extra data cannot be null");
        }
        return playerInventory.player.level().getBlockEntity(buf.readBlockPos());
    }
    
    // 服务端构造函数
    public TurretMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        super(ModMenuTypes.TURRET_BASE.get(), containerId);
        
        if (!(blockEntity instanceof TurretBaseBlockEntity turretBase)) {
            throw new IllegalStateException("Invalid block entity type: " + 
                    (blockEntity != null ? blockEntity.getClass().getSimpleName() : "null"));
        }
        
        this.blockEntity = turretBase;
        this.levelAccess = ContainerLevelAccess.create(turretBase.getLevel(), turretBase.getBlockPos());
        
        addStorageSlots();
        addUpgradeSlots();
        addPluginSlot();
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }
    
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
    
    private void addUpgradeSlots() {
        addSlot(new SlotItemHandler(blockEntity.getUpgradeSlots(), 0, 176, 18));
        addSlot(new SlotItemHandler(blockEntity.getUpgradeSlots(), 1, 176, 36));
    }
    
    private void addPluginSlot() {
        addSlot(new SlotItemHandler(blockEntity.getPluginSlot(), 0, 176, 60));
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
    
    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        
        if (!slot.hasItem()) {
            return result;
        }
        
        ItemStack slotStack = slot.getItem();
        result = slotStack.copy();
        
        if (index < PLAYER_INV_START) {
            // 从容器移动到玩家物品栏
            if (!moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 从玩家物品栏移动到容器
            if (!moveItemStackTo(slotStack, STORAGE_START, PLUGIN_END, false)) {
                return ItemStack.EMPTY;
            }
        }
        
        if (slotStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        
        return result;
    }
    
    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(levelAccess, player, ModBlocks.TURRET_BASE_T1.get()) ||
               stillValid(levelAccess, player, ModBlocks.TURRET_BASE_T2.get()) ||
               stillValid(levelAccess, player, ModBlocks.TURRET_BASE_T3.get());
    }
    
    public TurretBaseBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
