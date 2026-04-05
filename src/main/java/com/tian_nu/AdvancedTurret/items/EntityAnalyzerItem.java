package com.tian_nu.AdvancedTurret.items;

import com.tian_nu.AdvancedTurret.client.EntityAnalyzerClientHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 生命体分析机：右键生物记录实体 ID 并打开结果界面。
 */
public class EntityAnalyzerItem extends Item {

    public EntityAnalyzerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (usedHand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(interactionTarget.getType());
        if (key == null) {
            return InteractionResult.PASS;
        }

        String entityId = key.toString();
        if (player.level().isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> EntityAnalyzerClientHooks.handleEntityScan(entityId));
        }

        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.advanced_turret.entity_analyzer.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.advanced_turret.entity_analyzer.tooltip_2").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
