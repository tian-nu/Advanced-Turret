package com.tian_nu.AdvancedTurret;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;

/**
 * 客户端配置管理器
 * 
 * <p>负责保存和加载客户端GUI设置</p>
 * 
 * @author tian_nu
 */
public class ConfigManager {
    
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve(TurretMod.MOD_ID);
    private static final File CONFIG_FILE = CONFIG_DIR.resolve("client_config.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static ClientConfig config = new ClientConfig();
    
    /**
     * 客户端配置数据类
     */
    public static class ClientConfig {
        public float backgroundAlpha = 1.0F;
        public float energyBarAlpha = 1.0F;
        
        public ClientConfig() {}
        
        public ClientConfig(float backgroundAlpha, float energyBarAlpha) {
            this.backgroundAlpha = backgroundAlpha;
            this.energyBarAlpha = energyBarAlpha;
        }
    }
    
    /**
     * 加载配置
     */
    public static void loadConfig() {
        try {
            if (!CONFIG_DIR.toFile().exists()) {
                CONFIG_DIR.toFile().mkdirs();
            }
            
            if (CONFIG_FILE.exists()) {
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    config = GSON.fromJson(reader, ClientConfig.class);
                }
            } else {
                saveConfig();
            }
        } catch (Exception e) {
            config = new ClientConfig();
        }
    }
    
    /**
     * 保存配置
     */
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
}
