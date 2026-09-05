package com.aicivilization.mod.civilization;

import com.aicivilization.mod.ai.BrainCallScheduler;
import com.aicivilization.mod.brain.BrainProfile;
import com.aicivilization.mod.brain.BrainProfilePool;
import com.aicivilization.mod.brain.BrainProfilePoolProvider;
import com.aicivilization.mod.economy.EconomySystem;
import com.aicivilization.mod.entity.AICitizenEntity;
import com.aicivilization.mod.memory.CitizenMemoryData;
import com.aicivilization.mod.memory.CitizenMemoryStore;
import com.aicivilization.mod.politics.ElectionSystem;
import com.aicivilization.mod.politics.PoliticsState;
import com.aicivilization.mod.religion.ReligionState;
import com.aicivilization.mod.religion.ReligionSystem;
import com.aicivilization.mod.war.WarSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 全AI市民の「思考ループ」を回すティックハンドラ。
 * <p>
 * 毎ティック全員に思考させるのはレート制限的に不可能なため、
 * BrainCallScheduler のクールダウンを通してから実際にGroqへ問い合わせる。
 * 結果の適用（NBT更新・行動決定）は必ずサーバーのメインスレッドで行う。
 */
@Mod.EventBusSubscriber(modid = com.aicivilization.mod.AICivilizationMod.MOD_ID)
public final class CivilizationTickHandler {

    /** ワールド全体からAI市民を検索するための広大な範囲（縦横高さとも実質無制限扱い）。 */
    private static final AABB WORLD_BOUNDS = new AABB(
            -3.0E7, -2048, -3.0E7, 3.0E7, 2048, 3.0E7);

    private CivilizationTickHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        for (ServerLevel level : server.getAllLevels()) {
            BrainProfilePool pool = BrainProfilePoolProvider.get(level);
            PoliticsState politics = level.getDataStorage().computeIfAbsent(
                    PoliticsState::load, PoliticsState::new, PoliticsState.getDataName());
            ReligionState religion = level.getDataStorage().computeIfAbsent(
                    ReligionState::load, ReligionState::new, ReligionState.getDataName());

            List<AICitizenEntity> allCitizens = new ArrayList<>(
                    level.getEntitiesOfClass(AICitizenEntity.class, WORLD_BOUNDS));

            for (AICitizenEntity citizen : allCitizens) {
                tickCitizen(level, pool, religion, citizen, allCitizens);
            }

            // 政治: 人口が閾値を超えたら選挙をチェック（大人のみ対象）
            List<AICitizenEntity> adults = allCitizens.stream()
                    .filter(c -> !c.isAIChild())
                    .toList();
            ElectionSystem.checkAndRunElection(level, politics, adults);
        }
    }

    private static void tickCitizen(ServerLevel level, BrainProfilePool pool, ReligionState religion,
                                     AICitizenEntity citizen, List<AICitizenEntity> allCitizens) {
        if (citizen.isAIChild()) {
            tickChildGrowth(level, pool, citizen);
            return; // 子供は思考しない（成長して大人になるまで静止/簡易AIのみ）
        }

        checkLifeEvents(level, citizen, allCitizens);
        EconomySystem.tickIncome(level, citizen);

        // 近隣AIとの経済取引・宗教勧誘・対立チェック（重い全探索を避けるため近傍のみ）
        List<AICitizenEntity> nearby = allCitizens.stream()
                .filter(c -> c != citizen && c.distanceToSqr(citizen) < 400)
                .toList();
        for (AICitizenEntity other : nearby) {
            EconomySystem.tryTrade(level, citizen, other);
            WarSystem.checkConflict(level, religion, citizen, other);
        }
        ReligionSystem.tryConvertNearby(level, religion, nearby);

        List<BrainProfile> brains = pool.getProfilesForEntity(citizen.getUUID());
        if (brains.isEmpty()) {
            return; // 脳がない状態は本来スポーン時点で弾かれているはずだが、念のため防御
        }

        if (!BrainCallScheduler.canThinkNow(citizen.getUUID(), false)) {
            return;
        }

        String situationContext = buildSituationContext(citizen, nearby);
        String systemPrompt = buildSystemPrompt(level, citizen);

        BrainCallScheduler.requestThink(citizen.getUUID(), brains, false, systemPrompt, situationContext,
                (resultText) -> {
                    // Groq呼び出しは別スレッドで完了するため、ワールド操作はメインスレッドに戻す
                    level.getServer().execute(() -> applyThinkResult(level, religion, citizen, resultText));
                });
    }

    /** 子供の成長進行を管理する。成長完了時刻に達したら、成長イベントをプレイヤーに通知する。 */
    private static void tickChildGrowth(ServerLevel level, BrainProfilePool pool, AICitizenEntity child) {
        if (child.tickCount < BirthSystem.GROWTH_TICKS_REQUIRED) {
            return;
        }
        if (!pool.getUnassignedProfiles().isEmpty()) {
            UUID freeProfileId = pool.getUnassignedProfiles().get(0).getProfileId();
            String error = BirthSystem.growUp(level, pool, child, freeProfileId);
            if (error == null) {
                for (ServerPlayer player : level.players()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§6[AI文明] §f" + child.getCitizenName() + "が成長し、大人になりました。"));
                }
            }
        }
        // 未割当の脳がない場合は成長を保留し、次のティックで再チェックする
        // （プレイヤーが新しい脳を登録するまで待機）
    }

    /** 結婚・離婚・出産の判定を行う（毎ティック軽量チェックのみ、実際の思考頻度制御はBrainCallScheduler側）。 */
    private static void checkLifeEvents(ServerLevel level, AICitizenEntity citizen, List<AICitizenEntity> allCitizens) {
        if (citizen.getPartnerId() != null) {
            AICitizenEntity partner = findCitizenByUUID(level, citizen.getPartnerId());
            if (partner != null) {
                if (MarriageSystem.shouldConsiderDivorce(level, citizen, partner)) {
                    MarriageSystem.divorce(level, citizen, partner);
                } else if (BirthSystem.shouldAttemptBirth(citizen, partner)) {
                    BirthSystem.giveBirth(level, citizen, partner);
                }
            }
            return;
        }

        // 未婚の場合、近くの未婚AIとの好感度をチェックしてプロポーズを試みる
        for (AICitizenEntity other : allCitizens) {
            if (other == citizen || other.distanceToSqr(citizen) >= 100) {
                continue;
            }
            if (MarriageSystem.canMarry(citizen, other)
                    && MarriageSystem.hasEnoughAffection(level, citizen, other)
                    && MarriageSystem.hasEnoughAffection(level, other, citizen)) {
                MarriageSystem.marry(level, citizen, other);
                break;
            }
        }
    }

    private static AICitizenEntity findCitizenByUUID(ServerLevel level, UUID uuid) {
        var entity = level.getEntity(uuid);
        return entity instanceof AICitizenEntity ac ? ac : null;
    }

    /**
     * AIへの状況説明プロンプト。固定の役割分担を持たせず、
     * 「今どういう状況か」を伝えて、AI自身に自由に行動を考えさせる。
     */
    private static String buildSituationContext(AICitizenEntity citizen, List<AICitizenEntity> nearby) {
        StringBuilder sb = new StringBuilder();
        sb.append("あなたは").append(citizen.getCitizenName()).append("という名前のMinecraft世界の住人です。\n");
        sb.append("現在の幸福度: ").append(citizen.getHappiness()).append("/100\n");

        if (citizen.getPartnerId() != null) {
            sb.append("あなたには結婚相手がいます。\n");
        } else {
            sb.append("あなたはまだ結婚していません。\n");
        }

        sb.append("近くには").append(nearby.size()).append("人の他の住人がいます。\n");

        sb.append("あなたは今何をしたいですか？ 自由に考えて、短く一言で答えてください。");
        return sb.toString();
    }

    private static String buildSystemPrompt(ServerLevel level, AICitizenEntity citizen) {
        StringBuilder sb = new StringBuilder();
        sb.append("あなたはMinecraft世界に住む自律したAI住人「").append(citizen.getCitizenName()).append("」です。\n");
        sb.append("固定のルールに縛られず、あなた自身の意思で考え、行動を選んでください。\n");
        sb.append("結婚、離婚、子育て、家づくり、仕事、友人関係、対立など、");
        sb.append("人間らしい判断を自分自身で行ってください。\n");

        CitizenMemoryData memory = getPrimaryMemory(level, citizen);
        if (memory != null && !memory.summary.isBlank()) {
            sb.append("これまでのあなたの記憶の要約: ").append(memory.summary).append("\n");
        }
        return sb.toString();
    }

    private static CitizenMemoryData getPrimaryMemory(ServerLevel level, AICitizenEntity citizen) {
        List<UUID> brainIds = citizen.getBrainProfileIds();
        if (brainIds.isEmpty()) {
            return null;
        }
        return CitizenMemoryStore.load(level, brainIds.get(0));
    }

    private static void applyThinkResult(ServerLevel level, ReligionState religion, AICitizenEntity citizen, String resultText) {
        if (resultText == null || resultText.isBlank()) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(citizen) < 2500) { // 半径50ブロック程度
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§b" + citizen.getCitizenName() + "§7: " + resultText.trim()));
            }
        }

        // 発言内容から実際の行動（建築等）へ変換する
        ActionInterpreter.interpretAndAct(level, citizen, resultText);

        // 宗教の自然発生チェック
        ReligionSystem.checkProphetEmergence(level, religion, citizen, resultText);

        // 記憶ファイルへの記録
        List<UUID> brainIds = citizen.getBrainProfileIds();
        if (!brainIds.isEmpty()) {
            UUID primaryBrainId = brainIds.get(0);
            CitizenMemoryData memory = CitizenMemoryStore.load(level, primaryBrainId);
            if (memory == null) {
                memory = new CitizenMemoryData();
                memory.profileId = primaryBrainId.toString();
                memory.citizenName = citizen.getCitizenName();
            }
            memory.addEvent(resultText.trim());
            CitizenMemoryStore.save(level, primaryBrainId, memory);
        }
    }
}
