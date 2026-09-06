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
