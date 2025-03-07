package org.fengling.anti_addiction;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

public class PlaytimeCommandHandler {

    private final CommandRegistry commandRegistry; // Reference back to CommandRegistry for helper methods

    public PlaytimeCommandHandler(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    public int executePlaytimeSet(CommandSourceStack source, List<ServerPlayer> targets, int playtimeMinutes) {
        ModConfig config = ModConfig.loadConfig("config/mod_config.json");
        PlayTimeKick playtimeTracker = commandRegistry.getPlayTimeKickInstance();
        if (playtimeTracker == null) {
            commandRegistry.sendErrorMessage(source, "Playtime tracker not initialized.");
            return 0;
        }
        for (ServerPlayer player : targets) {
            UUID playerId = player.getUUID();
            playtimeTracker.playerPlayTimes.put(playerId, playtimeMinutes * 60 * 20);
            player.sendSystemMessage(Component.literal("已将您的游戏时间设置为 " + playtimeMinutes + " 分钟"));
        }
        playtimeTracker.savePlayTimes();
        return 1;
    }

    public int executePlaytimeSetMaxTime(CommandSourceStack source, int maxTimeMinutes) {
        ModConfig config = ModConfig.loadConfig("config/mod_config.json");
        config.getServerConfig().setMaxPlayTimeMinutes(maxTimeMinutes);
        config.saveConfig("config/mod_config.json");
        source.sendSystemMessage(Component.literal("最大游戏时间已设置为 " + maxTimeMinutes + " 分钟"));
        return 1;
    }

    public int executePlaytimeClear(CommandSourceStack source, List<ServerPlayer> targets) {
        PlayTimeKick playtimeTracker = commandRegistry.getPlayTimeKickInstance();
        if (playtimeTracker == null) {
            commandRegistry.sendErrorMessage(source, "Playtime tracker not initialized.");
            return 0;
        }
        for (ServerPlayer player : targets) {
            UUID playerId = player.getUUID();
            playtimeTracker.playerPlayTimes.remove(playerId);
            player.sendSystemMessage(Component.literal("已清除您的游戏时间"));
        }
        playtimeTracker.savePlayTimes();
        return 1;
    }

    public int executePlaytimeClearAll(CommandSourceStack source) {
        PlayTimeKick playtimeTracker = commandRegistry.getPlayTimeKickInstance();
        if (playtimeTracker == null) {
            commandRegistry.sendErrorMessage(source, "Playtime tracker not initialized.");
            return 0;
        }
        playtimeTracker.playerPlayTimes.clear();
        source.sendSuccess(() -> Component.literal("已清除所有玩家的游戏时间"), true);
        playtimeTracker.savePlayTimes();
        return 1;
    }
}