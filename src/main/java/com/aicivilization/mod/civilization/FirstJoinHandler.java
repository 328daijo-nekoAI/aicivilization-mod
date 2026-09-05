package com.aicivilization.mod.civilization;

import com.aicivilization.mod.AICivilizationMod;
import com.aicivilization.mod.brain.BrainProfilePool;
import com.aicivilization.mod.brain.BrainProfilePoolProvider;
import com.aicivilization.mod.network.packet.SyncBrainPoolPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

/**
 * ワールドへの初回参加を検知し、AI出現セットアップの案内を出すハンドラ。
 * <p>
 * 仕様4.1: 「最初に出現させるAI人数」等を選ぶGUIは、いきなり強制的に開くと
 * 操作に戸惑う可能性があるため、フェーズ1ではチャットで案内を出し、
 * プレイヤー自身が /aicivilization spawn またはアイテムを使って開く方式にする。
 * （強制ポップアップの是非は運用しながら調整する）
 */
@Mod.EventBusSubscriber(modid = AICivilizationMod.MOD_ID)
public final class FirstJoinHandler {

    // サーバー起動中のみ有効な簡易フラグ（恒久化する場合はSavedData化する）
    private static final Set<java.util.UUID> notifiedPlayers = new HashSet<>();

    private FirstJoinHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (notifiedPlayers.contains(player.getUUID())) {
            return;
        }
        notifiedPlayers.add(player.getUUID());

        BrainProfilePool pool = BrainProfilePoolProvider.get(player.serverLevel());
        boolean isFirstEverJoin = pool.getAllProfiles().isEmpty();

        if (isFirstEverJoin) {
            player.sendSystemMessage(Component.literal(
                    "§6[AI文明] §fようこそ。Groq APIキーを登録してAIたちを生み出しましょう。"));
            player.sendSystemMessage(Component.literal(
                    "§6[AI文明] §f/aicivilization brains でAPIキー登録、/aicivilization spawn でAIを出現させられます。"));
            player.sendSystemMessage(Component.literal(
                    "§6[AI文明] §fブラウザで http://localhost:" + com.aicivilization.mod.dashboard.DashboardLifecycle.DASHBOARD_PORT
                            + " を開くと管理ダッシュボードが使えます。"));
        } else {
            SyncBrainPoolPacket.sendTo(player, pool);
        }
    }
}
