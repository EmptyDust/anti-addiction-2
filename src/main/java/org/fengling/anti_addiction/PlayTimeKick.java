package org.fengling.anti_addiction;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class PlayTimeKick {

    Map<UUID, Integer> playerPlayTimes = new HashMap<>();
    private Timer backupTimer;
    private Timer resetTimer;

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        Anti_addiction.LOGGER.info("HELLO from server starting");
        loadPlayTimes();
        scheduleBackup();
        scheduleDailyReset();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        savePlayTimes();
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event)
    {

        if (event.getEntity() instanceof ServerPlayer player) {
            String playerName = player.getName().getString();
            Anti_addiction.LOGGER.info("Player {} has logged in.", playerName);

            UUID playerId = player.getUUID();
            int playTime = playerPlayTimes.getOrDefault(playerId, 0);

            playerPlayTimes.put(playerId, playTime);
            checkAndKickPlayer(player);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.player;
            UUID playerId = player.getUUID();

            int playTime = playerPlayTimes.getOrDefault(playerId, 0);
            playTime++;
            playerPlayTimes.put(playerId, playTime);

            checkAndKickPlayer(player);

            displayPlayTime(player, playTime / 20);
        }
    }

    private void checkAndKickPlayer(ServerPlayer player) {
        UUID playerId = player.getUUID();
        int playTime = playerPlayTimes.getOrDefault(playerId, 0);

        ModConfig config = ModConfig.loadConfig("config/mod_config.json");
        int maxPlayTimeMinutes = config.getServerConfig().getMaxPlayTimeMinutes();

        if (playTime >= maxPlayTimeMinutes * 60 * 20) {
            player.connection.disconnect(Component.literal("您今天的在线时间已达到上限"));
        }
    }

    private void displayPlayTime(ServerPlayer player, int playTime) {
        // 计算小时和分钟
        int minutes = playTime / 60;
        int second = playTime % 60;

        Component actionbar = Component.literal("已玩时间:" + String.format("%02d:%02d", minutes, second));

        player.connection.send(new ClientboundSetActionBarTextPacket(actionbar));
        player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 70, 20));
    }

    private void loadPlayTimes() {
        ModConfig config = ModConfig.loadConfig("config/mod_config.json");
        String playtimeFilePath = config.getServerConfig().getPlaytimeFilePath();
        try {
            String content = new String(Files.readAllBytes(Paths.get(playtimeFilePath)));
            playerPlayTimes = new Gson().fromJson(content, new TypeToken<HashMap<UUID, Integer>>(){}.getType());
            Anti_addiction.LOGGER.info("Play times loaded from file.");
        } catch (IOException e) {
            playerPlayTimes = new HashMap<>();
            Anti_addiction.LOGGER.info("No playtime file found, creating new.");
        }
    }

    void savePlayTimes() {
        ModConfig config = ModConfig.loadConfig("config/mod_config.json");
        String playtimeFilePath = config.getServerConfig().getPlaytimeFilePath();
        try (Writer writer = new FileWriter(playtimeFilePath)) {
            new Gson().toJson(playerPlayTimes, writer);
            Anti_addiction.LOGGER.info("Play times saved to file.");
        } catch (IOException e) {
            e.printStackTrace();
            Anti_addiction.LOGGER.error("Failed to save play times to file!", e);
        }
    }

    private void scheduleBackup() {
        ModConfig config = ModConfig.loadConfig("config/mod_config.json");
        int backupIntervalMinutes = config.getServerConfig().getBackupIntervalMinutes();

        backupTimer = new Timer();
        backupTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                savePlayTimes();
            }
        }, 0, backupIntervalMinutes * 60 * 1000L); // 每x分钟备份一次
        Anti_addiction.LOGGER.info("Playtime backup scheduled every {} minutes.", backupIntervalMinutes);
    }

    private void scheduleDailyReset() {
        ModConfig config = ModConfig.loadConfig("config/mod_config.json");
        String resetTimeStr = config.getServerConfig().getResetTime();
        LocalTime resetTime = LocalTime.parse(resetTimeStr, DateTimeFormatter.ISO_LOCAL_TIME);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextResetTime = now.toLocalDate().atTime(resetTime);

        if (now.toLocalTime().isAfter(resetTime)) {
            nextResetTime = nextResetTime.plusDays(1);
        }

        long initialDelay = ChronoUnit.MILLIS.between(now, nextResetTime);

        Anti_addiction.LOGGER.info("Current time: {}, Next reset time: {}, Initial delay: {} ms", now, nextResetTime, initialDelay);

        resetTimer = new Timer();
        resetTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Anti_addiction.LOGGER.info("Daily playtime reset triggered at: {}", LocalDateTime.now());
                playerPlayTimes.clear();
                savePlayTimes();
                Anti_addiction.LOGGER.info("Playtime reset complete.");
            }
        }, initialDelay, 24 * 60 * 60 * 1000L); // 每天凌晨定时重置, initialDelay is already in milliseconds
        Anti_addiction.LOGGER.info("Daily playtime reset scheduled for {}.", resetTimeStr);
    }
}