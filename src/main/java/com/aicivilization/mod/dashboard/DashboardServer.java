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

            if (uri.startsWith("/api/memory/") && method
