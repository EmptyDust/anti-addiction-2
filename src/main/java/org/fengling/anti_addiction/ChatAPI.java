package org.fengling.anti_addiction;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatAPI {

    private static final String DEFAULT_API_ENDPOINT = "whyAreUHere.AddMeQQ.hello"; // 默认 API 地址
    private static final String API_KEY = "1422492074";

    // 存储对话上下文，可以使用 HashMap，key 可以是对话 ID (Player UUID), value 是 DialogueContext 对象
    static final Map<String, DialogueContext> dialogueContextMap = new HashMap<>();

    public static String getChatReply(String model, String prompt) {
        HttpClient client = HttpClient.newHttpClient();

        // 从 ModConfig 加载配置
        ModConfig config = ModConfig.loadConfig("config/mod_config.json"); // 配置文件路径，请确保路径正确
        String apiEndpoint = config.getServerConfig().getAiChatServerAddress();

        // 如果配置文件中没有设置 API 地址，则使用默认地址
        if (apiEndpoint == null || apiEndpoint.trim().isEmpty()) {
            apiEndpoint = DEFAULT_API_ENDPOINT;
        }

        Anti_addiction.LOGGER.info(apiEndpoint);

        // 构建请求体（JSON 格式）
        String requestBody = String.format("{\"model\": \"%s\",\"messages\": [{\"role\": \"user\", \"content\": \"%s\"}]}", model, prompt);
        Anti_addiction.LOGGER.info(requestBody);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiEndpoint)) // 使用从配置中获取的 API 地址
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        Anti_addiction.LOGGER.info(request.toString());

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString()); // 同步请求
            int statusCode = response.statusCode();
            Anti_addiction.LOGGER.info("Response code: " + statusCode);
            String responseBodyString = response.body();
            Anti_addiction.LOGGER.info("Response body: " + responseBodyString);

            if (statusCode >= 200 && statusCode < 300) {
                try {
                    Gson gson = new Gson();
                    Map<String, Object> responseJson = gson.fromJson(responseBodyString, Map.class);

                    List<Map<String, Object>> choicesArray = (List<Map<String, Object>>) responseJson.get("choices");
                    if (choicesArray != null && !choicesArray.isEmpty()) {
                        Map<String, Object> firstChoice = choicesArray.get(0);
                        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                        if (message != null) {
                            String aiReply = (String) message.get("content");
                            return aiReply != null ? aiReply.trim() : null;
                        } else {
                            Anti_addiction.LOGGER.info("API 响应中 message 字段为空");
                            return null;
                        }
                    } else {
                        Anti_addiction.LOGGER.info("API 响应中没有 choices 字段或为空");
                        return null;
                    }
                } catch (JsonParseException e) {
                    Anti_addiction.LOGGER.info("JSON 解析异常: " + e.getMessage());
                    Anti_addiction.LOGGER.error("JSON 解析异常: " + e.getMessage(),e);
                    return null;
                }
            } else {
                Anti_addiction.LOGGER.error("API call failed. Status code: " + statusCode);
                Anti_addiction.LOGGER.error("Response body: " + responseBodyString);
                return null;
            }

        } catch (IOException | InterruptedException e) {
            Anti_addiction.LOGGER.info("internet request error: " + e.getMessage());
            Anti_addiction.LOGGER.error("internet request error: {}", e.getMessage(), e);
            return null;
        }
    }

    public static String getDialogueReply(String model, String prompt, String dialogueId) {
        DialogueContext context = dialogueContextMap.computeIfAbsent(dialogueId, k -> new DialogueContext());

        context.addUserMessage(prompt); // 添加用户消息到上下文

        HttpClient client = HttpClient.newHttpClient();

        // 从 ModConfig 加载配置 (这里可以复用 getChatReply 中的配置加载逻辑，如果需要的话)
        ModConfig config = ModConfig.loadConfig("config/mod_config.json");
        String apiEndpoint = config.getServerConfig().getAiChatServerAddress();

        if (apiEndpoint == null || apiEndpoint.trim().isEmpty()) {
            apiEndpoint = DEFAULT_API_ENDPOINT;
        }
        Anti_addiction.LOGGER.info(apiEndpoint);

        // 构建包含完整对话历史的请求体
        String requestBody = buildDialogueRequestBody(model, context.getMessages());
        Anti_addiction.LOGGER.info(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiEndpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        Anti_addiction.LOGGER.info(request.toString());

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            Anti_addiction.LOGGER.info("Response code: " + statusCode);
            String responseBodyString = response.body();
            Anti_addiction.LOGGER.info("Response body: " + responseBodyString);

            if (statusCode >= 200 && statusCode < 300) {
                try {
                    Gson gson = new Gson();
                    Map<String, Object> responseJson = gson.fromJson(responseBodyString, Map.class);

                    List<Map<String, Object>> choicesArray = (List<Map<String, Object>>) responseJson.get("choices");
                    if (choicesArray != null && !choicesArray.isEmpty()) {
                        Map<String, Object> firstChoice = choicesArray.get(0);
                        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                        if (message != null) {
                            String aiReply = (String) message.get("content");
                            if (aiReply != null) {
                                String trimmedReply = aiReply.trim();
                                context.addAssistantMessage(trimmedReply); // 添加 AI 回复到上下文
                                return trimmedReply;
                            } else {
                                Anti_addiction.LOGGER.info("API 响应 message 内容为空");
                                return null;
                            }
                        } else {
                            Anti_addiction.LOGGER.info("API 响应中 message 字段为空");
                            return null;
                        }
                    } else {
                        Anti_addiction.LOGGER.info("API 响应中没有 choices 字段或为空");
                        return null;
                    }
                } catch (JsonParseException e) {
                    Anti_addiction.LOGGER.info("JSON 解析异常: " + e.getMessage());
                    Anti_addiction.LOGGER.error("JSON 解析异常: " + e.getMessage(),e);
                    return null;
                }
            } else {
                Anti_addiction.LOGGER.error("API call failed. Status code: " + statusCode);
                Anti_addiction.LOGGER.error("Response body: " + responseBodyString);
                return null;
            }

        } catch (IOException | InterruptedException e) {
            Anti_addiction.LOGGER.info("internet request error: " + e.getMessage());
            Anti_addiction.LOGGER.error("internet request error: {}", e.getMessage(), e);
            return null;
        }
    }


    private static String buildDialogueRequestBody(String model, List<Map<String, String>> messages) {
        // 使用 Gson 库更方便地构建 JSON
        Gson gson = new Gson();
        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", model);
        requestBodyMap.put("messages", messages);
        return gson.toJson(requestBodyMap);
    }


    public static void main(String[] args) {
        String modelName = "Gemini-Thinking"; // Specify the model name here

        // 简单的单次对话测试 (仍然可用)
        String userPrompt = "good morning";
        String aiReply = ChatAPI.getChatReply(modelName, userPrompt);
        if (aiReply != null) {
            Anti_addiction.LOGGER.info("AI response (Single turn): " + aiReply);
        } else {
            Anti_addiction.LOGGER.info("获取 AI 回复失败 (Single turn)");
        }

        // 多轮对话测试 - Player 1
        String player1DialogueId = "player1"; // 使用玩家ID或唯一标识符作为 dialogueId
        String replyP1_1 = ChatAPI.getDialogueReply(modelName, "Hello, how are you?", player1DialogueId);
        if (replyP1_1 != null) {
            Anti_addiction.LOGGER.info("Player 1 AI (Round 1): " + replyP1_1);
        } else {
            Anti_addiction.LOGGER.info("获取 Player 1 AI 回复失败 (Round 1)");
        }

        String replyP1_2 = ChatAPI.getDialogueReply(modelName, "Tell me a joke.", player1DialogueId); // 同一个 dialogueId，继续对话 Player 1
        if (replyP1_2 != null) {
            Anti_addiction.LOGGER.info("Player 1 AI (Round 2): " + replyP1_2);
        } else {
            Anti_addiction.LOGGER.info("获取 Player 1 AI 回复失败 (Round 2)");
        }

        // 多轮对话测试 - Player 2 (不同的 dialogueId)
        String player2DialogueId = "player2"; // 为 Player 2 使用不同的 dialogueId
        String replyP2_1 = ChatAPI.getDialogueReply(modelName, "Hi AI, what's the weather like today?", player2DialogueId);
        if (replyP2_1 != null) {
            Anti_addiction.LOGGER.info("Player 2 AI (Round 1): " + replyP2_1);
        } else {
            Anti_addiction.LOGGER.info("获取 Player 2 AI 回复失败 (Round 1)");
        }

        String replyP2_2 = ChatAPI.getDialogueReply(modelName, "Thanks!", player2DialogueId); // 继续对话 Player 2，使用 player2DialogueId
        if (replyP2_2 != null) {
            Anti_addiction.LOGGER.info("Player 2 AI (Round 2): " + replyP2_2);
        } else {
            Anti_addiction.LOGGER.info("获取 Player 2 AI 回复失败 (Round 2)");
        }

        // 再次 Player 1 对话，验证上下文隔离
        String replyP1_3 = ChatAPI.getDialogueReply(modelName, "Thanks!", player1DialogueId); // 继续 Player 1 对话
        if (replyP1_3 != null) {
            Anti_addiction.LOGGER.info("Player 1 AI (Round 3): " + replyP1_3);
        } else {
            Anti_addiction.LOGGER.info("获取 Player 1 AI 回复失败 (Round 3)");
        }
    }
}


// DialogueContext 类保持不变
class DialogueContext {
    private List<Map<String, String>> messages;

    public DialogueContext() {
        this.messages = new ArrayList<>();
    }

    public List<Map<String, String>> getMessages() {
        return messages;
    }

    public void addUserMessage(String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);
        this.messages.add(message);
    }

    public void addAssistantMessage(String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", "assistant");
        message.put("content", content);
        this.messages.add(message);
    }

    public void clearMessages() {
        this.messages.clear();
    }
}