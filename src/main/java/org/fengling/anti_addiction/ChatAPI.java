package org.fengling.anti_addiction;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class ChatAPI {

    private static final String DEFAULT_API_ENDPOINT = "whyAreUHere.AddMeQQ.hello"; // 默认 API 地址
    private static final String API_KEY = "1422492074";

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
                    Map<String, Object> responseJson = (Map<String, Object>) GenericJsonParser.parse(responseBodyString);

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
                } catch (IOException e) {
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
//        HttpClient client = HttpClient.newHttpClient();
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create("https://codeforces.com/api/user.info?handles=Empty_Dust"))
//                .build();
//
//        try {
//            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//            Anti_addiction.LOGGER.info(response.body());
//            return response.body();
//        } catch (IOException | InterruptedException e) {
//            Anti_addiction.LOGGER.error("Error fetching Codeforces contest list: {}", e.getMessage());
//            return null;
//        }
    }

    public static void main(String[] args) {
        String userPrompt = "good morning";
        String modelName = "Gemini-Thinking"; // Specify the model name here
        String aiReply = ChatAPI.getChatReply(modelName, userPrompt); // 直接调用，同步获取结果

        if (aiReply != null) {
            Anti_addiction.LOGGER.info("AI response: " + aiReply);
        } else {
            Anti_addiction.LOGGER.info("获取 AI 回复失败");
        }
    }
}