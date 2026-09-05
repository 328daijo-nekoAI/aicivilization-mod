package com.aicivilization.mod.civilization;

import com.aicivilization.mod.brain.BrainProfile;
import com.aicivilization.mod.brain.BrainProfilePool;
import com.aicivilization.mod.entity.AICitizenEntity;
import com.aicivilization.mod.entity.ModEntities;
import com.aicivilization.mod.memory.CitizenMemoryData;
import com.aicivilization.mod.memory.CitizenMemoryStore;
import com.aicivilization.mod.memory.CivilizationLog;
import com.aicivilization.mod.name.NameRegistry;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 出産・子供の誕生・成長システム（仕様5.3, 2.4）。
 * <p>
 * 子供は誕生時点では思考しない（脳を持たない）。
 * 成長が完了した時点で初めて、新規APIプロファイルをプールから消費し、
 * 子供時代の記憶データを引き継いだ状態で自律思考を開始する。
 */
public final class BirthSystem {

    private static final Random RANDOM = new Random();

    /** 結婚済みペアが1ティックごとに出産を試みる確率（十分に低くして頻発を防ぐ）。 */
    private static final double BIRTH_CHANCE_PER_CHECK = 0.001;

    /** 子供が成長するまでの必要ティック数（現実の分単位に相当する適度な長さ）。 */
    public static final int GROWTH_TICKS_REQUIRED = 24000 * 3; // ゲーム内3日相当

    private BirthSystem() {
    }

    public static boolean shouldAttemptBirth(AICitizenEntity a, AICitizenEntity b) {
        if (a.getPartnerId() == null || !a.getPartnerId().equals(b.getUUID())) {
            return false; // 結婚していないペアは対象外
        }
        return RANDOM.nextDouble() < BIRTH_CHANCE_PER_CHECK;
    }

    /** 子供エンティティを誕生させる。脳の割当は行わず、成長完了まで思考しない。 */
    public static AICitizenEntity giveBirth(ServerLevel level, AICitizenEntity mother, AICitizenEntity father) {
        AICitizenEntity child = ModEntities.AI_CITIZEN.get().create(level);
        if (child == null) {
            return null;
        }
        child.moveTo(mother.getX(), mother.getY(), mother.getZ(), 0f, 0f);
        child.setAIChild(true);
        child.setMotherId(mother.getUUID());
        child.setFatherId(father.getUUID());

        NameRegistry nameRegistry = level.getDataStorage().computeIfAbsent(
                NameRegistry::load, NameRegistry::new, NameRegistry.getDataName());
        String name = nameRegistry.generateUniqueName();
        child.setCitizenName(name);

        level.addFreshEntity(child);

        CivilizationLog.record(level, "birth",
                mother.getCitizenName() + " と " + father.getCitizenName() + " の間に "
                        + name + " が生まれました。");

        // 子供時代の記憶データを、脳未割当の状態で先に用意しておく（親の名前・出生情報を記録）。
        // profileIdはまだ存在しないため、エンティティのUUIDを仮キーとして使う暫定ファイルに記録し、
        // 成長時にBrainProfilePool側の新規プロファイルへ引き継ぐ。
        CitizenMemoryData childMemory = new CitizenMemoryData();
        childMemory.citizenName = name;
        childMemory.addEvent(mother.getCitizenName() + "と" + father.getCitizenName() + "の子として生まれた。");
        CitizenMemoryStore.save(level, child.getUUID(), childMemory);

        return child;
    }

    /**
     * 成長完了時の処理。新しいAPIプロファイルをプールから確保し、
     * 子供時代の記憶を引き継いで自律思考を開始させる（仕様2.4）。
     *
     * @return 成功時はnull、失敗時（プールに空きがない等）はエラーメッセージ
     */
    public static String growUp(ServerLevel level, BrainProfilePool pool, AICitizenEntity child,
                                 UUID newProfileId) {
        BrainProfile newProfile = pool.getProfile(newProfileId);
        if (newProfile == null) {
            return "指定された脳プロファイルが見つかりません。";
        }
        if (newProfile.isAssigned()) {
            return "指定された脳プロファイルは既に使用中です。";
        }

        String error = pool.assignProfilesToEntity(child.getUUID(), List.of(newProfileId));
        if (error != null) {
            return error;
        }

        child.setBrainProfileIds(List.of(newProfileId));
        child.setAIChild(false);

        // 子供時代の暫定記憶（エンティティUUIDキー）を、新しいプロファイルIDへ引き継ぐ
        CitizenMemoryStore.transferMemory(level, child.getUUID(), newProfileId);

        CivilizationLog.record(level, "growth",
                child.getCitizenName() + " が成長し、自分の意思で行動できるようになりました。");

        return null;
    }
}
