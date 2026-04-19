package com.tian_nu.AdvancedTurret.items;

import com.tian_nu.AdvancedTurret.Config;
import com.tian_nu.AdvancedTurret.entity.GrenadeEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HandGrenadeItem extends Item {

    private static final float THROW_SPEED = 1.35F;
    private static final float THROW_ANGLE_OFFSET = 2.0F;
    private static final double THROW_SPAWN_FORWARD_OFFSET = 0.6D;

    public HandGrenadeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            var look = player.getLookAngle();
            GrenadeEntity grenade = new GrenadeEntity(
                    level,
                    player.getX() + look.x * THROW_SPAWN_FORWARD_OFFSET,
                    player.getEyeY() - 0.1D + look.y * THROW_SPAWN_FORWARD_OFFSET,
                    player.getZ() + look.z * THROW_SPAWN_FORWARD_OFFSET,
                    (float) Config.grenadeLauncherDirectDamage);
            grenade.setOwner(player);
            grenade.setExplosionDamage((float) Config.grenadeLauncherExplosionDamage);
            grenade.setExplosionRadius((float) Config.grenadeLauncherExplosionRadius);
            grenade.setDestroyBlocks(false);
            grenade.shootFromRotation(player, player.getXRot() - THROW_ANGLE_OFFSET, player.getYRot(), 0.0F, THROW_SPEED, 0.4F);
            level.addFreshEntity(grenade);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.8F);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.advanced_turret.hand_grenade.tooltip").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
