package com.tian_nu.AdvancedTurret.gui;

import com.tian_nu.AdvancedTurret.blocks.ModBlocks;
import com.tian_nu.AdvancedTurret.blocks.entitys.TurretBaseBlockEntity;
import com.tian_nu.AdvancedTurret.items.ModItems;
import com.tian_nu.AdvancedTurret.items.SmartChipItem;
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
 * <p>管理弹药槽位和玩家物品栏</p>
 * 
 * @author tian_nu
 */
public class TurretMenu extends AbstractContainerMenu {
    
    private final TurretBaseBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final ContainerData data;
    private final int containerSlots;
    
    // 槽位索引常量
    private static final int AMMO_START = 0;
    private static final int AMMO_END = AMMO_START + TurretBaseBlockEntity.AMMO_SLOTS;
    private final int pluginSlotStart;
    
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
        this.pluginSlotStart = AMMO_END;
        // 使用实际槽数量兼容旧存档
        int actualPluginSlots = Math.min(turretBase.getPluginSlotCount(), turretBase.getBasePluginSlot().getSlots());
        this.containerSlots = AMMO_END + actualPluginSlots;  // 动态插件槽数量
        
        addAmmoSlots();
        addPluginSlots();  // 支持多插件槽
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        
        addDataSlots(data);
    }
    
    // UI 重构：调整槽位坐标
    // 背景图尺寸 194x166
    // 弹药槽位 (3x3) 放在左侧
    private void addAmmoSlots() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                int x = 8 + col * 18;
                int y = 18 + row * 18;
                addSlot(new SlotItemHandler(blockEntity.getAmmoInventory(), index, x, y));
            }
        }
    }

    private void addPluginSlots() {
        // 使用实际槽数量兼容旧存档
        int slotCount = Math.min(blockEntity.getPluginSlotCount(), blockEntity.getBasePluginSlot().getSlots());
        if (slotCount == 0) return;
        
        // 添加插件槽（从弹药槽右侧开始）
        for (int i = 0; i < slotCount; i++) {
            // 第一个插件槽在弹药槽右侧，第二个在下方
            int x = 80 + (i % 2) * 18;  // 第一列80，第二列98
            int y = 60 + (i / 2) * 18;  // 第一行60，第二行78
            addSlot(new SlotItemHandler(blockEntity.getBasePluginSlot(), i, x, y));
        }
    }
    
    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = 16 + col * 18;
                int y = 84 + row * 18;
                int index = col + row * 9 + 9;
                addSlot(new Slot(playerInventory, index, x, y));
            }
        }
    }
    
    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; col++) {
            int x = 16 + col * 18;
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
            
            int playerInvStart = containerSlots;
            int playerInvEnd = playerInvStart + 27;
            int playerHotbarEnd = playerInvEnd + 9;

            if (index < playerInvStart) {
                // 从容器移动到玩家背包
                if (!this.moveItemStackTo(itemstack1, playerInvStart, playerHotbarEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家背包移动到容器
                // 使用实际槽数量兼容旧存档
                int pluginSlotCount = Math.min(blockEntity.getPluginSlotCount(), blockEntity.getBasePluginSlot().getSlots());
                if (pluginSlotCount > 0 && isPluginItem(itemstack1)) {
                    // 尝试移动到插件槽
                    if (!this.moveItemStackTo(itemstack1, pluginSlotStart, pluginSlotStart + pluginSlotCount, false)) {
                        // 插件槽满了，尝试移动到弹药槽
                        if (!this.moveItemStackTo(itemstack1, AMMO_START, AMMO_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else if (!this.moveItemStackTo(itemstack1, AMMO_START, AMMO_END, false)) {
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

    private boolean isPluginItem(ItemStack stack) {
        return stack.getItem() instanceof SmartChipItem
                || stack.is(ModItems.CREATIVE_POWER_COMPONENT.get())
                || stack.is(ModItems.SOLAR_PLUGIN.get())
                || stack.is(ModItems.AMMO_RECYCLING_PLUGIN.get())
                || stack.is(ModItems.REDSTONE_CONVERSION_PLUGIN.get())
                || stack.is(ModItems.DESTRUCTION_PLUGIN.get());
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.levelAccess, player, ModBlocks.TURRET_BASE_T1.get()) ||
               stillValid(this.levelAccess, player, ModBlocks.TURRET_BASE_T2.get()) ||
                stillValid(this.levelAccess, player, ModBlocks.TURRET_BASE_T3.get()) ||
                stillValid(this.levelAccess, player, ModBlocks.TURRET_BASE_T4.get()) ||
                stillValid(this.levelAccess, player, ModBlocks.TURRET_BASE_T5.get());
    }
}
