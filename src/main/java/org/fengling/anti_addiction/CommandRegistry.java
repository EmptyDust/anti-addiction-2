package org.fengling.anti_addiction;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CommandRegistry {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // Root command /aa and alias /anti_addiction
        LiteralArgumentBuilder<CommandSourceStack> antiAddictionCommand = Commands.literal("anti_addiction");
        LiteralArgumentBuilder<CommandSourceStack> aaCommand = Commands.literal("aa");

        // Playtime commands under /aa playtime
        LiteralArgumentBuilder<CommandSourceStack> playtimeCommand = Commands.literal("playtime");

        // /aa playtime set <targets> <playtime>
        playtimeCommand.then(Commands.literal("set")
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("playtime", IntegerArgumentType.integer(0))
                                .executes(ctx -> executePlaytimeSet(ctx.getSource(), (List<ServerPlayer>) EntityArgument.getPlayers(ctx, "targets"), IntegerArgumentType.getInteger(ctx, "playtime")))
                        )
                )
        );

        // /aa playtime set_max_time <max_time>
        playtimeCommand.then(Commands.literal("set_max_time")
                .requires(source -> source.hasPermission(4)) // OP only
                .then(Commands.argument("max_time", IntegerArgumentType.integer(0))
                        .executes(ctx -> executePlaytimeSetMaxTime(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "max_time")))
                )
        );

        // /aa playtime clear <targets>
        playtimeCommand.then(Commands.literal("clear")
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(ctx -> executePlaytimeClear(ctx.getSource(), (List<ServerPlayer>) EntityArgument.getPlayers(ctx, "targets")))
                )
        );

        // /aa playtime clearall
        playtimeCommand.then(Commands.literal("clearall")
                .requires(source -> source.hasPermission(4)) // OP only
                .executes(ctx -> executePlaytimeClearAll(ctx.getSource()))
        );

        aaCommand.then(playtimeCommand); // Nest playtime commands under /aa
        antiAddictionCommand.redirect(aaCommand.build()); // Alias /anti_addiction to /aa

        event.getDispatcher().register(aaCommand);
        event.getDispatcher().register(antiAddictionCommand);


        // Create /codeforces command (remains as /cf for short)
        LiteralArgumentBuilder<CommandSourceStack> codeforcesCommand = Commands.literal("codeforces");

        // Add /codeforces info subcommand
        codeforcesCommand.then(Commands.literal("info")
                .then(Commands.argument("username", StringArgumentType.string())
                        .executes(context -> executeCodeforcesInfo(context.getSource(), StringArgumentType.getString(context, "username")))));

        // Add /codeforces contest subcommand
        codeforcesCommand.then(Commands.literal("contest")
                .executes(context -> executeCodeforcesContest(context.getSource())));

        // Add /codeforces bond subcommand
        codeforcesCommand.then(Commands.literal("bond")
                .then(Commands.argument("id", StringArgumentType.string())
                        .executes(context -> executeCodeforcesBond(context.getSource(), StringArgumentType.getString(context, "id")))));

        // Register /codeforces command and shorthand /cf
        event.getDispatcher().register(codeforcesCommand);
        event.getDispatcher().register(
                Commands.literal("cf").redirect(codeforcesCommand.build()) // Shorthand /cf
        );

        // Create /chat command
        LiteralArgumentBuilder<CommandSourceStack> chatCommand = Commands.literal("chat");

        // Add /chat grok subcommand (now repurposed for Gemini with deprecation message)
        chatCommand.then(Commands.literal("grok")
                .then(Commands.argument("content", StringArgumentType.string())
                        .executes(context -> executeAiQuery(context.getSource(), StringArgumentType.getString(context, "content"), "Grok-2")) // Still uses "Grok-2" as model name for now, handled in executeAiQuery
                )
        );

        // Add /chat gemini subcommand
        chatCommand.then(Commands.literal("gemini")
                .then(Commands.argument("content", StringArgumentType.string())
                        .executes(context -> executeAiQuery(context.getSource(), StringArgumentType.getString(context, "content"), "Gemini-Thinking"))
                )
        );

        // Add /chat config subcommand
        chatCommand.then(Commands.literal("config")
                .requires(source -> source.hasPermission(4)) // Requires OP permission
                .then(Commands.literal("set")
                        .then(Commands.argument("url", StringArgumentType.string())
                                .executes(context -> executeChatConfigSet(context.getSource(), StringArgumentType.getString(context, "url"))))));

        // Register /chat command
        event.getDispatcher().register(chatCommand);
    }


    // --- Playtime Command Execution ---

    private int executePlaytimeSet(CommandSourceStack source, List<ServerPlayer> targets, int playtimeMinutes) {
        ModConfig config = ModConfig.loadConfig("config/mod_config.json");
        PlayTimeKick playtimeTracker = getPlayTimeKickInstance(); // Assuming you have a way to access PlayTimeKick instance
        if (playtimeTracker == null) {
            sendErrorMessage(source, "Playtime tracker not initialized.");
            return 0;
        }
        for (ServerPlayer player : targets) {
            UUID playerId = player.getUUID();
            playtimeTracker.playerPlayTimes.put(playerId, playtimeMinutes * 60 * 20);
            player.sendSystemMessage(Component.literal("已将您的游戏时间设置为 " + playtimeMinutes + " 分钟"));
        }
        playtimeTracker.savePlayTimes(); // Save after command execution
        return 1;
    }

    private int executePlaytimeSetMaxTime(CommandSourceStack source, int maxTimeMinutes) {
        ModConfig config = ModConfig.loadConfig("config/mod_config.json");
        config.getServerConfig().setMaxPlayTimeMinutes(maxTimeMinutes);
        config.saveConfig("config/mod_config.json");
        source.sendSystemMessage(Component.literal("最大游戏时间已设置为 " + maxTimeMinutes + " 分钟"));
        return 1;
    }

    private int executePlaytimeClear(CommandSourceStack source, List<ServerPlayer> targets) {
        PlayTimeKick playtimeTracker = getPlayTimeKickInstance(); // Assuming you have a way to access PlayTimeKick instance
        if (playtimeTracker == null) {
            sendErrorMessage(source, "Playtime tracker not initialized.");
            return 0;
        }
        for (ServerPlayer player : targets) {
            UUID playerId = player.getUUID();
            playtimeTracker.playerPlayTimes.remove(playerId);
            player.sendSystemMessage(Component.literal("已清除您的游戏时间"));
        }
        playtimeTracker.savePlayTimes(); // Save after command execution
        return 1;
    }

    private int executePlaytimeClearAll(CommandSourceStack source) {
        PlayTimeKick playtimeTracker = getPlayTimeKickInstance(); // Assuming you have a way to access PlayTimeKick instance
        if (playtimeTracker == null) {
            sendErrorMessage(source, "Playtime tracker not initialized.");
            return 0;
        }
        playtimeTracker.playerPlayTimes.clear();
        source.sendSuccess(() -> Component.literal("已清除所有玩家的游戏时间"), true);
        playtimeTracker.savePlayTimes(); // Save after command execution
        return 1;
    }


    // --- Codeforces Command Execution ---

    private int executeCodeforcesInfo(CommandSourceStack source, String handle) {
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
                        sendErrorMessage(source, "Error: " + e.getMessage());
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
                sendErrorMessage(source, "No user data found.");
                Anti_addiction.LOGGER.info("Fail3.");
            }
        } else {
            String status = (response != null) ? response.status : "Unknown";
            sendErrorMessage(source, "Codeforces API Error (Status): " + status);
            Anti_addiction.LOGGER.info("Fail2.");
        }
    }

    private int executeCodeforcesContest(CommandSourceStack source) {
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
                        sendErrorMessage(source, "Error fetching contest list: " + e.getMessage());
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
                sendErrorMessage(source, "No contest data found.");
                Anti_addiction.LOGGER.info("No contest data received from API.");
            }
        } else {
            String status = (contestResponse != null) ? contestResponse.status : "Unknown";
            sendErrorMessage(source, "Codeforces Contest API Error (Status): " + status);
            Anti_addiction.LOGGER.info("Codeforces Contest API Error, status: {}", status);
        }
    }

    private int executeCodeforcesBond(CommandSourceStack source, String cfId) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            sendErrorMessage(source, "This command can only be executed by a player.");
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


    // --- Chat Command Execution ---

    private int executeChatConfigSet(CommandSourceStack source, String url) {
        ModConfig config = ModConfig.loadConfig("config/mod_config.json");
        config.getServerConfig().setAiChatServerAddress(url);
        config.saveConfig("config/mod_config.json");

        source.sendSystemMessage(Component.literal("Chat API Server URL set to: " + url).withStyle(ChatFormatting.GREEN));
        Anti_addiction.LOGGER.info("Chat API Server URL set to: {}", url);
        return 1;
    }

    private int executeAiQuery(CommandSourceStack source, String content,final String modelName) {
        if (content == null || content.trim().isEmpty()) {
            sendErrorMessage(source, "Content cannot be empty.");
            return 0;
        }

        MinecraftServer server = source.getServer();
        MutableComponent inputComponent = Component.literal("You queried: ").withStyle(ChatFormatting.BLUE).append(Component.literal(content).withStyle(ChatFormatting.WHITE));
        source.sendSystemMessage(inputComponent);

        if (modelName.equalsIgnoreCase("Grok-2")) {
            MutableComponent deprecationMessage = Component.literal("Warning: Grok-2 support is deprecated and may be removed in future versions.").withStyle(ChatFormatting.YELLOW);
            source.sendSystemMessage(deprecationMessage);
            return 0;
        }

        CompletableFuture<String> aiReplyFuture = CompletableFuture.supplyAsync(() -> ChatAPI.getChatReply(modelName, content)); // Asynchronous call

        aiReplyFuture.thenAccept(aiReply -> {
            server.execute(() -> { // Switch back to the main server thread for UI updates
                if (aiReply != null) {
                    MutableComponent replyComponent = Component.literal(modelName.replace("-Thinking", "") + " Reply: ").withStyle(ChatFormatting.GREEN).append(Component.literal(aiReply).withStyle(ChatFormatting.WHITE));
                    source.sendSystemMessage(replyComponent);
                } else {
                    sendErrorMessage(source, "Failed to get " + modelName.replace("-Thinking", "") + " reply.  Check the server logs.");
                }
            });
        }).exceptionally(e -> {
            server.execute(() -> { // Switch back to the main server thread for error handling
                sendErrorMessage(source, "Failed to get " + modelName.replace("-Thinking", "") + " reply, Error: " + e.getMessage());
                Anti_addiction.LOGGER.error("Failed to get {} reply", modelName, e);
            });
            return null; // Return null to complete the CompletableFuture chain
        });

        return 1; // Command execution started, but reply is pending (asynchronous)
    }


    // --- Helper Methods ---
    private void sendErrorMessage(CommandSourceStack source, String message) {
        source.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    // Placeholder to get PlayTimeKick instance, you need to implement this based on your mod's structure
    private PlayTimeKick getPlayTimeKickInstance() {
        // This is a placeholder, replace with your actual logic to get the PlayTimeKick instance
        // For example, if PlayTimeKick is a static instance in your main mod class:
        // return AntiAddictionMod.playTimeKickInstance;
        return null; // Replace null with actual instance retrieval
    }
}