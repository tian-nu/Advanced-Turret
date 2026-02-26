package com.tian_nu.AdvancedTurret.gui;

import com.tian_nu.AdvancedTurret.blocks.ModBlocks;
import com.tian_nu.AdvancedTurret.blocks.entitys.TurretBaseBlockEntity;
import net.minecraft.core.Direction;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class TurretFaceConfigMenu extends AbstractContainerMenu {

    private final TurretBaseBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final ContainerData data;
    private static final int UPGRADE_SLOTS = 6 * TurretBaseBlockEntity.MAX_UPGRADE_SLOTS_PER_FACE;
    private static final int PLAYER_INV_START = UPGRADE_SLOTS;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int PLAYER_HOTBAR_END = PLAYER_INV_END + 9;

    public TurretFaceConfigMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData), new SimpleContainerData(3), false);
    }

    private static BlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf buf) {
        return playerInventory.player.level().getBlockEntity(buf.readBlockPos());
    }

    public TurretFaceConfigMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity, null, true);
    }

    private TurretFaceConfigMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity, ContainerData clientData, boolean isServerSide) {
        super(ModMenuTypes.TURRET_FACE_CONFIG.get(), containerId);

        if (!(blockEntity instanceof TurretBaseBlockEntity turretBase)) {
            throw new IllegalStateException("Invalid block entity type: " +
                    (blockEntity != null ? blockEntity.getClass().getSimpleName() : "null"));
        }

        this.blockEntity = turretBase;
        this.levelAccess = ContainerLevelAccess.create(turretBase.getLevel(), turretBase.getBlockPos());
        if (isServerSide) {
            this.data = new ContainerData() {
                @Override
                public int get(int index) {
                    return switch (index) {
                        case 0 -> TurretFaceConfigMenu.this.blockEntity.getEnergyStored();
                        case 1 -> TurretFaceConfigMenu.this.blockEntity.getMaxEnergyStored();
                        default -> 0;
                    };
                }

                @Override
                public void set(int index, int value) {
                }

                @Override
                public int getCount() {
                    return 3;
                }
            };
        } else {
            this.data = clientData;
        }

        addFaceUpgradeSlots();
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        addDataSlots(this.data);
    }

    private void addFaceUpgradeSlots() {
        Direction[] faces = new Direction[] {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (int faceIndex = 0; faceIndex < faces.length; faceIndex++) {
            Direction face = faces[faceIndex];
            IItemHandlerModifiable handler = (IItemHandlerModifiable) blockEntity.getFaceUpgradeSlots(face);
            for (int upgradeIndex = 0; upgradeIndex < TurretBaseBlockEntity.MAX_UPGRADE_SLOTS_PER_FACE; upgradeIndex++) {
                int x = 8 + faceIndex * 18;
                int y = 18 + upgradeIndex * 18;
                int slotIndex = upgradeIndex;
                addSlot(new ConditionalSlotItemHandler(handler, slotIndex, x, y, () -> blockEntity.hasTurretOnFace(face) && slotIndex < blockEntity.getUpgradeSlotsPerFace()));
            }
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
        return blockEntity;
    }

    public int getEnergy() {
        return data.get(0);
    }

    public int getMaxEnergy() {
        return data.get(1);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index < PLAYER_INV_START) {
                if (!this.moveItemStackTo(itemstack1, PLAYER_INV_START, PLAYER_HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(itemstack1, 0, PLAYER_INV_START, false)) {
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
                stillValid(this.levelAccess, player, ModBlocks.TURRET_BASE_T3.get()) ||
                stillValid(this.levelAccess, player, ModBlocks.TURRET_BASE_T4.get()) ||
                stillValid(this.levelAccess, player, ModBlocks.TURRET_BASE_T5.get());
    }

    private static final class ConditionalSlotItemHandler extends SlotItemHandler {

        private final ActiveSupplier activeSupplier;

        private ConditionalSlotItemHandler(IItemHandler itemHandler, int index, int xPosition, int yPosition, ActiveSupplier activeSupplier) {
            super(itemHandler, index, xPosition, yPosition);
            this.activeSupplier = activeSupplier;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return activeSupplier.isActive() && super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(Player playerIn) {
            if (this.hasItem()) return true;
            return activeSupplier.isActive() && super.mayPickup(playerIn);
        }

        @Override
        public boolean isActive() {
            if (this.hasItem()) return true;
            return activeSupplier.isActive();
        }
    }

    @FunctionalInterface
    private interface ActiveSupplier {
        boolean isActive();
    }
}
