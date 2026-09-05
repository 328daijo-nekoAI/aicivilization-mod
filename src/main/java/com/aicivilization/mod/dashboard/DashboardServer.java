package com.aicivilization.mod.dashboard;

import com.aicivilization.mod.AICivilizationMod;
import com.aicivilization.mod.brain.BrainProfile;
import com.aicivilization.mod.brain.BrainProfilePool;
import com.aicivilization.mod.brain.BrainProfilePoolProvider;
import com.aicivilization.mod.memory.CitizenMemoryData;
import com.aicivilization.mod.memory.CitizenMemoryStore;
import com.aicivilization.mod.memory.CivilizationLog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.iki.elonen.NanoHTTPD;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ワールド全体を見渡せる管理ダッシュボードをローカルで配信するHTTPサーバー。
 * <p>
 * ブラウザから http://localhost:<port> でアクセスする。
 * 提供するAPI:
 * - GET  /                : ダッシュボードHTML
 * - GET  /api/brains      : 脳プロファイル一覧(JSON)
 * - POST /api/brains      : 脳プロファイル追加
 * - DELETE /api/brains/{id} : 脳プロファイル削除
 * - GET  /api/memory/{profileId} : 個体別記憶ファイルの内容
 * - POST /api/memory/{profileId} : 個体別記憶ファイルの編集（上書き保存）
 * - GET  /api/log         : 文明全体ログ（直近100件）
 * <p>
 * サーバーはMinecraft本体とは別ポートで動く単純なローカルサーバーであり、
 * 外部公開は想定しない（認証なし、localhost限定運用を前提とする）。
 */
public class DashboardServer extends NanoHTTPD {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DashboardServer instance;

    private final MinecraftServer mcServer;

    public DashboardServer(MinecraftServer mcServer, int port) {
        super(port);
        this.mcServer = mcServer;
    }

    public static void startIfNotRunning(MinecraftServer mcServer, int port) {
        if (instance != null) {
            return;
        }
        try {
            instance = new DashboardServer(mcServer, port);
            instance.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            AICivilizationMod.LOGGER.info("[AICivilization] 管理ダッシュボードを起動しました: http://localhost:{}", port);
        } catch (Exception e) {
            AICivilizationMod.LOGGER.error("[AICivilization] 管理ダッシュボードの起動に失敗しました。", e);
        }
    }

    public static void stopServer() {
        if (instance != null) {
            instance.stop();
            instance = null;
        }
    }

    private ServerLevel overworld() {
        return mcServer.overworld();
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        try {
            if (uri.equals("/") && method == Method.GET) {
                return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8",
                        DashboardHtml.PAGE);
            }

            if (uri.equals("/api/brains") && method == Method.GET) {
                return jsonResponse(getBrainsJson());
            }

            if (uri.equals("/api/brains") && method == Method.POST) {
                return handleAddBrain(session);
            }

            if (uri.startsWith("/api/brains/") && method == Method.DELETE) {
                String idStr = uri.substring("/api/brains/".length());
                return handleRemoveBrain(idStr);
            }

            if (uri.startsWith("/api/memory/") && method == Method.GET) {
                String idStr = uri.substring("/api/memory/".length());
                return handleGetMemory(idStr);
            }

            if (uri.startsWith("/api/memory/") && method == Method.POST) {
                String idStr = uri.substring("/api/memory/".length());
                return handleSaveMemory(session, idStr);
            }

            if (uri.equals("/api/log") && method == Method.GET) {
                return jsonResponse(getLogJson());
            }

            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
        } catch (Exception e) {
            AICivilizationMod.LOGGER.error("[AICivilization] ダッシュボードAPIエラー", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal Error: " + e.getMessage());
        }
    }

    private Response jsonResponse(String json) {
        Response response = newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", json);
        response.addHeader("Access-Control-Allow-Origin", "*");
        return response;
    }

    private String getBrainsJson() {
        BrainProfilePool pool = BrainProfilePoolProvider.get(overworld());
        List<Map<String, Object>> list = new ArrayList<>();
        for (BrainProfile p : pool.getAllProfiles()) {
            list.add(Map.of(
                    "id", p.getProfileId().toString(),
                    "name", p.getProfileName(),
                    "maskedKey", p.getMaskedApiKey(),
                    "model", p.getModelName(),
                    "assigned", p.isAssigned()
            ));
        }
        return GSON.toJson(list);
    }

    private Response handleAddBrain(IHTTPSession session) throws java.io.IOException, ResponseException {
        Map<String, String> body = parseBody(session);
        String name = body.getOrDefault("name", "無題の脳");
        String key = body.get("apiKey");
        String model = body.getOrDefault("model", "llama-3.3-70b-versatile");

        if (key == null || key.isBlank()) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"apiKeyは必須です\"}");
        }

        BrainProfilePool pool = BrainProfilePoolProvider.get(overworld());
        BrainProfile created = pool.addProfile(name, key, model);
        return jsonResponse(GSON.toJson(Map.of("id", created.getProfileId().toString())));
    }

    private Response handleRemoveBrain(String idStr) {
        try {
            UUID id = UUID.fromString(idStr);
            BrainProfilePool pool = BrainProfilePoolProvider.get(overworld());
            boolean removed = pool.removeProfile(id);
            return jsonResponse(GSON.toJson(Map.of("removed", removed)));
        } catch (IllegalArgumentException e) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"不正なID\"}");
        }
    }

    private Response handleGetMemory(String idStr) {
        try {
            UUID id = UUID.fromString(idStr);
            CitizenMemoryData data = CitizenMemoryStore.load(overworld(), id);
            if (data == null) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\":\"記憶が見つかりません\"}");
            }
            return jsonResponse(GSON.toJson(data));
        } catch (IllegalArgumentException e) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"不正なID\"}");
        }
    }

    private Response handleSaveMemory(IHTTPSession session, String idStr) throws java.io.IOException, ResponseException {
        try {
            UUID id = UUID.fromString(idStr);
            Map<String, String> body = parseBody(session);
            String rawJson = body.get("postData");
            CitizenMemoryData data = GSON.fromJson(rawJson, CitizenMemoryData.class);
            if (data == null) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"JSONの解析に失敗しました\"}");
            }
            CitizenMemoryStore.save(overworld(), id, data);
            return jsonResponse("{\"saved\":true}");
        } catch (IllegalArgumentException e) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"不正なID\"}");
        }
    }

    private String getLogJson() {
        List<CivilizationLog.LogEntry> entries = CivilizationLog.readRecent(overworld(), 100);
        return GSON.toJson(entries);
    }

    /** NanoHTTPDのフォームデータ解析ヘルパー。POST本体は"postData"キーで取得できる。 */
    private Map<String, String> parseBody(IHTTPSession session) throws java.io.IOException, ResponseException {
        Map<String, String> files = new java.util.HashMap<>();
        session.parseBody(files);
        String raw = files.get("postData");
        if (raw == null) {
            return Map.of();
        }
        // JSON形式のPOSTボディをそのまま返しつつ、簡易パースもしておく
        Map<String, String> result = new java.util.HashMap<>();
        result.put("postData", raw);
        try {
            Map<?, ?> parsed = GSON.fromJson(raw, Map.class);
            for (Map.Entry<?, ?> e : parsed.entrySet()) {
                result.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
            }
        } catch (Exception ignored) {
            // JSONでない場合はpostDataのみ返す
        }
        return result;
    }
}
