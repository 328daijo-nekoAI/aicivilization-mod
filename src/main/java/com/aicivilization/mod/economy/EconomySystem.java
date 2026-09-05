package com.aicivilization.mod.economy;

import com.aicivilization.mod.entity.AICitizenEntity;
import com.aicivilization.mod.memory.CitizenMemoryData;
import com.aicivilization.mod.memory.CitizenMemoryStore;
import com.aicivilization.mod.memory.CivilizationLog;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Random;

/**
 * 経済システム（仕様8.1）。
 * <p>
 * AI個体の「職業」に応じて、一定間隔で収入(wealth)が発生する。
 * AI同士の取引（売買）は、好感度が一定以上あるペア同士でランダムに発生する
 * 簡易モデルとして実装する。
 */
public final class EconomySystem {

    private static final Random RANDOM = new Random();

    /** 職業ごとの基本収入（wealth換算のポイント/回）。 */
    private static final java.util.Map<String, Float> OCCUPATION_INCOME = java.util.Map.of(
            "農家", 3f,
            "商人", 5f,
            "鍛冶屋", 4f,
            "大工", 4f,
            "漁師", 3f,
            "無職", 0.5f
    );

    private static final double INCOME_CHANCE_PER_CHECK = 0.02;
    private static final double TRADE_CHANCE_PER_CHECK = 0.01;

    private EconomySystem() {
    }

    public static void tickIncome(ServerLevel level, AICitizenEntity citizen) {
        if (RANDOM.nextDouble() >= INCOME_CHANCE_PER_CHECK) {
            return;
        }
        List<java.util.UUID> brainIds = citizen.getBrainProfileIds();
        if (brainIds.isEmpty()) {
            return;
        }
        CitizenMemoryData memory = CitizenMemoryStore.load(level, brainIds.get(0));
        if (memory == null) {
            return;
        }
        String occupation = memory.occupation.isBlank() ? "無職" : memory.occupation;
        float income = OCCUPATION_INCOME.getOrDefault(occupation, 1f);
        memory.wealth += income;
        CitizenMemoryStore.save(level, brainIds.get(0), memory);
    }

    /** 近くにいる好感度の高いAI同士で、ランダムに簡易取引を発生させる。 */
    public static void tryTrade(ServerLevel level, AICitizenEntity a, AICitizenEntity b) {
        if (RANDOM.nextDouble() >= TRADE_CHANCE_PER_CHECK) {
            return;
        }
        List<java.util.UUID> brainIdsA = a.getBrainProfileIds();
        List<java.util.UUID> brainIdsB = b.getBrainProfileIds();
        if (brainIdsA.isEmpty() || brainIdsB.isEmpty()) {
            return;
        }

        CitizenMemoryData memA = CitizenMemoryStore.load(level, brainIdsA.get(0));
        CitizenMemoryData memB = CitizenMemoryStore.load(level, brainIdsB.get(0));
        if (memA == null || memB == null) {
            return;
        }

        float tradeAmount = 1f + RANDOM.nextFloat() * 3f;
        if (memA.wealth < tradeAmount) {
            return;
        }

        memA.wealth -= tradeAmount;
        memB.wealth += tradeAmount;

        CitizenMemoryStore.save(level, brainIdsA.get(0), memA);
        CitizenMemoryStore.save(level, brainIdsB.get(0), memB);

        CivilizationLog.record(level, "economy",
                a.getCitizenName() + " が " + b.getCitizenName() + " と取引を行いました。");
    }
}
