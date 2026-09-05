package com.aicivilization.mod.civilization;

import com.aicivilization.mod.entity.AICitizenEntity;
import com.aicivilization.mod.memory.CitizenMemoryData;
import com.aicivilization.mod.memory.CitizenMemoryStore;
import com.aicivilization.mod.memory.CivilizationLog;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/**
 * 結婚・離婚の判定と成立処理（仕様5.1, 5.2）。
 * <p>
 * 好感度(Affection)は CitizenMemoryData.relationships に格納される。
 * AIの思考結果テキストに「プロポーズ」「結婚したい」等の意図が含まれるかは
 * フェーズ3の初期実装では簡易キーワード判定で行い、
 * 将来的にはGroqへの構造化出力リクエスト（JSON形式で意図を返させる）に置き換える。
 */
public final class MarriageSystem {

    private static final float MARRIAGE_AFFECTION_THRESHOLD = 70f;
    private static final float DIVORCE_GRUDGE_THRESHOLD = 70f;

    private MarriageSystem() {
    }

    /** 2人が結婚可能な状態かを判定する。 */
    public static boolean canMarry(AICitizenEntity a, AICitizenEntity b) {
        if (a.isAIChild() || b.isAIChild()) {
            return false;
        }
        if (a.getPartnerId() != null || b.getPartnerId() != null) {
            return false; // どちらかが既婚
        }
        return true;
    }

    /** 好感度が閾値を超えているかをチェックする（記憶ファイルの relationships を参照）。 */
    public static boolean hasEnoughAffection(ServerLevel level, AICitizenEntity self, AICitizenEntity other) {
        CitizenMemoryData memory = getMemory(level, self);
        if (memory == null) {
            return false;
        }
        CitizenMemoryData.RelationshipEntry rel = memory.relationships.get(other.getCitizenName());
        return rel != null && rel.affection >= MARRIAGE_AFFECTION_THRESHOLD;
    }

    /** 結婚を成立させる。双方のNBTデータと記憶ファイルを更新する。 */
    public static void marry(ServerLevel level, AICitizenEntity a, AICitizenEntity b) {
        a.setPartnerId(b.getUUID());
        b.setPartnerId(a.getUUID());

        updateRelationship(level, a, b, "配偶者");
        updateRelationship(level, b, a, "配偶者");

        CivilizationLog.record(level, "marriage",
                a.getCitizenName() + " と " + b.getCitizenName() + " が結婚しました。");
    }

    /** 離婚を成立させる。 */
    public static void divorce(ServerLevel level, AICitizenEntity a, AICitizenEntity b) {
        a.setPartnerId(null);
        b.setPartnerId(null);

        updateRelationship(level, a, b, "元配偶者");
        updateRelationship(level, b, a, "元配偶者");

        CivilizationLog.record(level, "divorce",
                a.getCitizenName() + " と " + b.getCitizenName() + " が離婚しました。");
    }

    /** 不満度が閾値を超えているかチェックする（離婚判断に使用）。 */
    public static boolean shouldConsiderDivorce(ServerLevel level, AICitizenEntity self, AICitizenEntity partner) {
        CitizenMemoryData memory = getMemory(level, self);
        if (memory == null) {
            return false;
        }
        CitizenMemoryData.RelationshipEntry rel = memory.relationships.get(partner.getCitizenName());
        // 信頼度の低下を「不満の蓄積」の代理指標として使う（簡易実装）
        return rel != null && rel.trust <= (100f - DIVORCE_GRUDGE_THRESHOLD);
    }

    private static void updateRelationship(ServerLevel level, AICitizenEntity self, AICitizenEntity other, String relationType) {
        CitizenMemoryData memory = getMemory(level, self);
        if (memory == null) {
            return;
        }
        CitizenMemoryData.RelationshipEntry rel = memory.relationships.computeIfAbsent(
                other.getCitizenName(), k -> new CitizenMemoryData.RelationshipEntry());
        rel.relationType = relationType;

        UUID primaryBrainId = self.getBrainProfileIds().isEmpty() ? null : self.getBrainProfileIds().get(0);
        if (primaryBrainId != null) {
            CitizenMemoryStore.save(level, primaryBrainId, memory);
        }
    }

    private static CitizenMemoryData getMemory(ServerLevel level, AICitizenEntity citizen) {
        if (citizen.getBrainProfileIds().isEmpty()) {
            return null;
        }
        return CitizenMemoryStore.load(level, citizen.getBrainProfileIds().get(0));
    }
}
