package com.aicivilization.mod.war;

import com.aicivilization.mod.entity.AICitizenEntity;
import com.aicivilization.mod.memory.CitizenMemoryData;
import com.aicivilization.mod.memory.CitizenMemoryStore;
import com.aicivilization.mod.memory.CivilizationLog;
import com.aicivilization.mod.religion.ReligionState;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Random;

/**
 * 戦争システム（仕様8.4）。
 * <p>
 * 資源・思想（宗教・信頼関係）の対立が蓄積すると、AI同士が敵対状態になり、
 * 最終的に武力衝突（実際の戦闘AI化）に発展することがある。
 * <p>
 * フェーズ1〜6の実装では、対立度(hostility)を記憶データの信頼度(trust)の
 * 逆数的指標として扱い、一定閾値を下回った異なる信仰同士のペアに、
 * まれに戦闘トリガーを発生させる簡易モデルとする。
 */
public final class WarSystem {

    private static final Random RANDOM = new Random();
    private static final double CONFLICT_CHANCE_PER_CHECK = 0.002;
    private static final float HOSTILITY_TRUST_THRESHOLD = 20f;

    private WarSystem() {
    }

    /**
     * 異なる信仰に属する、信頼度の低い2人のAIの間で武力衝突が起きるかを判定する。
     * 衝突が起きた場合、双方を一時的に敵対状態にし、Minecraftの通常の戦闘AIに委ねる。
     */
    public static void checkConflict(ServerLevel level, ReligionState religionState,
                                      AICitizenEntity a, AICitizenEntity b) {
        if (a.getBrainProfileIds().isEmpty() || b.getBrainProfileIds().isEmpty()) {
            return;
        }

        boolean differentFaith = isDifferentFaith(religionState, a, b);
        if (!differentFaith) {
            return; // フェーズ1〜6では思想対立を宗教の違いで代理する簡易モデル
        }

        CitizenMemoryData memA = CitizenMemoryStore.load(level, a.getBrainProfileIds().get(0));
        if (memA == null) {
            return;
        }
        CitizenMemoryData.RelationshipEntry rel = memA.relationships.get(b.getCitizenName());
        float trust = rel != null ? rel.trust : 50f; // 未知の相手はデフォルト中立

        if (trust > HOSTILITY_TRUST_THRESHOLD) {
            return;
        }
        if (RANDOM.nextDouble() >= CONFLICT_CHANCE_PER_CHECK) {
            return;
        }

        triggerCombat(level, a, b);
    }

    private static boolean isDifferentFaith(ReligionState religionState, AICitizenEntity a, AICitizenEntity b) {
        // 単純化: それぞれが所属する信仰の預言者IDが異なれば「異なる信仰」とみなす。
        // どちらも無宗教なら対立要因にはしない。
        var faiths = religionState.getAllFaiths();
        String faithOfA = findFaith(faiths, a.getUUID());
        String faithOfB = findFaith(faiths, b.getUUID());
        if (faithOfA == null && faithOfB == null) {
            return false;
        }
        return !java.util.Objects.equals(faithOfA, faithOfB);
    }

    private static String findFaith(List<ReligionState.Faith> faiths, java.util.UUID citizenId) {
        for (ReligionState.Faith faith : faiths) {
            if (faith.prophetId.equals(citizenId) || faith.followers.contains(citizenId)) {
                return faith.faithName;
            }
        }
        return null;
    }

    private static void triggerCombat(ServerLevel level, AICitizenEntity a, AICitizenEntity b) {
        // Minecraft標準の敵対AIターゲティングを利用して、双方を一時的に交戦状態にする。
        a.setLastHurtByMob(b);
        b.setLastHurtByMob(a);

        CivilizationLog.record(level, "war",
                a.getCitizenName() + " と " + b.getCitizenName() + " の間で対立が武力衝突に発展しました。");
    }
}
