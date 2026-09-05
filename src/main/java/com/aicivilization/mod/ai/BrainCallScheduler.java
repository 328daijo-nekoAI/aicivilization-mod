package com.aicivilization.mod.ai;

import com.aicivilization.mod.AICivilizationMod;
import com.aicivilization.mod.brain.BrainProfile;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * AI個体ごとの「思考の呼び出し頻度」を制御するマネージャー。
 * <p>
 * Groq無料枠のレート制限を考慮し、個体ごとにクールダウンを設ける。
 * 複数の脳（APIプロファイル）を持つ個体は、固定の役割分担ではなく
 * ラウンドロビン方式で順番に脳を切り替えて使う
 * （2.2章: AI自身が自由に考える方式のシンプルな実装として）。
 */
public final class BrainCallScheduler {

    private static final Random RANDOM = new Random();

    // 個体ごとの最終呼び出し時刻(ms)
    private static final Map<UUID, Long> lastCallTime = new ConcurrentHashMap<>();
    // 個体ごとの「次に使う脳のインデックス」（ラウンドロビン用）
    private static final Map<UUID, Integer> nextBrainIndex = new ConcurrentHashMap<>();

    /** 通常の思考（会話・日常行動判断）の最短呼び出し間隔。 */
    private static final long NORMAL_COOLDOWN_MS = 15_000;

    /** 結婚・離婚・引っ越しなど重大な判断をさせる場合の間隔（頻度を落として負荷を抑える）。 */
    private static final long MAJOR_DECISION_COOLDOWN_MS = 60_000;

    private BrainCallScheduler() {
    }

    public static boolean canThinkNow(UUID entityId, boolean isMajorDecision) {
        long cooldown = isMajorDecision ? MAJOR_DECISION_COOLDOWN_MS : NORMAL_COOLDOWN_MS;
        long last = lastCallTime.getOrDefault(entityId, 0L);
        return System.currentTimeMillis() - last >= cooldown;
    }

    /**
     * そのAIに割り当てられた脳リストから、ラウンドロビンで次に使う1つを選ぶ。
     * 重大な判断の場合は、複数脳があればランダムな別の脳を使うことで、
     * 単一の脳の癖に判断が偏るのを防ぐ。
     */
    public static BrainProfile selectBrain(UUID entityId, List<BrainProfile> availableBrains, boolean isMajorDecision) {
        if (availableBrains.isEmpty()) {
            return null;
        }
        if (availableBrains.size() == 1) {
            return availableBrains.get(0);
        }
        if (isMajorDecision) {
            return availableBrains.get(RANDOM.nextInt(availableBrains.size()));
        }
        int idx = nextBrainIndex.getOrDefault(entityId, 0) % availableBrains.size();
        nextBrainIndex.put(entityId, idx + 1);
        return availableBrains.get(idx);
    }

    /**
     * 思考リクエストを実行する。呼び出し可否のチェック・クールダウン更新・
     * エラーハンドリングをまとめて行う。
     * <p>
     * 注意: onResult はGroq呼び出し用の別スレッドから呼ばれる。
     * ワールド状態（エンティティやNBT）を直接操作する処理は、呼び出し元で
     * サーバーのメインスレッドキュー（ServerLevelのexecute等）に積んでから行うこと。
     *
     * @param onResult 別スレッドから呼ばれる結果コールバック（メインスレッド化は呼び出し元の責務）
     */
    public static void requestThink(UUID entityId, List<BrainProfile> availableBrains, boolean isMajorDecision,
                                     String systemPrompt, String userPrompt,
                                     Consumer<String> onResult) {
        if (!canThinkNow(entityId, isMajorDecision)) {
            return;
        }
        BrainProfile brain = selectBrain(entityId, availableBrains, isMajorDecision);
        if (brain == null) {
            return;
        }

        lastCallTime.put(entityId, System.currentTimeMillis());

        CompletableFuture<String> future = GroqClient.requestCompletion(
                brain.getApiKey(), brain.getModelName(), systemPrompt, userPrompt);

        future.whenComplete((result, error) -> {
            if (error != null) {
                AICivilizationMod.LOGGER.warn("[AICivilization] AI({})の思考呼び出しに失敗しました。", entityId, error);
                return;
            }
            onResult.accept(result);
        });
    }

    /** エンティティ削除時にクリーンアップする。 */
    public static void cleanup(UUID entityId) {
        lastCallTime.remove(entityId);
        nextBrainIndex.remove(entityId);
    }
}
