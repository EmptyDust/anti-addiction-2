package org.fengling.anti_addiction;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;


public class CommandRegistry {

    private PlaytimeCommandHandler playtimeCommandHandler;
    private CodeforcesCommandHandler codeforcesCommandHandler;
    private ChatCommandHandler chatCommandHandler;

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        playtimeCommandHandler = new PlaytimeCommandHandler(this);
        codeforcesCommandHandler = new CodeforcesCommandHandler(this);
        chatCommandHandler = new ChatCommandHandler(this);

        // Root command /aa and alias /antiaddiction (renamed from anti_addiction)
        LiteralArgumentBuilder<CommandSourceStack> antiaddictionCommand = Commands.literal("antiaddiction");
        LiteralArgumentBuilder<CommandSourceStack> aaCommand = Commands.literal("aa");

        // Playtime commands under /aa playtime
        LiteralArgumentBuilder<CommandSourceStack> playtimeCommand = Commands.literal("playtime");

        // /aa playtime set <targets> <playtime>
        playtimeCommand.then(Commands.literal("set")
                .requires(source -> source.hasPermission(3)) // Admin permission
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("playtime", IntegerArgumentType.integer(0))
                                .executes(ctx -> playtimeCommandHandler.executePlaytimeSet(ctx.getSource(), (List<ServerPlayer>) EntityArgument.getPlayers(ctx, "targets"), IntegerArgumentType.getInteger(ctx, "playtime")))
                        )
                )
        );

        // /aa playtime set_max_time <max_time>
        playtimeCommand.then(Commands.literal("set_max_time")
                .requires(source -> source.hasPermission(4)) // OP only
                .then(Commands.argument("max_time", IntegerArgumentType.integer(0))
                        .executes(ctx -> playtimeCommandHandler.executePlaytimeSetMaxTime(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "max_time")))
                )
        );

        // /aa playtime clear <targets>
        playtimeCommand.then(Commands.literal("clear")
                .requires(source -> source.hasPermission(3)) // Admin permission
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(ctx -> playtimeCommandHandler.executePlaytimeClear(ctx.getSource(), (List<ServerPlayer>) EntityArgument.getPlayers(ctx, "targets")))
                )
        );

        // /aa playtime clearall
        playtimeCommand.then(Commands.literal("clearall")
                .requires(source -> source.hasPermission(4)) // OP only
                .executes(ctx -> playtimeCommandHandler.executePlaytimeClearAll(ctx.getSource()))
        );

        aaCommand.then(playtimeCommand); // Nest playtime commands under /aa
        antiaddictionCommand.redirect(aaCommand.build()); // Alias /antiaddiction to /aa

        event.getDispatcher().register(aaCommand);
        event.getDispatcher().register(antiaddictionCommand);


        // Create /codeforces command (remains as /cf for short)
        LiteralArgumentBuilder<CommandSourceStack> codeforcesCommand = Commands.literal("codeforces");

        // Add /codeforces info subcommand
        codeforcesCommand.then(Commands.literal("info")
                .then(Commands.argument("username", StringArgumentType.string())
                        .executes(context -> codeforcesCommandHandler.executeCodeforcesInfo(context.getSource(), StringArgumentType.getString(context, "username")))));

        // Add /codeforces contest subcommand
        codeforcesCommand.then(Commands.literal("contest")
                .executes(context -> codeforcesCommandHandler.executeCodeforcesContest(context.getSource())));

        // Add /codeforces bond subcommand
        codeforcesCommand.then(Commands.literal("bond")
                .then(Commands.argument("id", StringArgumentType.string())
                        .executes(context -> codeforcesCommandHandler.executeCodeforcesBond(context.getSource(), StringArgumentType.getString(context, "id")))));

        // Add /codeforces query subcommand
        codeforcesCommand.then(Commands.literal("query")
                .then(Commands.argument("content", StringArgumentType.string())
                        .executes(context -> codeforcesCommandHandler.executeCodeforcesQuery(context.getSource(), StringArgumentType.getString(context, "content")))));


        // Register /codeforces command and shorthand /cf
        event.getDispatcher().register(codeforcesCommand);
        event.getDispatcher().register(
                Commands.literal("cf").redirect(codeforcesCommand.build()) // Shorthand /cf
        );

        // Create /chat command
        LiteralArgumentBuilder<CommandSourceStack> chatCommand = Commands.literal("chat");

        // Modify /chat gemini subcommand
        chatCommand.then(Commands.literal("gemini")
                .then(Commands.argument("content", StringArgumentType.string())
                        .executes(context -> chatCommandHandler.executeAiQuery(context.getSource(), StringArgumentType.getString(context, "content"), "Gemini-Thinking", false)) // Case 1: /chat gemini <content> (no useDialogue argument)
                        .then(Commands.argument("useDialogue", BoolArgumentType.bool())
                                .executes(context -> chatCommandHandler.executeAiQuery(context.getSource(), StringArgumentType.getString(context, "content"), "Gemini-Thinking", BoolArgumentType.getBool(context, "useDialogue"))) // Case 2: /chat gemini <content> <useDialogue>
                        )
                )
        );

        // Add /chat config subcommand with renamed set_url
        chatCommand.then(Commands.literal("config")
                .requires(source -> source.hasPermission(4)) // Requires OP permission
                .then(Commands.literal("set_url") // Renamed from "set"
                        .then(Commands.argument("url", StringArgumentType.string())
                                .executes(context -> chatCommandHandler.executeChatConfigSet(context.getSource(), StringArgumentType.getString(context, "url")))))
                .then(Commands.literal("set_key")
                        .then(Commands.argument("apiKey", StringArgumentType.string())
                                .executes(context -> chatCommandHandler.executeChatConfigSetApiKey(context.getSource(), StringArgumentType.getString(context, "apiKey"))))));
                
        // Add /chat proxy subcommand (moved from ProxyCommands)
        chatCommand.then(Commands.literal("proxy")
                .requires(source -> source.hasPermission(3)) // Admin permission
                .then(Commands.literal("enable")
                    .then(Commands.argument("enable", BoolArgumentType.bool())
                        .executes(context -> chatCommandHandler.executeChatProxyEnable(context.getSource(), BoolArgumentType.getBool(context, "enable"))))
                )
                .then(Commands.literal("host")
                    .then(Commands.argument("host", StringArgumentType.string())
                        .executes(context -> chatCommandHandler.executeChatProxyHost(context.getSource(), StringArgumentType.getString(context, "host"))))
                )
                .then(Commands.literal("port")
                    .then(Commands.argument("port", IntegerArgumentType.integer(1, 65535))
                        .executes(context -> chatCommandHandler.executeChatProxyPort(context.getSource(), IntegerArgumentType.getInteger(context, "port"))))
                )
                .then(Commands.literal("status")
                    .executes(context -> chatCommandHandler.executeChatProxyStatus(context.getSource())))
        );

        // Add /chat clear subcommand
        chatCommand.then(Commands.literal("clear")
                .executes(context -> chatCommandHandler.executeChatClear(context.getSource())));

        // Register /chat command
        event.getDispatcher().register(chatCommand);
    }

    // --- Helper Methods (Keep in CommandRegistry) ---
    public void sendErrorMessage(CommandSourceStack source, String message) {
        source.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    // Placeholder to get PlayTimeKick instance, you need to implement this based on your mod's structure
    public PlayTimeKick getPlayTimeKickInstance() {
        // This is a placeholder, replace with your actual logic to get the PlayTimeKick instance
        // For example, if PlayTimeKick is a static instance in your main mod class:
        // return AntiAddictionMod.playTimeKickInstance;
        return Anti_addiction.playTimeKick;
    }
}