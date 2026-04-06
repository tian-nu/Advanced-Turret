package com.tian_nu.AdvancedTurret.client;

import com.tian_nu.AdvancedTurret.ConfigManager;
import com.tian_nu.AdvancedTurret.gui.EntityAnalyzerResultScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 生命体分析机客户端行为。
 */
public final class EntityAnalyzerClientHooks {

    private EntityAnalyzerClientHooks() {
    }

    public static void handleEntityScan(String entityId, boolean openGui) {
        if (entityId == null || entityId.isBlank()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ConfigManager.pushRecentEntityScan(entityId);

        if (minecraft.keyboardHandler != null) {
            minecraft.keyboardHandler.setClipboard(entityId);
        }

        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(
                Component.translatable("message.advanced_turret.entity_analyzer.copied", entityId).withStyle(ChatFormatting.GREEN),
                true
            );
        }

        if (openGui) {
            minecraft.setScreen(new EntityAnalyzerResultScreen(entityId, ConfigManager.getRecentEntityScans()));
        }
    }

}
