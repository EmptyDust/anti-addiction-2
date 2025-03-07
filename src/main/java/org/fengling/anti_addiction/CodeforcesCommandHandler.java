package org.fengling.anti_addiction;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CodeforcesCommandHandler {

    private final CommandRegistry commandRegistry; // Reference back to CommandRegistry for helper methods
    private final CodeforcesAgent codeforcesAgent; // Instance of CodeforcesAgent

    public CodeforcesCommandHandler(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
        this.codeforcesAgent = new CodeforcesAgent(); // Initialize CodeforcesAgent in the handler
    }

    public int executeCodeforcesInfo(CommandSourceStack source, String handle) {
        Anti_addiction.LOGGER.info("Attempt to get {}'s info.", handle);

        CompletableFuture.supplyAsync(() -> CodeforcesAPI.getUserInfoJson(handle))
                .thenApply(userInfoJson -> {
                    if (userInfoJson != null) {
                        try {
                            return CodeforcesAPI.parseUserInfoJson(userInfoJson);
                        } catch (Exception e) {
                            Anti_addiction.LOGGER.error("JSON Parsing exception", e);
                            return null;
                        }
                    } else {
                        return null;
                    }
                })
                .thenAccept(response -> {
                    MinecraftServer server = source.getServer();
                    server.execute(() -> {
                        sendFormattedCodeforcesInfo(source, response);
                    });
                })
                .exceptionally(e -> {
                    MinecraftServer server = source.getServer();
                    server.execute(() -> {
                        commandRegistry.sendErrorMessage(source, "Error: " + e.getMessage());
                        Anti_addiction.LOGGER.error("Error during async task: {}", e.getMessage());
                    });
                    return null;
                });

        return 1;
    }

    private void sendFormattedCodeforcesInfo(CommandSourceStack source, CodeforcesAPI.CodeforcesResponse response) {
        if (response instanceof CodeforcesAPI.CodeforcesResponse && "OK".equals(response.status)) {
            List<CodeforcesAPI.UserInfo> results = response.result;

            if (results != null && !results.isEmpty()) {
                CodeforcesAPI.UserInfo user = results.get(0);

                MutableComponent topBorder = Component.literal("----------------------------------------").withStyle(ChatFormatting.GRAY);
                MutableComponent handleComponent = Component.literal("Handle: ").withStyle(ChatFormatting.GREEN).append(Component.literal(user.handle).withStyle(ChatFormatting.YELLOW));
                MutableComponent ratingComponent = Component.literal("Rating: ").withStyle(ChatFormatting.GREEN).append(Component.literal(String.valueOf(user.rating)).withStyle(ChatFormatting.AQUA));
                MutableComponent rankComponent = Component.literal("Rank: ").withStyle(ChatFormatting.GREEN).append(Component.literal(user.rank).withStyle(ChatFormatting.GOLD));
                MutableComponent maxRatingComponent = Component.literal("Max Rating: ").withStyle(ChatFormatting.GREEN).append(Component.literal(String.valueOf(user.maxRating)).withStyle(ChatFormatting.RED));
                MutableComponent bottomBorder = Component.literal("----------------------------------------").withStyle(ChatFormatting.GRAY);

                source.sendSystemMessage(topBorder);
                source.sendSystemMessage(handleComponent);
                source.sendSystemMessage(ratingComponent);
                source.sendSystemMessage(rankComponent);
                source.sendSystemMessage(maxRatingComponent);
                source.sendSystemMessage(bottomBorder);
            } else {
                commandRegistry.sendErrorMessage(source, "No user data found.");
                Anti_addiction.LOGGER.info("Fail3.");
            }
        } else {
            String status = (response != null) ? response.status : "Unknown";
            commandRegistry.sendErrorMessage(source, "Codeforces API Error (Status): " + status);
            Anti_addiction.LOGGER.info("Fail2.");
        }
    }

    public int executeCodeforcesContest(CommandSourceStack source) {
        Anti_addiction.LOGGER.info("Attempt to get recent Codeforces contests.");

        CompletableFuture.supplyAsync(CodeforcesAPI::getRecentContestsJson)
                .thenApply(contestListJson -> {
                    if (contestListJson != null) {
                        try {
                            return CodeforcesAPI.parseContestListJson(contestListJson);
                        } catch (Exception e) {
                            Anti_addiction.LOGGER.error("JSON Parsing exception for contest list", e);
                            return null;
                        }
                    } else {
                        return null;
                    }
                })
                .thenAccept(contestResponse -> {
                    MinecraftServer server = source.getServer();
                    server.execute(() -> {
                        sendFormattedCodeforcesContestInfo(source, contestResponse);
                    });
                })
                .exceptionally(e -> {
                    MinecraftServer server = source.getServer();
                    server.execute(() -> {
                        commandRegistry.sendErrorMessage(source, "Error fetching contest list: " + e.getMessage());
                        Anti_addiction.LOGGER.error("Error during async contest task: {}", e.getMessage());
                    });
                    return null;
                });

        return 1;
    }

    private void sendFormattedCodeforcesContestInfo(CommandSourceStack source, CodeforcesAPI.ContestListResponse contestResponse) {
        if (contestResponse instanceof CodeforcesAPI.ContestListResponse && "OK".equals(contestResponse.status)) {
            List<CodeforcesAPI.ContestInfo> contests = contestResponse.result;

            if (contests != null && !contests.isEmpty()) {
                MutableComponent topBorder = Component.literal("------------------ Recent Contests ------------------").withStyle(ChatFormatting.GRAY);
                MutableComponent bottomBorder = Component.literal("---------------------------------------------------").withStyle(ChatFormatting.GRAY);
                source.sendSystemMessage(topBorder);

                long currentTimeSeconds = System.currentTimeMillis() / 1000;
                long threeDaysAgoSeconds = currentTimeSeconds - (3 * 24 * 60 * 60);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

                for (CodeforcesAPI.ContestInfo contest : contests) {
                    if (contest.phase.equals("BEFORE") || (contest.startTimeSeconds >= threeDaysAgoSeconds && !contest.phase.equals("BEFORE"))) {
                        LocalDateTime startTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(contest.startTimeSeconds), ZoneId.systemDefault());
                        String formattedStartTime = formatter.format(startTime);
                        String duration = String.format("%d hours %d minutes", contest.durationSeconds / 3600, (contest.durationSeconds % 3600) / 60);

                        MutableComponent contestInfo = Component.literal("").append(Component.literal("Name: ").withStyle(ChatFormatting.GREEN).append(Component.literal(contest.name).withStyle(ChatFormatting.YELLOW)))
                                .append(Component.literal(" | Type: ").withStyle(ChatFormatting.GREEN).append(Component.literal(contest.type).withStyle(ChatFormatting.AQUA)))
                                .append(Component.literal(" | Start: ").withStyle(ChatFormatting.GREEN).append(Component.literal(formattedStartTime).withStyle(ChatFormatting.GOLD)))
                                .append(Component.literal(" | Duration: ").withStyle(ChatFormatting.GREEN).append(Component.literal(duration).withStyle(ChatFormatting.RED)));
                        source.sendSystemMessage(contestInfo);
                    }
                }
                source.sendSystemMessage(bottomBorder);


            } else {
                commandRegistry.sendErrorMessage(source, "No contest data found.");
                Anti_addiction.LOGGER.info("No contest data received from API.");
            }
        } else {
            String status = (contestResponse != null) ? contestResponse.status : "Unknown";
            commandRegistry.sendErrorMessage(source, "Codeforces Contest API Error (Status): " + status);
            Anti_addiction.LOGGER.info("Codeforces Contest API Error, status: {}", status);
        }
    }

    public int executeCodeforcesBond(CommandSourceStack source, String cfId) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            commandRegistry.sendErrorMessage(source, "This command can only be executed by a player.");
            return 0;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        UUID playerUUID = player.getUUID();
        String playerName = player.getName().getString();

        ModConfig config = ModConfig.loadConfig("config/mod_config.json");
        List<ModConfig.PlayerConfig> playerConfigs = config.getPlayerConfigs();
        ModConfig.PlayerConfig playerConfig = null;

        for (ModConfig.PlayerConfig pc : playerConfigs) {
            if (pc.getPlayerUuid().equals(playerUUID)) {
                playerConfig = pc;
                break;
            }
        }

        if (playerConfig == null) {
            playerConfig = new ModConfig.PlayerConfig(playerUUID, playerName, 0, null);
            playerConfigs.add(playerConfig);
        }

        playerConfig.setCodeforcesID(cfId);
        config.saveConfig("config/mod_config.json");

        source.sendSystemMessage(Component.literal("Successfully bonded your Codeforces ID to: " + cfId).withStyle(ChatFormatting.GREEN));
        Anti_addiction.LOGGER.info("Player {} bonded Codeforces ID: {}", playerName, cfId);
        return 1;
    }

    public int executeCodeforcesQuery(CommandSourceStack source, String query) {
        source.sendSystemMessage(Component.literal("Codeforces Query: ").append(Component.literal(query).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY)); // Player feedback - query start

        codeforcesAgent.handleCodeforcesQuery(source, query).thenAccept(response -> {
            MinecraftServer server = source.getServer();
            server.execute(() -> {
                source.sendSystemMessage(Component.literal("Codeforces Agent Response:").withStyle(ChatFormatting.GRAY));
                source.sendSystemMessage(Component.literal(response).withStyle(ChatFormatting.WHITE));
            });
        }).exceptionally(e -> {
            MinecraftServer server = source.getServer();
            server.execute(() -> {
                commandRegistry.sendErrorMessage(source, "Codeforces Agent Query Error: " + e.getMessage());
                Anti_addiction.LOGGER.error("Codeforces Agent Query Error: {}", e.getMessage());
            });
            return null;
        });
        return 1;
    }
}