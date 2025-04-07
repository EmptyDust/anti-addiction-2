package org.fengling.anti_addiction;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatAPI {

    // 存储对话上下文，可以使用 HashMap，key 可以是对话 ID (Player UUID), value 是 DialogueContext 对象
    static final Map<String, DialogueContext> dialogueContextMap = new HashMap<>();

    // 创建HTTPClient，根据配置决定是否使用代理
    private static HttpClient createHttpClient(ModConfig config) {
        HttpClient.Builder clientBuilder = HttpClient.newBuilder();
        
        // 检查是否启用代理
        if (config.getServerConfig().isUseProxy()) {
            String proxyHost = config.getServerConfig().getProxyHost();
            int proxyPort = config.getServerConfig().getProxyPort();
            
            // 确保代理主机名不为空
            if (proxyHost != null && !proxyHost.isEmpty()) {
                Anti_addiction.LOGGER.info("Using proxy: {}:{}", proxyHost, proxyPort);
                clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)));
            } else {
                Anti_addiction.LOGGER.warn("Proxy enabled but no proxy host specified, using direct connection");
            }
        }
        
        return clientBuilder.build();
    }

    public static String getChatReply(String model, String prompt) {
        // 从 ModConfig 加载配置
        ModConfig config = ModConfig.loadConfig("config/mod_config.json"); // 配置文件路径，请确保路径正确
        String apiEndpoint = config.getServerConfig().getAiChatServerAddress();
        String apiKey = config.getServerConfig().getApiKey();

        // 创建可能带有代理设置的HttpClient
        HttpClient client = createHttpClient(config);

        Anti_addiction.LOGGER.info("Using API endpoint: {}", apiEndpoint);

        // 使用 Gemini API 格式构建请求体
        Map<String, Object> requestMap = new HashMap<>();
        
        // 创建 contents 数组
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        
        // 创建 parts 数组
        List<Map<String, String>> parts = new ArrayList<>();
        Map<String, String> textPart = new HashMap<>();
        textPart.put("text", prompt);
        parts.add(textPart);
        
        userMessage.put("parts", parts);
        contents.add(userMessage);
        
        requestMap.put("contents", contents);
        
        Gson gson = new Gson();
        String requestBody = gson.toJson(requestMap);
        
        Anti_addiction.LOGGER.info("Request body: {}", requestBody);
        
        // Append API key to the URL if available
        String finalEndpoint = apiEndpoint;
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            finalEndpoint += "?key=" + apiKey;
        }
        
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(finalEndpoint))
                .header("Content-Type", "application/json");
        
        HttpRequest request = requestBuilder
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        Anti_addiction.LOGGER.info("Request: {}", request);

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString()); // 同步请求
            int statusCode = response.statusCode();
            Anti_addiction.LOGGER.info("Response code: {}", statusCode);
            String responseBodyString = response.body();
            Anti_addiction.LOGGER.info("Response body: {}", responseBodyString);

            if (statusCode >= 200 && statusCode < 300) {
                try {
                    Map<String, Object> responseJson = gson.fromJson(responseBodyString, Map.class);

                    // Gemini API 使用 candidates 而不是 choices
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseJson.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map<String, Object> firstCandidate = candidates.get(0);
                        Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
                        if (content != null) {
                            List<Map<String, Object>> contentParts = (List<Map<String, Object>>) content.get("parts");
                            if (contentParts != null && !contentParts.isEmpty()) {
                                String aiReply = (String) contentParts.get(0).get("text");
                                return aiReply != null ? aiReply.trim() : null;
                            } else {
                                Anti_addiction.LOGGER.info("API 响应中 parts 字段为空");
                                return null;
                            }
                        } else {
                            Anti_addiction.LOGGER.info("API 响应中 content 字段为空");
                            return null;
                        }
                    } else {
                        Anti_addiction.LOGGER.info("API 响应中没有 candidates 字段或为空");
                        return null;
                    }
                } catch (JsonParseException e) {
                    Anti_addiction.LOGGER.info("JSON 解析异常: {}", e.getMessage());
                    Anti_addiction.LOGGER.error("JSON 解析异常: {}", e.getMessage(), e);
                    return null;
                }
            } else {
                Anti_addiction.LOGGER.error("API call failed. Status code: {}", statusCode);
                Anti_addiction.LOGGER.error("Response body: {}", responseBodyString);
                return null;
            }

        } catch (IOException | InterruptedException e) {
            Anti_addiction.LOGGER.info("internet request error: {}", e.getMessage());
            Anti_addiction.LOGGER.error("internet request error: {}", e.getMessage(), e);
            return null;
        }
    }

    public static String getDialogueReply(String model, String prompt, String dialogueId) {
        DialogueContext context = dialogueContextMap.computeIfAbsent(dialogueId, k -> new DialogueContext());

        context.addUserMessage(prompt); // 添加用户消息到上下文

        // 从 ModConfig 加载配置
        ModConfig config = ModConfig.loadConfig("config/mod_config.json");
        String apiEndpoint = config.getServerConfig().getAiChatServerAddress();
        String apiKey = config.getServerConfig().getApiKey();

        // 创建可能带有代理设置的HttpClient
        HttpClient client = createHttpClient(config);

        Anti_addiction.LOGGER.info("Using API endpoint: {}", apiEndpoint);

        // 使用 Gemini API 格式构建请求体
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("contents", context.getContents());
        
        Gson gson = new Gson();
        String requestBody = gson.toJson(requestMap);
        
        Anti_addiction.LOGGER.info("Request body: {}", requestBody);

        // Append API key to the URL if available
        String finalEndpoint = apiEndpoint;
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            finalEndpoint += "?key=" + apiKey;
        }
        
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(finalEndpoint))
                .header("Content-Type", "application/json");
        
        HttpRequest request = requestBuilder
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        Anti_addiction.LOGGER.info("Request: {}", request);

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            Anti_addiction.LOGGER.info("Response code: {}", statusCode);
            String responseBodyString = response.body();
            Anti_addiction.LOGGER.info("Response body: {}", responseBodyString);

            if (statusCode >= 200 && statusCode < 300) {
                try {
                    Map<String, Object> responseJson = gson.fromJson(responseBodyString, Map.class);

                    // Gemini API 使用 candidates 而不是 choices
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseJson.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map<String, Object> firstCandidate = candidates.get(0);
                        Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
                        if (content != null) {
                            List<Map<String, Object>> contentParts = (List<Map<String, Object>>) content.get("parts");
                            if (contentParts != null && !contentParts.isEmpty()) {
                                String aiReply = (String) contentParts.get(0).get("text");
                                if (aiReply != null) {
                                    String trimmedReply = aiReply.trim();
                                    context.addModelMessage(trimmedReply); // 添加 AI 回复到上下文
                                    return trimmedReply;
                                } else {
                                    Anti_addiction.LOGGER.info("API 响应 text 内容为空");
                                    return null;
                                }
                            } else {
                                Anti_addiction.LOGGER.info("API 响应中 parts 字段为空");
                                return null;
                            }
                        } else {
                            Anti_addiction.LOGGER.info("API 响应中 content 字段为空");
                            return null;
                        }
                    } else {
                        Anti_addiction.LOGGER.info("API 响应中没有 candidates 字段或为空");
                        return null;
                    }
                } catch (JsonParseException e) {
                    Anti_addiction.LOGGER.info("JSON 解析异常: {}", e.getMessage());
                    Anti_addiction.LOGGER.error("JSON 解析异常: {}", e.getMessage(),e);
                    return null;
                }
            } else {
                Anti_addiction.LOGGER.error("API call failed. Status code: {}", statusCode);
                Anti_addiction.LOGGER.error("Response body: {}", responseBodyString);
                return null;
            }

        } catch (IOException | InterruptedException e) {
            Anti_addiction.LOGGER.info("internet request error: {}", e.getMessage());
            Anti_addiction.LOGGER.error("internet request error: {}", e.getMessage(), e);
            return null;
        }
    }

    private static String buildDialogueRequestBody(String model, List<Map<String, Object>> contents) {
        // 使用 Gson 库更方便地构建 JSON
        Gson gson = new Gson();
        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("contents", contents);
        return gson.toJson(requestBodyMap);
    }

    public static void main(String[] args) {
        // 从终端读取API密钥
        System.out.println("请输入Gemini API Key (不输入则使用配置文件中的密钥):");
        String inputApiKey = null;
        try {
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            inputApiKey = scanner.nextLine().trim();
        } catch (Exception e) {
            System.out.println("读取输入失败，将使用配置文件中的API Key");
        }

        // 设置模型名称
        String modelName = "gemini-pro"; // 使用正确的模型名称
        
        // 加载配置
        ModConfig config = ModConfig.loadConfig("config/mod_config.json");
        
        // 如果用户输入了API Key，则覆盖配置文件中的设置
        if (inputApiKey != null && !inputApiKey.isEmpty()) {
            config.getServerConfig().setApiKey(inputApiKey);
            System.out.println("正在使用输入的API Key进行测试");
        } else {
            System.out.println("正在使用配置文件中的API Key进行测试");
        }

        // 询问用户使用哪种测试模式
        System.out.println("请选择测试类型 (1: 单次对话, 2: 多轮对话测试):");
        int testType = 1; // 默认为单次对话
        try {
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            testType = Integer.parseInt(scanner.nextLine().trim());
        } catch (Exception e) {
            System.out.println("输入无效，将使用单次对话测试");
        }

        if (testType == 1) {
            // 单次对话测试
            System.out.println("请输入要发送给AI的消息:");
            try {
                java.util.Scanner scanner = new java.util.Scanner(System.in);
                String userPrompt = scanner.nextLine();
                
                System.out.println("正在发送请求，请稍候...");
                String aiReply = ChatAPI.getChatReply(modelName, userPrompt);
                
                if (aiReply != null) {
                    System.out.println("\nAI回复:\n" + aiReply);
                } else {
                    System.out.println("\n获取AI回复失败，请检查API Key是否正确或查看日志");
                }
            } catch (Exception e) {
                System.out.println("测试过程中出现错误: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // 多轮对话测试
            testDialogueChat(modelName);
        }
    }
    
    private static void testDialogueChat(String modelName) {
        try {
            String dialogueId = "test-" + System.currentTimeMillis();
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            System.out.println("\n开始多轮对话测试，输入'exit'结束对话");
            
            while (true) {
                System.out.print("\n用户: ");
                String userInput = scanner.nextLine().trim();
                
                if ("exit".equalsIgnoreCase(userInput)) {
                    System.out.println("对话结束");
                    break;
                }
                
                System.out.println("正在等待AI回复...");
                String aiReply = getDialogueReply(modelName, userInput, dialogueId);
                
                if (aiReply != null) {
                    System.out.println("\nAI: " + aiReply);
                } else {
                    System.out.println("\n获取AI回复失败，请检查API Key是否正确或查看日志");
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("多轮对话测试过程中出现错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


// 修改 DialogueContext 类以适应 Gemini API 格式
class DialogueContext {
    private List<Map<String, Object>> contents;

    public DialogueContext() {
        this.contents = new ArrayList<>();
    }

    public List<Map<String, Object>> getContents() {
        return contents;
    }

    public void addUserMessage(String text) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        
        List<Map<String, String>> parts = new ArrayList<>();
        Map<String, String> textPart = new HashMap<>();
        textPart.put("text", text);
        parts.add(textPart);
        
        message.put("parts", parts);
        this.contents.add(message);
    }

    public void addModelMessage(String text) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "model");  // Gemini API 使用 "model" 而不是 "assistant"
        
        List<Map<String, String>> parts = new ArrayList<>();
        Map<String, String> textPart = new HashMap<>();
        textPart.put("text", text);
        parts.add(textPart);
        
        message.put("parts", parts);
        this.contents.add(message);
    }

    public void clearMessages() {
        this.contents.clear();
    }
}