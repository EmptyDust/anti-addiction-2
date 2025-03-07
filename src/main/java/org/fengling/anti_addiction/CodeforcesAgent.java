package org.fengling.anti_addiction;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class CodeforcesAgent {

    private static final String MODEL_NAME = "Gemini-Thinking"; // Or any suitable model name
    private static final Logger LOGGER = LoggerFactory.getLogger(CodeforcesAgent.class); // Using SLF4j Logger

    private ChatAPI chatAPI;

    public CodeforcesAgent() {
        this.chatAPI = new ChatAPI();
    }

    public CompletableFuture<String> handleCodeforcesQuery(CommandSourceStack source, String userQuery) {
        CheckerAgent checker = new CheckerAgent(chatAPI);
        PlannerAgent planner = new PlannerAgent(chatAPI);
        ExecutorAgent executor = new ExecutorAgent(chatAPI);
        SummarizerAgent summarizer = new SummarizerAgent(chatAPI);

        CompletableFuture<String> resultFuture = new CompletableFuture<>();

        source.sendSystemMessage(Component.literal("Codeforces Agent: Checking if the query is Codeforces related...").withStyle(ChatFormatting.GRAY)); // Player feedback - Checker start
        checker.isCodeforcesRelated(userQuery).thenAccept(isRelated -> {
            if (!isRelated) {
                source.sendSystemMessage(Component.literal("Codeforces Agent: Query is not Codeforces related.").withStyle(ChatFormatting.GRAY)); // Player feedback - Not related
                resultFuture.complete("Sorry, this question does not seem to be related to Codeforces, I cannot answer it.");
            } else {
                source.sendSystemMessage(Component.literal("Codeforces Agent: Query is Codeforces related. Planning URL to browse...").withStyle(ChatFormatting.GRAY)); // Player feedback - Planner start
                planner.askForUrlToBrowse(userQuery).thenAccept(urlToBrowse -> { // Planner suggests URL to browse
                    source.sendSystemMessage(Component.literal("Codeforces Agent: Planned URL to browse: ").append(Component.literal(urlToBrowse).withStyle(ChatFormatting.YELLOW)).append(Component.literal(" ...").withStyle(ChatFormatting.GRAY))); // Player feedback - URL planned
                    source.sendSystemMessage(Component.literal("Codeforces Agent: Browsing URL: ").append(Component.literal(urlToBrowse).withStyle(ChatFormatting.YELLOW)).append(Component.literal(" ...").withStyle(ChatFormatting.GRAY))); // Player feedback - Executor start
                    executor.browseUrlAndGetContent(urlToBrowse).thenAccept(webpageContent -> { // Executor browses URL
                        source.sendSystemMessage(Component.literal("Codeforces Agent: Webpage content retrieved. Summarizing...").withStyle(ChatFormatting.GRAY)); // Player feedback - Summarizer start
                        summarizer.summarizeWebpageContent(userQuery, webpageContent).thenAccept(summary -> { // Summarizer summarizes webpage content
                            source.sendSystemMessage(Component.literal("Codeforces Agent: Summarization complete.").withStyle(ChatFormatting.GRAY)); // Player feedback - Summarizer end
                            resultFuture.complete(summary); // Return the summary as the final result
                        }).exceptionally(e -> {
                            LOGGER.error("Summarizer Agent Error: {}", e.getMessage(), e);
                            source.sendSystemMessage(Component.literal("Codeforces Agent: Summarizer Agent Error.").withStyle(ChatFormatting.RED)); // Player feedback - Summarizer error
                            resultFuture.completeExceptionally(e);
                            return null;
                        });
                    }).exceptionally(e -> {
                        LOGGER.error("Executor Agent Error: {}", e.getMessage(), e);
                        source.sendSystemMessage(Component.literal("Codeforces Agent: Executor Agent Error.").withStyle(ChatFormatting.RED)); // Player feedback - Executor error
                        resultFuture.completeExceptionally(e);
                        return null;
                    });
                }).exceptionally(e -> {
                    LOGGER.error("Planner Agent Error: {}", e.getMessage(), e);
                    source.sendSystemMessage(Component.literal("Codeforces Agent: Planner Agent Error.").withStyle(ChatFormatting.RED)); // Player feedback - Planner error
                    resultFuture.completeExceptionally(e);
                    return null;
                });
            }
        }).exceptionally(e -> {
            LOGGER.error("Checker Agent Error: {}", e.getMessage(), e);
            source.sendSystemMessage(Component.literal("Codeforces Agent: Checker Agent Error.").withStyle(ChatFormatting.RED)); // Player feedback - Checker error
            resultFuture.completeExceptionally(e);
            return null;
        });

        return resultFuture;
    }


    // --- Agent Classes ---

    private static class CheckerAgent {
        private ChatAPI chatAPI;

        public CheckerAgent(ChatAPI chatAPI) {
            this.chatAPI = chatAPI;
        }

        public CompletableFuture<Boolean> isCodeforcesRelated(String query) {
            String promptTemplate = "Determine if the question below is related to the Codeforces programming contest website (especially mirror.codeforces.com). Question: '%s'.  Answer **EXACTLY** with 'Yes' or 'No' **ONLY**. Do not provide any explanations or additional words.";
            String prompt = String.format(promptTemplate, escapeStringForJson(query));
            LOGGER.info("CheckerAgent Prompt: \n{}", prompt); // AI thinking - Checker prompt
            return CompletableFuture.supplyAsync(() -> {
                String response = chatAPI.getChatReply(MODEL_NAME, prompt);
                LOGGER.info("CheckerAgent Response: {}", response); // AI thinking - Checker response
                return response != null && response.trim().equalsIgnoreCase("Yes");
            });
        }
    }

    private static class PlannerAgent {
        private ChatAPI chatAPI;

        public PlannerAgent(ChatAPI chatAPI) {
            this.chatAPI = chatAPI;
        }

        public CompletableFuture<String> askForUrlToBrowse(String codeforcesQuery) { // New method for URL suggestion
            String promptTemplate = "For the Codeforces question: '%s', suggest the *most direct* source to get the answer.  Ideally, if the answer can be obtained directly and efficiently through the Codeforces API, suggest the **complete and valid Codeforces API URL** (e.g., 'https://codeforces.com/api/contest.list'). If browsing a webpage is more appropriate, then suggest a specific and relevant full URL **exclusively from mirror.codeforces.com (including https://mirror.codeforces.com/)**.  **Ensure the answer is a valid URL. Return ONLY the valid URL.** No explanation needed, and **do not suggest URLs from codeforces.com, only use mirror.codeforces.com for webpages.**";
            String prompt = String.format(promptTemplate, escapeStringForJson(codeforcesQuery));
            LOGGER.info("PlannerAgent Prompt: \n{}", prompt); // AI thinking - Planner prompt
            return CompletableFuture.supplyAsync(() -> {
                String response = chatAPI.getChatReply(MODEL_NAME, prompt);
                LOGGER.info("PlannerAgent Response: {}", response); // AI thinking - Planner response
                return response;
            });
        }
    }

    public static class ExecutorAgent { // Note: 'public static' to allow access from main for MockChatAPI

        private ChatAPI chatAPI;

        public ExecutorAgent(ChatAPI chatAPI) {
            this.chatAPI = chatAPI;
        }

        public CompletableFuture<String> browseUrlAndGetContent(String urlToBrowse) { // New method for browsing and content retrieval
            return CompletableFuture.supplyAsync(() -> {
                LOGGER.info("Executor Agent browsing URL: {}", urlToBrowse); // Player feedback already in handleCodeforcesQuery
                String webpageContent = CodeforcesAPI.getMirrorCodeforcesPageSource(urlToBrowse); // Use CodeforcesAPI to fetch webpage content from mirror site
                if (webpageContent != null) {
                    LOGGER.info("Webpage content fetched successfully (length: {} characters).", webpageContent.length());
                    return webpageContent;
                } else {
                    String errorMessage = "Failed to fetch content from URL: " + urlToBrowse;
                    LOGGER.error(errorMessage);
                    return errorMessage; // Return error message as content
                }
            });
        }
    }


    private static class SummarizerAgent { // New Summarizer Agent Class
        private ChatAPI chatAPI;

        public SummarizerAgent(ChatAPI chatAPI) {
            this.chatAPI = chatAPI;
        }

        public CompletableFuture<String> summarizeWebpageContent(String userQuery, String webpageContent) {
            String promptTemplate = "Summarize the following webpage content to answer the Codeforces question: '%s'.\\n\\nWebpage Content:\\n'%s'\\n\\nProvide a concise and direct answer based on the webpage content.";
            String prompt = String.format(promptTemplate, escapeStringForJson(userQuery), escapeStringForJson(webpageContent));
            LOGGER.info("SummarizerAgent Prompt: \n{}", prompt); // AI thinking - Summarizer prompt
            return CompletableFuture.supplyAsync(() -> {
                String response = chatAPI.getChatReply(MODEL_NAME, prompt);
                LOGGER.info("SummarizerAgent Response: {}", response); // AI thinking - Summarizer response
                return response;
            });
        }
    }


    public static void main(String[] args) {
        // Example Usage (requires a running ChatAPI and ModConfig setup as in previous examples)
        CodeforcesAgent agent = new CodeforcesAgent();

        String userQuery = "告诉我Tourist的最高段位"; // User query


//        agent.handleCodeforcesQuery((CommandSourceStack)mockSource, userQuery).thenAccept(response -> {
//            LOGGER.info("\\n--- Final Agent Response ---\\n{}", response);
//        }).exceptionally(e -> {
//            LOGGER.error("Agent processing failed: {}", e.getMessage(), e);
//            System.err.println("Agent processing failed: " + e.getMessage()); // Print to console
//            e.printStackTrace(); // Print stack trace to console
//            return null;
//        });

        // Keep main thread alive for async operations to complete (for simple main method testing)
        try {
            Thread.sleep(30000); // Wait up to 30 seconds for the agent to respond
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Helper function to escape backslashes and newlines in strings for prompts
    private static String escapeStringForJson(String str) {
        if (str == null) return "";
        return str.replace("\n", "\\n")
                .replace("\"","\\\"").substring(1,Math.min(10000, str.length()));
    }
}