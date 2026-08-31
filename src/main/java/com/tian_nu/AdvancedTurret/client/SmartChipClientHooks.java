package com.tian_nu.AdvancedTurret.client;

import com.tian_nu.AdvancedTurret.gui.SmartChipConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/**
 * 智能芯片配置界面的客户端入口。
 * 与物理客户端环境隔离，保证服务端加载安全。
 */
public class SmartChipClientHooks {

    /**
     * 打开智能芯片配置界面。
     */
    public static void openConfigScreen(ItemStack stack) {
        Minecraft.getInstance().setScreen(new SmartChipConfigScreen(stack, null));
    }
}