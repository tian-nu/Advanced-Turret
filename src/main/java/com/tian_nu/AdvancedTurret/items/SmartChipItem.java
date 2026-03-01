package com.tian_nu.AdvancedTurret.items;

import com.tian_nu.AdvancedTurret.blocks.entitys.TurretBaseBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SmartChipItem extends Item {
    
    // NBT Keys
    public static final String KEY_TARGET_MODE = "TargetMode";
    public static final String KEY_FRIENDLY_FIRE = "FriendlyFire";
    public static final String KEY_PREDICTIVE_AIMING = "PredictiveAiming";
    public static final String KEY_ENABLED_FACES = "EnabledFaces";
    public static final String KEY_BLACKLIST = "Blacklist";
    public static final String KEY_WHITELIST = "Whitelist";

    public enum TargetMode {
        HOSTILE,        // 仅敌对
        NEUTRAL,        // 仅中立
        FRIENDLY,       // 仅友善 (被动生物)
        PLAYERS,        // 仅玩家
        BLACKLIST_ONLY, // 仅黑名单
        ALL;            // 所有生物

        public static TargetMode byId(int id) {
            if (id < 0 || id >= values().length) return HOSTILE;
            return values()[id];
        }
    }
    
    // NBT Key for combination mode
    public static final String KEY_TARGET_FLAGS = "TargetFlags";
    
    // Flags
    public static final int FLAG_HOSTILE = 1;
    public static final int FLAG_NEUTRAL = 2;
    public static final int FLAG_FRIENDLY = 4;
    public static final int FLAG_PLAYERS = 8;
    
    public SmartChipItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new com.tian_nu.AdvancedTurret.gui.SmartChipConfigScreen(player.getItemInHand(hand), null));
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return super.useOn(context);
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof TurretBaseBlockEntity base) {
            ItemStack heldStack = context.getItemInHand();
            // 尝试插入插件
            if (!base.hasPluginSlot()) {
                return InteractionResult.FAIL;
            }

            ItemStack existingPlugin = base.getBasePluginSlot().getStackInSlot(0);
            if (existingPlugin.isEmpty()) {
                if (!level.isClientSide) {
                    ItemStack toInsert = heldStack.copy();
                    toInsert.setCount(1);
                    base.getBasePluginSlot().insertItem(0, toInsert, false);
                    heldStack.shrink(1);
                    player.displayClientMessage(Component.translatable("message.advanced_turret.chip_installed").withStyle(ChatFormatting.GREEN), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            } else {
                 if (!level.isClientSide) {
                     player.displayClientMessage(Component.translatable("message.advanced_turret.chip_slot_full").withStyle(ChatFormatting.RED), true);
                 }
                 return InteractionResult.FAIL;
            }
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("item.advanced_turret.smart_chip.tooltip").withStyle(ChatFormatting.GRAY));
        
        CompoundTag tag = stack.getOrCreateTag();
        TargetMode mode = TargetMode.byId(tag.getInt(KEY_TARGET_MODE));
        tooltipComponents.add(Component.translatable("gui.advanced_turret.target_mode." + mode.name().toLowerCase()).withStyle(ChatFormatting.BLUE));
        
        if (tag.getBoolean(KEY_FRIENDLY_FIRE)) {
            tooltipComponents.add(Component.translatable("gui.advanced_turret.friendly_fire.enabled").withStyle(ChatFormatting.GREEN));
        }
        
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }

    public static void setTargetFlags(ItemStack stack, int flags) {
        stack.getOrCreateTag().putInt(KEY_TARGET_FLAGS, flags);
    }
    
    public static int getTargetFlags(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(KEY_TARGET_FLAGS)) {
            // Default to HOSTILE if not set
            return FLAG_HOSTILE;
        }
        return tag.getInt(KEY_TARGET_FLAGS);
    }

    // ========== NBT Helpers ==========

    public static void setTargetMode(ItemStack stack, TargetMode mode) {
        stack.getOrCreateTag().putInt(KEY_TARGET_MODE, mode.ordinal());
    }

    public static TargetMode getTargetMode(ItemStack stack) {
        return TargetMode.byId(stack.getOrCreateTag().getInt(KEY_TARGET_MODE));
    }

    public static void setFriendlyFire(ItemStack stack, boolean enabled) {
        stack.getOrCreateTag().putBoolean(KEY_FRIENDLY_FIRE, enabled);
    }

    public static boolean isFriendlyFire(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean(KEY_FRIENDLY_FIRE);
    }

    public static void setPredictiveAiming(ItemStack stack, boolean enabled) {
        stack.getOrCreateTag().putBoolean(KEY_PREDICTIVE_AIMING, enabled);
    }

    public static boolean isPredictiveAiming(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean(KEY_PREDICTIVE_AIMING);
    }

    public static void setEnabledFaces(ItemStack stack, byte mask) {
        stack.getOrCreateTag().putByte(KEY_ENABLED_FACES, mask);
    }

    public static byte getEnabledFaces(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(KEY_ENABLED_FACES)) return 0b111111; // Default all enabled
        return tag.getByte(KEY_ENABLED_FACES);
    }

    public static List<String> getBlacklist(ItemStack stack) {
        List<String> list = new ArrayList<>();
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains(KEY_BLACKLIST)) {
            ListTag tagList = tag.getList(KEY_BLACKLIST, Tag.TAG_STRING);
            for (int i = 0; i < tagList.size(); i++) {
                list.add(tagList.getString(i));
            }
        }
        return list;
    }

    public static void addToBlacklist(ItemStack stack, String entityId) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag list;
        if (tag.contains(KEY_BLACKLIST)) {
            list = tag.getList(KEY_BLACKLIST, Tag.TAG_STRING);
        } else {
            list = new ListTag();
        }
        // Avoid duplicates
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(entityId)) return;
        }
        list.add(StringTag.valueOf(entityId));
        tag.put(KEY_BLACKLIST, list);
    }

    public static List<String> getWhitelist(ItemStack stack) {
        List<String> list = new ArrayList<>();
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains(KEY_WHITELIST)) {
            ListTag tagList = tag.getList(KEY_WHITELIST, Tag.TAG_STRING);
            for (int i = 0; i < tagList.size(); i++) {
                list.add(tagList.getString(i));
            }
        }
        return list;
    }
}
