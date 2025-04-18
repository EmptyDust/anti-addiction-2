package org.fengling.anti_addiction;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

public class ChatCommandHandler {

    private final CommandRegistry commandRegistry; // Reference back to CommandRegistry for helper methods

    public ChatCommandHandler(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    public int executeChatConfigSet(CommandSourceStack source, String url) {
        ModConfig config = ModConfig.loadConfig("config/mod_config.json");
        config.getServerConfig().setAiChatServerAddress(url);
        config.saveConfig("config/mod_config.json");

        source.sendSystemMessage(Component.literal("Chat API Server URL set to: " + url).withStyle(ChatFormatting.GREEN));
        Anti_addiction.LOGGER.info("Chat API Server URL set to: {}", url);
        return 1;
    }
    
    public int executeChatConfigSetApiKey(CommandSourceStack source, String apiKey) {
        ModConfig config = ModConfig.loadConfig("config/mod_config.json");
        config.getServerConfig().setApiKey(apiKey);
        config.saveConfig("config/mod_config.json");

        source.sendSystemMessage(Component.literal("Chat API Key set successfully").withStyle(ChatFormatting.GREEN));
        Anti_addiction.LOGGER.info("Chat API Key set successfully");
        return 1;
    }

    public int executeAiQuery(CommandSourceStack source, String content, final String modelName, boolean useDialogue) {
        if (content == null || content.trim().isEmpty()) {
            commandRegistry.sendErrorMessage(source, "Content cannot be empty.");
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

        CompletableFuture<String> aiReplyFuture;
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            commandRegistry.sendErrorMessage(source, "This command can only be executed by a player.");
            return 0;
        }

        String playerDialogueId = player.getUUID().toString();

        if (useDialogue) {
            aiReplyFuture = CompletableFuture.supplyAsync(() -> ChatAPI.getDialogueReply(modelName, content, playerDialogueId));
        } else {
            aiReplyFuture = CompletableFuture.supplyAsync(() -> ChatAPI.getChatReply(modelName, content));
        }

        aiReplyFuture.thenAccept(aiReply -> {
            server.execute(() -> {
                if (aiReply != null) {
                    MutableComponent replyComponent = Component.literal(modelName.replace("-Thinking", "") + " Reply: ").withStyle(ChatFormatting.GREEN).append(Component.literal(aiReply).withStyle(ChatFormatting.WHITE));
                    source.sendSystemMessage(replyComponent);
                } else {
                    commandRegistry.sendErrorMessage(source, "Failed to get " + modelName.replace("-Thinking", "") + " reply.  Check the server logs.");
                }
            });
        }).exceptionally(e -> {
            server.execute(() -> {
                commandRegistry.sendErrorMessage(source, "Failed to get " + modelName.replace("-Thinking", "") + " reply, Error: " + e.getMessage());
                Anti_addiction.LOGGER.error("Failed to get {} reply", modelName, e);
            });
            return null;
        });

        return 1;
    }

    public int executeChatClear(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            commandRegistry.sendErrorMessage(source, "This command can only be executed by a player.");
            return 0;
        }
        ServerPlayer player = (ServerPlayer) source.getEntity();
        String playerDialogueId = player.getUUID().toString();

        if (ChatAPI.dialogueContextMap.containsKey(playerDialogueId)) {
            ChatAPI.dialogueContextMap.remove(playerDialogueId);
            source.sendSystemMessage(Component.literal("Chat history cleared.").withStyle(ChatFormatting.GREEN));
            Anti_addiction.LOGGER.info("Chat history cleared for player: {}", player.getName().getString());
        } else {
            source.sendSystemMessage(Component.literal("No chat history to clear.").withStyle(ChatFormatting.YELLOW));
        }
        return 1;
    }
    
    // --- Proxy Command Methods ---
    public int executeChatProxyEnable(CommandSourceStack source, boolean enable) {
        // Load the current config
        ModConfig config = ModConfig.loadConfig("config/mod_config.json");
        
        // Set the proxy enabled status
        config.getServerConfig().setUseProxy(enable);
        
        // Save the config
        config.saveConfig("config/mod_config.json");
        
        source.sendSuccess(() -> Component.literal("Proxy " + (enable ? "enabled" : "disabled")), true);
        return 1;
    }
    
    public int executeChatProxyHost(CommandSourceStack source, String host) {
        // Load the current config
        ModConfig config = ModConfig.loadConfig("config/mod_config.json");
        
        // Set the proxy host
        config.getServerConfig().setProxyHost(host);
        
        // Save the config
        config.saveConfig("config/mod_config.json");
        
        source.sendSuccess(() -> Component.literal("Proxy host set to: " + host), true);
        return 1;
    }
    
    public int executeChatProxyPort(CommandSourceStack source, int port) {
        // Load the current config
        ModConfig config = ModConfig.loadConfig("config/mod_config.json");
        
        // Set the proxy port
        config.getServerConfig().setProxyPort(port);
        
        // Save the config
        config.saveConfig("config/mod_config.json");
        
        source.sendSuccess(() -> Component.literal("Proxy port set to: " + port), true);
        return 1;
    }
    
    public int executeChatProxyStatus(CommandSourceStack source) {
        // Load the current config
        ModConfig config = ModConfig.loadConfig("config/mod_config.json");
        
        // Get proxy settings
        boolean enabled = config.getServerConfig().isUseProxy();
        String host = config.getServerConfig().getProxyHost();
        int port = config.getServerConfig().getProxyPort();
        
        // Send status message
        source.sendSuccess(() -> Component.literal("Proxy status:"), false);
        source.sendSuccess(() -> Component.literal("  Enabled: " + enabled), false);
        source.sendSuccess(() -> Component.literal("  Host: " + (host.isEmpty() ? "[Not Set]" : host)), false);
        source.sendSuccess(() -> Component.literal("  Port: " + port), false);
        
        return 1;
    }
}