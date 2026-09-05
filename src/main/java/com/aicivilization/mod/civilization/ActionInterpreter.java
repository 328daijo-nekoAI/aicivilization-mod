package com.aicivilization.mod.civilization;

import com.aicivilization.mod.construction.HouseBuilder;
import com.aicivilization.mod.entity.AICitizenEntity;
import net.minecraft.server.level.ServerLevel;

/**
 * AIの思考結果（自然文）から、実際のワールド操作につながる行動意図を
 * 簡易的なキーワードマッチングで読み取るクラス。
 * <p>
 * 将来的には、Groqへのリクエスト自体を「JSON形式で意図を返させる」
 * 構造化出力方式に置き換えることで精度を上げられる
 * （フェーズ7のバランス調整で検討）。フェーズ1〜6ではまず動くものを優先する。
 */
public final class ActionInterpreter {

    private ActionInterpreter() {
    }

    public static void interpretAndAct(ServerLevel level, AICitizenEntity citizen, String thoughtText) {
        if (thoughtText == null) {
            return;
        }
        String text = thoughtText.trim();

        if (!citizen.getResidenceData().hasHome()
                && containsAny(text, "家を建て", "家がほしい", "住む場所", "引っ越し", "新居")) {
            HouseBuilder.tryBuildHouse(level, citizen);
        }

        // 幸福度への簡易フィードバック: ポジティブ/ネガティブなキーワードで微調整する。
        if (containsAny(text, "嬉しい", "楽しい", "幸せ", "良かった")) {
            citizen.setHappiness(citizen.getHappiness() + 2f);
        } else if (containsAny(text, "悲しい", "つらい", "腹が立つ", "嫌だ")) {
            citizen.setHappiness(citizen.getHappiness() - 2f);
        }
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
