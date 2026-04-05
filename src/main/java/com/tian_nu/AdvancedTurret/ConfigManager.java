package com.tian_nu.AdvancedTurret;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户端配置管理器
 */
public class ConfigManager {

    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve(TurretMod.MOD_ID);
    private static final File CONFIG_FILE = CONFIG_DIR.resolve("client_config.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ClientConfig config = new ClientConfig();

    public static class ClientConfig {
        public float backgroundAlpha = 0.0F;
        public float energyBarAlpha = 1.0F;
        public List<String> recentEntityScans = new ArrayList<>();

        public ClientConfig() {
        }

        public ClientConfig(float backgroundAlpha, float energyBarAlpha) {
            this.backgroundAlpha = backgroundAlpha;
            this.energyBarAlpha = energyBarAlpha;
        }
    }

    public static void loadConfig() {
        try {
            if (!CONFIG_DIR.toFile().exists()) {
                CONFIG_DIR.toFile().mkdirs();
            }

            if (CONFIG_FILE.exists()) {
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    config = GSON.fromJson(reader, ClientConfig.class);
                    if (config == null) {
                        config = new ClientConfig();
                    }
                    if (config.recentEntityScans == null) {
                        config.recentEntityScans = new ArrayList<>();
                    }
                }
            } else {
                saveConfig();
            }
        } catch (Exception e) {
            config = new ClientConfig();
        }
    }

    public static void saveConfig() {
        try {
            if (!CONFIG_DIR.toFile().exists()) {
                CONFIG_DIR.toFile().mkdirs();
            }

            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(config, writer);
            }
        } catch (Exception e) {
            // 保存失败时静默处理
        }
    }

    public static float getBackgroundAlpha() {
        return config.backgroundAlpha;
    }

    public static void setBackgroundAlpha(float alpha) {
        config.backgroundAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
    }

    public static float getEnergyBarAlpha() {
        return config.energyBarAlpha;
    }

    public static void setEnergyBarAlpha(float alpha) {
        config.energyBarAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
    }

    public static List<String> getRecentEntityScans() {
        if (config.recentEntityScans == null) {
            config.recentEntityScans = new ArrayList<>();
        }
        return new ArrayList<>(config.recentEntityScans);
    }

    public static void pushRecentEntityScan(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return;
        }
        if (config.recentEntityScans == null) {
            config.recentEntityScans = new ArrayList<>();
        }

        config.recentEntityScans.removeIf(entityId::equals);
        config.recentEntityScans.add(0, entityId);

        final int maxSize = 12;
        while (config.recentEntityScans.size() > maxSize) {
            config.recentEntityScans.remove(config.recentEntityScans.size() - 1);
        }

        saveConfig();
    }
}
