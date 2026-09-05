package com.aicivilization.mod.politics;

import com.aicivilization.mod.entity.AICitizenEntity;
import com.aicivilization.mod.memory.CivilizationLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Random;

/**
 * 村長選挙イベントの実行ロジック。
 * 人口が閾値を超え、かつ一定期間選挙が行われていない場合に発生させる。
 */
public final class ElectionSystem {

    private static final Random RANDOM = new Random();

    /** 選挙間隔（同じ村長が長期間居座らないよう、一定間隔で再選挙を行う）。 */
    private static final long ELECTION_INTERVAL_TICKS = 24000L * 7; // ゲーム内7日相当

    private ElectionSystem() {
    }

    public static void checkAndRunElection(ServerLevel level, PoliticsState politics, List<AICitizenEntity> adults) {
        if (adults.size() < PoliticsState.ELECTION_POPULATION_THRESHOLD) {
            return;
        }

        long currentTick = level.getGameTime();
        if (politics.hasMayor() && (currentTick - politics.getLastElectionTick()) < ELECTION_INTERVAL_TICKS) {
            return;
        }

        // 簡易実装: ランダムな大人AIを村長に選出する。
        // 将来的には人望（好感度の合計）等に基づく選出ロジックに拡張できる。
        AICitizenEntity newMayor = adults.get(RANDOM.nextInt(adults.size()));
        politics.setMayor(newMayor.getUUID(), currentTick);

        CivilizationLog.record(level, "politics",
                newMayor.getCitizenName() + " が村長に選出されました。");

        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§6[AI文明] §f村長選挙の結果、" + newMayor.getCitizenName() + "が新しい村長になりました。"));
        }
    }
}
