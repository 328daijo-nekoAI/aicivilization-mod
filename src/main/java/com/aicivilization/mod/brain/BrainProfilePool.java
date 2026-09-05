package com.aicivilization.mod.brain;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ワールドに登録されている全ての「脳（APIプロファイル）」をまとめて管理するプール。
 * <p>
 * SavedData として world/data/ 配下に永続化される。
 * ゲーム内GUIから追加・編集・削除でき、config直接編集は不要。
 * <p>
 * 重要な制約:
 * - AIエンティティ1体につき1〜5個のプロファイルを割り当てる
 * - 0個の状態のAIはスポーンさせない（呼び出し側でバリデーションする）
 */
public class BrainProfilePool extends SavedData {

    private static final String DATA_NAME = "aicivilization_brain_pool";
    private static final int MAX_PROFILES_PER_ENTITY = 5;
    private static final int MIN_PROFILES_PER_ENTITY = 1;

    private final Map<UUID, BrainProfile> profiles = new HashMap<>();

    public BrainProfilePool() {
    }

    // ------------------------------------------------------------------
    // プロファイルCRUD
    // ------------------------------------------------------------------

    public BrainProfile addProfile(String profileName, String apiKey, String modelName) {
        BrainProfile profile = BrainProfile.createNew(profileName, apiKey, modelName);
        profiles.put(profile.getProfileId(), profile);
        setDirty();
        return profile;
    }

    public boolean removeProfile(UUID profileId) {
        BrainProfile removed = profiles.remove(profileId);
        if (removed != null) {
            setDirty();
            return true;
        }
        return false;
    }

    public BrainProfile getProfile(UUID profileId) {
        return profiles.get(profileId);
    }

    public List<BrainProfile> getAllProfiles() {
        return new ArrayList<>(profiles.values());
    }

    /** まだどのAIにも割り当てられていない脳の一覧（新規AI追加時の選択肢に出す）。 */
    public List<BrainProfile> getUnassignedProfiles() {
        return profiles.values().stream()
                .filter(p -> !p.isAssigned())
                .collect(Collectors.toList());
    }

    public void updateProfile(UUID profileId, String newName, String newApiKey, String newModelName) {
        BrainProfile profile = profiles.get(profileId);
        if (profile == null) {
            return;
        }
        if (newName != null && !newName.isBlank()) {
            profile.setProfileName(newName);
        }
        if (newApiKey != null && !newApiKey.isBlank()) {
            profile.setApiKey(newApiKey);
        }
        if (newModelName != null && !newModelName.isBlank()) {
            profile.setModelName(newModelName);
        }
        setDirty();
    }

    // ------------------------------------------------------------------
    // AIエンティティへの割り当て
    // ------------------------------------------------------------------

    /**
     * 指定した脳プロファイル群をあるAIエンティティに割り当てる。
     * 1〜5個の範囲を外れる場合はエラーメッセージを返し、割当を行わない。
     *
     * @return 成功時はnull、失敗時はエラーメッセージ
     */
    public String assignProfilesToEntity(UUID entityId, List<UUID> profileIds) {
        if (profileIds == null || profileIds.size() < MIN_PROFILES_PER_ENTITY) {
            return "AIには最低" + MIN_PROFILES_PER_ENTITY + "個の脳（APIプロファイル）が必要です。";
        }
        if (profileIds.size() > MAX_PROFILES_PER_ENTITY) {
            return "AIに割り当てられる脳は最大" + MAX_PROFILES_PER_ENTITY + "個までです。";
        }

        List<BrainProfile> targets = new ArrayList<>();
        for (UUID id : profileIds) {
            BrainProfile profile = profiles.get(id);
            if (profile == null) {
                return "指定された脳プロファイルが見つかりません: " + id;
            }
            if (profile.isAssigned() && !profile.getAssignedEntityId().equals(entityId)) {
                return "脳プロファイル「" + profile.getProfileName() + "」は既に他のAIに割り当てられています。";
            }
            targets.add(profile);
        }

        for (BrainProfile profile : targets) {
            profile.setAssignedEntityId(entityId);
        }
        setDirty();
        return null;
    }

    /** そのエンティティに割り当てられている脳の一覧を取得する。 */
    public List<BrainProfile> getProfilesForEntity(UUID entityId) {
        return profiles.values().stream()
                .filter(p -> entityId.equals(p.getAssignedEntityId()))
                .collect(Collectors.toList());
    }

    /** エンティティ削除時などに、割当を解除してプールに戻す。 */
    public void unassignAllForEntity(UUID entityId) {
        boolean changed = false;
        for (BrainProfile profile : profiles.values()) {
            if (entityId.equals(profile.getAssignedEntityId())) {
                profile.setAssignedEntityId(null);
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    public boolean hasValidBrainCount(int count) {
        return count >= MIN_PROFILES_PER_ENTITY && count <= MAX_PROFILES_PER_ENTITY;
    }

    public static int getMaxProfilesPerEntity() {
        return MAX_PROFILES_PER_ENTITY;
    }

    public static int getMinProfilesPerEntity() {
        return MIN_PROFILES_PER_ENTITY;
    }

    // ------------------------------------------------------------------
    // 永続化
    // ------------------------------------------------------------------

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (BrainProfile profile : profiles.values()) {
            list.add(profile.save());
        }
        tag.put("Profiles", list);
        return tag;
    }

    public static BrainProfilePool load(CompoundTag tag) {
        BrainProfilePool pool = new BrainProfilePool();
        ListTag list = tag.getList("Profiles", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            BrainProfile profile = BrainProfile.load(list.getCompound(i));
            pool.profiles.put(profile.getProfileId(), profile);
        }
        return pool;
    }

    public static String getDataName() {
        return DATA_NAME;
    }
}
