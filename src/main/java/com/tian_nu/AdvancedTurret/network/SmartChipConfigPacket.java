package com.tian_nu.AdvancedTurret.network;

import com.tian_nu.AdvancedTurret.blocks.entitys.TurretBaseBlockEntity;
import com.tian_nu.AdvancedTurret.items.SmartChipItem;
import com.tian_nu.AdvancedTurret.items.SmartChipItem.TargetMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SmartChipConfigPacket {
	private final BlockPos pos; // Null if handheld
	private final TargetMode targetMode;
	private final boolean friendlyFire;
	private final boolean predictiveAiming;
	private final boolean thriftyMode;
	private final byte enabledFacesMask;
	private final List<String> blacklist;
	private final List<String> whitelist;
	private final int targetFlags;

	public SmartChipConfigPacket(BlockPos pos, TargetMode targetMode, boolean friendlyFire, boolean predictiveAiming, boolean thriftyMode, byte enabledFacesMask, List<String> blacklist, List<String> whitelist, int targetFlags) {
		this.pos = pos;
		this.targetMode = targetMode;
		this.friendlyFire = friendlyFire;
		this.predictiveAiming = predictiveAiming;
		this.thriftyMode = thriftyMode;
		this.enabledFacesMask = enabledFacesMask;
		this.blacklist = blacklist;
		this.whitelist = whitelist;
		this.targetFlags = targetFlags;
	}

	// Legacy constructor for backward compatibility
	public SmartChipConfigPacket(BlockPos pos, TargetMode targetMode, boolean friendlyFire, boolean predictiveAiming, byte enabledFacesMask, List<String> blacklist, List<String> whitelist, int targetFlags) {
		this(pos, targetMode, friendlyFire, predictiveAiming, false, enabledFacesMask, blacklist, whitelist, targetFlags);
	}

	public static void encode(SmartChipConfigPacket packet, FriendlyByteBuf buf) {
		buf.writeBoolean(packet.pos != null);
		if (packet.pos != null) {
			buf.writeBlockPos(packet.pos);
		}
		buf.writeEnum(packet.targetMode);
		buf.writeBoolean(packet.friendlyFire);
		buf.writeBoolean(packet.predictiveAiming);
		buf.writeBoolean(packet.thriftyMode);
		buf.writeByte(packet.enabledFacesMask);

		buf.writeCollection(packet.blacklist, FriendlyByteBuf::writeUtf);
		buf.writeCollection(packet.whitelist, FriendlyByteBuf::writeUtf);

		buf.writeInt(packet.targetFlags);
	}

	public static SmartChipConfigPacket decode(FriendlyByteBuf buf) {
		BlockPos pos = buf.readBoolean() ? buf.readBlockPos() : null;
		TargetMode mode = buf.readEnum(TargetMode.class);
		boolean ff = buf.readBoolean();
		boolean pa = buf.readBoolean();
		boolean tm = false; // Default for old packets
		try {
			if (buf.isReadable()) {
				tm = buf.readBoolean();
			}
		} catch (Exception e) {
			// Old packet without thriftyMode
		}
		byte faces = buf.readByte();

		List<String> blacklist = buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf);
		List<String> whitelist = buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf);

		int flags = 1; // Default
		try {
			if (buf.isReadable()) {
				flags = buf.readInt();
			}
		} catch (Exception e) {
			// End of stream if old packet
		}

		return new SmartChipConfigPacket(pos, mode, ff, pa, tm, faces, blacklist, whitelist, flags);
	}

    public static void handle(SmartChipConfigPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            
            ItemStack stackToUpdate = ItemStack.EMPTY;
            
            if (packet.pos != null) {
                // Update BlockEntity
                Level level = player.level();
                if (level.isLoaded(packet.pos) && player.distanceToSqr(packet.pos.getCenter()) < 64.0) {
                    BlockEntity be = level.getBlockEntity(packet.pos);
                    if (be instanceof TurretBaseBlockEntity base) {
                        stackToUpdate = base.getPluginStack();
                        // Mark BE as changed to sync/save
                        base.setChanged();
                    }
                }
            } else {
                // Update Main Hand Item
                ItemStack handStack = player.getItemInHand(InteractionHand.MAIN_HAND);
                if (handStack.getItem() instanceof SmartChipItem) {
                    stackToUpdate = handStack;
                }
            }
            
if (!stackToUpdate.isEmpty() && stackToUpdate.getItem() instanceof SmartChipItem) {
			// SmartChipItem.setTargetMode(stackToUpdate, packet.targetMode); // Deprecated
			SmartChipItem.setTargetFlags(stackToUpdate, packet.targetFlags);
			SmartChipItem.setFriendlyFire(stackToUpdate, packet.friendlyFire);
			SmartChipItem.setPredictiveAiming(stackToUpdate, packet.predictiveAiming);
			SmartChipItem.setThriftyMode(stackToUpdate, packet.thriftyMode);
			SmartChipItem.setEnabledFaces(stackToUpdate, packet.enabledFacesMask);
                
                // Update Lists
                stackToUpdate.getOrCreateTag().remove(SmartChipItem.KEY_BLACKLIST);
                for (String s : packet.blacklist) {
                    SmartChipItem.addToBlacklist(stackToUpdate, s);
                }
                
                stackToUpdate.getOrCreateTag().remove(SmartChipItem.KEY_WHITELIST);
                for (String s : packet.whitelist) {
                     addStringToTagList(stackToUpdate, SmartChipItem.KEY_WHITELIST, s);
                }
            }
        });
        context.get().setPacketHandled(true);
    }
    
    private static void addStringToTagList(ItemStack stack, String key, String value) {
        net.minecraft.nbt.CompoundTag tag = stack.getOrCreateTag();
        net.minecraft.nbt.ListTag list;
        if (tag.contains(key)) {
            list = tag.getList(key, net.minecraft.nbt.Tag.TAG_STRING);
        } else {
            list = new net.minecraft.nbt.ListTag();
        }
        // Avoid duplicates
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(value)) return;
        }
        list.add(net.minecraft.nbt.StringTag.valueOf(value));
        tag.put(key, list);
    }
}
