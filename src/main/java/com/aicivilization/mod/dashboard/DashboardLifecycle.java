package com.aicivilization.mod.dashboard;

import com.aicivilization.mod.AICivilizationMod;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * サーバー起動・終了に合わせて管理ダッシュボードを起動・停止する。
 * ポート番号は固定値だが、将来的にはconfig化してもよい
 * （configの主目的はAPIキー管理から外れるため、フェーズ1〜4では固定値で運用）。
 */
@Mod.EventBusSubscriber(modid = AICivilizationMod.MOD_ID)
public final class DashboardLifecycle {

    public static final int DASHBOARD_PORT = 25580;

    private DashboardLifecycle() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        DashboardServer.startIfNotRunning(event.getServer(), DASHBOARD_PORT);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        DashboardServer.stopServer();
    }
}
