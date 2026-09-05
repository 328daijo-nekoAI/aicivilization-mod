package com.aicivilization.mod.ai;

import com.aicivilization.mod.AICivilizationMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Groq API（OpenAI互換のchat/completionsエンドポイント）を叩くクライアント。
 * <p>
 * 重要: すべての呼び出しは非同期で行い、Minecraftのメインスレッド（ワールドティック）を
 * 絶対にブロックしない。結果はCompletableFutureで返し、呼び出し側は
 * サーバーのメインスレッドに戻ってから結果を適用すること。
 */
public final class GroqClient {

    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final Gson GSON = new GsonBuilder().create();

    // AI呼び出し専用のスレッドプール。メインスレッドと完全に分離する。
    private static final Executor AI_EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "aicivilization-groq-worker");
        t.setDaemon(true);
        return t;
    });

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(30))
            .writeTimeout(Duration.ofSeconds(10))
            .build();

    private GroqClient() {
    }

    /**
     * AIに「今どう行動すべきか」を尋ねる。
     *
     * @param apiKey       Groq APIキー
     * @param modelName    使用モデル名
     * @param systemPrompt AIの人格・状況設定（第2章: 固定の役割分担を設けず、
     *                     AI自身が自由に考えられるようなプロンプトにする）
     * @param userPrompt   現在の状況コンテキスト
     * @return 非同期でAIの応答テキストを返すFuture。失敗時は例外を含むFutureになる。
     */
    public static CompletableFuture<String> requestCompletion(String apiKey, String modelName,
                                                                String systemPrompt, String userPrompt) {
        CompletableFuture<String> future = new CompletableFuture<>();

        AI_EXECUTOR.execute(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("model", modelName);
                body.addProperty("temperature", 0.9);
                body.addProperty("max_tokens", 400);

                JsonArray messages = new JsonArray();
                messages.add(makeMessage("system", systemPrompt));
                messages.add(makeMessage("user", userPrompt));
                body.add("messages", messages);

                RequestBody requestBody = RequestBody.create(
                        GSON.toJson(body), MediaType.parse("application/json"));

                Request request = new Request.Builder()
                        .url(ENDPOINT)
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .post(requestBody)
                        .build();

                try (Response response = HTTP_CLIENT.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errBody = response.body() != null ? response.body().string() : "";
                        future.completeExceptionally(
                                new IOException("Groq APIエラー: HTTP " + response.code() + " " + errBody));
                        return;
                    }
                    String responseBody = response.body() != null ? response.body().string() : "";
                    String content = extractContent(responseBody);
                    future.complete(content);
                }
            } catch (Exception e) {
                AICivilizationMod.LOGGER.warn("[AICivilization] Groq API呼び出しに失敗しました。", e);
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private static JsonObject makeMessage(String role, String content) {
        JsonObject msg = new JsonObject();
        msg.addProperty("role", role);
        msg.addProperty("content", content);
        return msg;
    }

    private static String extractContent(String responseJson) {
        JsonObject root = GSON.fromJson(responseJson, JsonObject.class);
        JsonArray choices = root.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        JsonObject firstChoice = choices.get(0).getAsJsonObject();
        JsonObject message = firstChoice.getAsJsonObject("message");
        return message.get("content").getAsString();
    }
}
