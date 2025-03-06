package org.fengling.anti_addiction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModConfig {

    // Server configuration
    public static class ServerConfig {
        private boolean enableAntiAddiction = false; // 防沉迷功能默认关闭
        private String aiChatServerAddress = "https://api.meguhx.top:50721/v1/chat/completions"; // 默认 AI 聊天服务器地址
        private int maxPlayTimeMinutes = 60 * 4; // 最大游戏时间，默认 4 小时
        private String playtimeFilePath = "config/playtime.json"; // 游戏时间数据文件路径
        private String resetTime = "05:00:00"; // 每日重置时间，默认早上 5 点
        private int backupIntervalMinutes = 5; // 备份间隔，默认 5 分钟

        public ServerConfig() {
            // 默认构造函数，Gson 需要
        }


        // Getters and Setters for Server Config
        public boolean isEnableAntiAddiction() {
            return enableAntiAddiction;
        }

        public void setEnableAntiAddiction(boolean enableAntiAddiction) {
            this.enableAntiAddiction = enableAntiAddiction;
        }

        public String getAiChatServerAddress() {
            return aiChatServerAddress;
        }

        public void setAiChatServerAddress(String aiChatServerAddress) {
            this.aiChatServerAddress = aiChatServerAddress;
        }

        public int getMaxPlayTimeMinutes() {
            return maxPlayTimeMinutes;
        }

        public void setMaxPlayTimeMinutes(int maxPlayTimeMinutes) {
            this.maxPlayTimeMinutes = maxPlayTimeMinutes;
        }

        public String getPlaytimeFilePath() {
            return playtimeFilePath;
        }

        public void setPlaytimeFilePath(String playtimeFilePath) {
            this.playtimeFilePath = playtimeFilePath;
        }

        public String getResetTime() {
            return resetTime;
        }

        public void setResetTime(String resetTime) {
            this.resetTime = resetTime;
        }

        public int getBackupIntervalMinutes() {
            return backupIntervalMinutes;
        }

        public void setBackupIntervalMinutes(int backupIntervalMinutes) {
            this.backupIntervalMinutes = backupIntervalMinutes;
        }


        @Override
        public String toString() {
            return "ServerConfig{" +
                    "enableAntiAddiction=" + enableAntiAddiction +
                    ", aiChatServerAddress='" + aiChatServerAddress + '\'' +
                    ", maxPlayTimeMinutes=" + maxPlayTimeMinutes +
                    ", playtimeFilePath='" + playtimeFilePath + '\'' +
                    ", resetTime='" + resetTime + '\'' +
                    ", backupIntervalMinutes=" + backupIntervalMinutes +
                    '}';
        }
    }

    // Player configuration
    public static class PlayerConfig {
        private UUID playerUuid;
        private String playerName;
        private long playedTime;
        private String codeforcesID;

        public PlayerConfig() {
            // Default constructor for Gson
        }

        public PlayerConfig(UUID playerUuid, String playerName, long playedTime, String codeforcesID) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.playedTime = playedTime;
            this.codeforcesID = codeforcesID;
        }

        // Getters and Setters for Player Config
        public UUID getPlayerUuid() {
            return playerUuid;
        }

        public void setPlayerUuid(UUID playerUuid) {
            this.playerUuid = playerUuid;
        }

        public String getPlayerName() {
            return playerName;
        }

        public void setPlayerName(String playerName) {
            this.playerName = playerName;
        }

        public long getPlayedTime() {
            return playedTime;
        }

        public void setPlayedTime(long playedTime) {
            this.playedTime = playedTime;
        }

        public String getCodeforcesID() {
            return codeforcesID;
        }

        public void setCodeforcesID(String codeforcesID) {
            this.codeforcesID = codeforcesID;
        }

        @Override
        public String toString() {
            return "PlayerConfig{" +
                    "playerUuid=" + playerUuid +
                    ", playerName='" + playerName + '\'' +
                    ", playedTime=" + playedTime +
                    ", codeforcesID='" + codeforcesID + '\'' +
                    '}';
        }
    }

    private ServerConfig serverConfig;
    private List<PlayerConfig> playerConfigs;

    public ModConfig() {
        this.serverConfig = new ServerConfig(); // Default server config
        this.playerConfigs = new ArrayList<>(); // Default player config list
    }

    // Getters and Setters for ModConfig
    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public List<PlayerConfig> getPlayerConfigs() {
        return playerConfigs;
    }

    public void setPlayerConfigs(List<PlayerConfig> playerConfigs) {
        this.playerConfigs = playerConfigs;
    }

    // Load config from JSON file
    public static ModConfig loadConfig(String filePath) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File configFile = new File(filePath);

        if (!configFile.exists()) {
            // If config file doesn't exist, create default config and save
            ModConfig defaultConfig = new ModConfig();
            defaultConfig.saveConfig(filePath);
            return defaultConfig;
        }

        try (Reader reader = new FileReader(configFile)) {
            return gson.fromJson(reader, ModConfig.class);
        } catch (IOException e) {
            e.printStackTrace(); // Print error log, or handle error more appropriately
            return new ModConfig(); // Load failed, return default config
        }
    }

    // Save config to JSON file
    public void saveConfig(String filePath) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File configFile = new File(filePath);

        try (Writer writer = new FileWriter(configFile)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace(); // Print error log, or handle error more appropriately
        }
    }

    public static void main(String[] args) {
        // Example usage

        // 1. Load ModConfig object (or create new if not exists)
        ModConfig config = ModConfig.loadConfig("config/mod_config.json"); // Assuming config file path

        // 2. Modify server config
        config.getServerConfig().setEnableAntiAddiction(true);
        config.getServerConfig().setAiChatServerAddress("http://example.com/ai_chat");
        config.getServerConfig().setMaxPlayTimeMinutes(60 * 5); // Set max play time to 5 hours
        config.getServerConfig().setPlaytimeFilePath("config/my_playtime.json"); // Custom playtime file path
        config.getServerConfig().setResetTime("06:30:00"); // Set reset time to 6:30 AM
        config.getServerConfig().setBackupIntervalMinutes(10); // Set backup interval to 10 minutes


        // 3. Add player config
        config.getPlayerConfigs().add(new PlayerConfig(UUID.randomUUID(), "Player1", 3600000, "player1_cf"));
        config.getPlayerConfigs().add(new PlayerConfig(UUID.randomUUID(), "Player2", 7200000, "player2_cf"));

        // 4. Save config to file
        config.saveConfig("config/mod_config.json");

        // 5. Load config from file again and print
        ModConfig loadedConfig = ModConfig.loadConfig("config/mod_config.json");
        System.out.println("Loaded Server Config: " + loadedConfig.getServerConfig());
        System.out.println("Loaded Player Configs: " + loadedConfig.getPlayerConfigs());


        // Example of parsing resetTime string to LocalTime
        String resetTimeStr = loadedConfig.getServerConfig().getResetTime();
        try {
            LocalTime resetTime = LocalTime.parse(resetTimeStr, DateTimeFormatter.ISO_LOCAL_TIME);
            System.out.println("Parsed Reset Time: " + resetTime);
        } catch (DateTimeParseException e) {
            System.err.println("Error parsing resetTime: " + e.getMessage());
        }
    }
}