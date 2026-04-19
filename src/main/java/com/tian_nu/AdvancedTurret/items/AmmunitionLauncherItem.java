package com.tian_nu.AdvancedTurret.items;

import com.tian_nu.AdvancedTurret.Config;
import com.tian_nu.AdvancedTurret.entity.LauncherGrenadeEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AmmunitionLauncherItem extends Item {

    private static final int COOLDOWN_TICKS = 20;
    private static final double BASE_LAUNCH_SPEED = 1.65D;
    private static final double CARRIER_SPEED_FACTOR = 0.85D;
    private static final double FORWARD_SPAWN_OFFSET = 0.9D;

    public AmmunitionLauncherItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack launcherStack = player.getItemInHand(hand);

        int ammoSlot = findGrenadeAmmo(player);
        boolean hasAmmo = ammoSlot >= 0 || player.getAbilities().instabuild;
        if (!hasAmmo) {
            return InteractionResultHolder.fail(launcherStack);
        }

        if (!level.isClientSide) {
            var look = player.getLookAngle().normalize();
            var playerVelocity = player.getDeltaMovement();
            double carriedForwardSpeed = Math.max(0.0D, playerVelocity.dot(look));
            Vec3 launchVelocity = look.scale(BASE_LAUNCH_SPEED + carriedForwardSpeed).add(playerVelocity.scale(CARRIER_SPEED_FACTOR));

            LauncherGrenadeEntity grenade = new LauncherGrenadeEntity(
                    level,
                    player.getX() + look.x * FORWARD_SPAWN_OFFSET,
                    player.getEyeY() - 0.1D + look.y * FORWARD_SPAWN_OFFSET,
                    player.getZ() + look.z * FORWARD_SPAWN_OFFSET,
                    (float) Config.grenadeLauncherDirectDamage);
            grenade.setOwner(player);
            grenade.setExplosionDamage((float) Config.grenadeLauncherExplosionDamage);
            grenade.setExplosionRadius((float) Config.grenadeLauncherExplosionRadius);
            grenade.setDestroyBlocks(false);
            grenade.setDeltaMovement(launchVelocity);
            level.addFreshEntity(grenade);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.35F, 1.35F);

            if (!player.getAbilities().instabuild && ammoSlot >= 0) {
                player.getInventory().getItem(ammoSlot).shrink(1);
            }
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        player.swing(hand, true);
        return InteractionResultHolder.sidedSuccess(launcherStack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.advanced_turret.ammunition_launcher.tooltip").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    private int findGrenadeAmmo(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(ModItems.GRENADE.get())) {
                return i;
            }
        }
        return -1;
    }
}
