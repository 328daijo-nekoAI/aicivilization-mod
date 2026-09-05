package com.aicivilization.mod.religion;

import com.aicivilization.mod.entity.AICitizenEntity;
import com.aicivilization.mod.memory.CivilizationLog;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Random;

/**
 * 宗教の自然発生を扱うロジック。
 * <p>
 * AIの発言に「預言者」「信仰」等のキーワードが含まれ、かつ幸福度が
 * 非常に高い（≒カリスマ的な自信がある状態のメタファー）場合に、
 * 一定確率でそのAIが新しい信仰を立ち上げる。
 * 周囲のAIは、そのAIとの好感度が高ければ信者になることがある。
 */
public final class ReligionSystem {

    private static final Random RANDOM = new Random();
    private static final double NEW_FAITH_CHANCE = 0.05;
    private static final double CONVERT_CHANCE = 0.1;

    private ReligionSystem() {
    }

    public static void checkProphetEmergence(ServerLevel level, ReligionState religionState,
                                              AICitizenEntity citizen, String thoughtText) {
        if (thoughtText == null || religionState.hasFaith(citizen.getUUID())) {
            return;
        }
        boolean mentionsFaith = thoughtText.contains("預言") || thoughtText.contains("信仰")
                || thoughtText.contains("神");
        if (!mentionsFaith || citizen.getHappiness() < 80f) {
            return;
        }
        if (RANDOM.nextDouble() >= NEW_FAITH_CHANCE) {
            return;
        }

        String faithName = citizen.getCitizenName() + "教";
        religionState.createFaith(citizen.getUUID(), faithName);

        CivilizationLog.record(level, "religion",
                citizen.getCitizenName() + " が新しい信仰「" + faithName + "」を立ち上げました。");
    }

    /** 近隣のAIが、好感度の高い預言者の信者になるかどうかを判定する。 */
    public static void tryConvertNearby(ServerLevel level, ReligionState religionState,
                                         List<AICitizenEntity> nearbyCitizens) {
        for (ReligionState.Faith faith : religionState.getAllFaiths()) {
            for (AICitizenEntity citizen : nearbyCitizens) {
                if (citizen.getUUID().equals(faith.prophetId) || faith.followers.contains(citizen.getUUID())) {
                    continue;
                }
                if (RANDOM.nextDouble() < CONVERT_CHANCE) {
                    religionState.addFollower(faith.prophetId, citizen.getUUID());
                    CivilizationLog.record(level, "religion",
                            citizen.getCitizenName() + " が「" + faith.faithName + "」の信者になりました。");
                }
            }
        }
    }
}
