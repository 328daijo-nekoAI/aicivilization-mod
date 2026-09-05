package com.aicivilization.mod.civilization;

import com.aicivilization.mod.brain.BrainProfile;
import com.aicivilization.mod.brain.BrainProfilePool;
import com.aicivilization.mod.entity.AICitizenEntity;
import com.aicivilization.mod.entity.ModEntities;
import com.aicivilization.mod.memory.CitizenMemoryStore;
import com.aicivilization.mod.name.NameRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.UUID;

/**
 * 新規AI市民を実際にワールドへスポーンさせる処理。
 * <p>
 * 呼び出し元（初回セットアップGUI・追加スポーンリクエスト）は、
 * 事前にBrainProfilePool側で脳の個数バリデーション(1〜5個)を済ませてから
 * ここを呼ぶ想定。
 *
 * @return 失敗時はエラーメッセージ文字列、成功時はnull
 */
public final class AISpawner {

    private AISpawner() {
    }

    public static String spawnNewAI(ServerLevel level, BlockPos pos, BrainProfilePool pool,
                                     List<UUID> selectedProfileIds) {

        // 1. 脳の個数バリデーション（念のため二重チェック）
        if (!pool.hasValidBrainCount(selectedProfileIds.size())) {
            return "AIには1〜" + BrainProfilePool.getMaxProfilesPerEntity() + "個の脳が必要です。";
        }

        // 2. エンティティ生成
        AICitizenEntity citizen = ModEntities.AI_CITIZEN.get().create(level);
        if (citizen == null) {
            return "AIエンティティの生成に失敗しました。";
        }
        citizen.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);

        // 3. 名前の重複回避割当
        NameRegistry nameRegistry = level.getDataStorage().computeIfAbsent(
                NameRegistry::load, NameRegistry::new, NameRegistry.getDataName());
        String name = nameRegistry.generateUniqueName();
        citizen.setCitizenName(name);

        // 4. 脳の割り当て（プール側の整合性チェック込み）
        String assignError = pool.assignProfilesToEntity(citizen.getUUID(), selectedProfileIds);
        if (assignError != null) {
            nameRegistry.releaseName(name);
            return assignError;
        }
        citizen.setBrainProfileIds(selectedProfileIds);

        // 5. 個体別記憶ファイルを初期化（脳プロファイル1個につき1ファイル）
        for (UUID profileId : selectedProfileIds) {
            BrainProfile profile = pool.getProfile(profileId);
            if (profile != null) {
                CitizenMemoryStore.initializeMemoryFile(level, profileId, name);
            }
        }

        // 6. ワールドへ追加
        level.addFreshEntity(citizen);

        return null;
    }
}
