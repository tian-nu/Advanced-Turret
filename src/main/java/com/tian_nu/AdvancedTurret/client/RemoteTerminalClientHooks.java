package com.tian_nu.AdvancedTurret.client;

import com.tian_nu.AdvancedTurret.gui.RemoteTerminalScreen;
import com.tian_nu.AdvancedTurret.network.RemoteTerminalBaseInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;


import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 远程终端客户端回调。
 */
public final class RemoteTerminalClientHooks {

    private static volatile List<RemoteTerminalBaseInfo> latestEntries = List.of();
    private static volatile boolean highlightEnabled = false;
    private static volatile String highlightedBaseKey = "";
    private static final Set<String> pinnedBaseKeys = new LinkedHashSet<>();

    private RemoteTerminalClientHooks() {
    }

    public static void openScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new RemoteTerminalScreen());
    }

    public static void openScreenForBase(String dimensionId, BlockPos pos) {
        if (dimensionId == null || dimensionId.isBlank() || pos == null) {
            openScreen();
            return;
        }
        RemoteTerminalScreen.prepareNextSelectedBase(dimensionId, pos);
        openScreen();
    }


    public static void handleBaseList(List<RemoteTerminalBaseInfo> entries) {
        latestEntries = List.copyOf(entries);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof RemoteTerminalScreen screen) {
            screen.applyServerData(entries);
        }
    }

    public static void handleOperationResult(String messageKey, int arg1, int arg2) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof RemoteTerminalScreen screen) {
            screen.applyOperationResult(messageKey, arg1, arg2);
        }
    }

    public static List<RemoteTerminalBaseInfo> getLatestEntries() {
        return latestEntries;
    }

    public static boolean isHighlightEnabled() {
        return highlightEnabled;
    }

    public static void toggleHighlightEnabled() {
        highlightEnabled = !highlightEnabled;
    }

    public static String getHighlightedBaseKey() {
        return highlightedBaseKey;
    }

    public static boolean toggleHighlightedBaseKey(String key) {
        if (key == null || key.isBlank()) {
            highlightedBaseKey = "";
            return false;
        }
        if (key.equals(highlightedBaseKey)) {
            highlightedBaseKey = "";
            return false;
        }
        highlightedBaseKey = key;
        return true;
    }

    public static synchronized boolean togglePinnedBaseKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        if (pinnedBaseKeys.contains(key)) {
            pinnedBaseKeys.remove(key);
            return false;
        }
        pinnedBaseKeys.add(key);
        return true;
    }

    public static synchronized boolean isPinned(String key) {
        return key != null && pinnedBaseKeys.contains(key);
    }

    public static synchronized List<String> getPinnedBaseKeys() {
        return new ArrayList<>(pinnedBaseKeys);
    }

    public static void clearLatestEntries() {
        latestEntries = new ArrayList<>();
    }
}
