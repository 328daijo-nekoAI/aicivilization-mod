package com.aicivilization.mod.brain;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * 1つの「脳」= Groq APIプロファイル。
 * <p>
 * profileName: プレイヤーが分かりやすいように付ける名前（例: "メインの脳1"）
 * apiKey: Groq APIキー（gsk_で始まる文字列）
 * modelName: 使用するGroqモデル名（例: llama-3.3-70b-versatile）
 * profileId: プール内でこのプロファイルを一意に識別するID
 * assignedEntityId: 現在このプロファイルを使用しているAIエンティティのUUID（未割当ならnull）
 */
public class BrainProfile {

    private final UUID profileId;
    private String profileName;
    private String apiKey;
    private String modelName;
    private UUID assignedEntityId; // どのAIに割り当て済みか（未割当ならnull）

    public BrainProfile(UUID profileId, String profileName, String apiKey, String modelName) {
        this.profileId = profileId;
        this.profileName = profileName;
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.assignedEntityId = null;
    }

    public static BrainProfile createNew(String profileName, String apiKey, String modelName) {
        return new BrainProfile(UUID.randomUUID(), profileName, apiKey, modelName);
    }

    public UUID getProfileId() {
        return profileId;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public UUID getAssignedEntityId() {
        return assignedEntityId;
    }

    public void setAssignedEntityId(UUID assignedEntityId) {
        this.assignedEntityId = assignedEntityId;
    }

    public boolean isAssigned() {
        return assignedEntityId != null;
    }

    /**
     * APIキーをUI表示用に一部マスクする（例: gsk_ab12...xyz9）。
     * 管理画面やチャット表示でキー全体を晒さないための配慮。
     */
    public String getMaskedApiKey() {
        if (apiKey == null || apiKey.length() < 10) {
            return "****";
        }
        return apiKey.substring(0, 6) + "..." + apiKey.substring(apiKey.length() - 4);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("ProfileId", profileId);
        tag.putString("ProfileName", profileName);
        tag.putString("ApiKey", apiKey);
        tag.putString("ModelName", modelName);
        if (assignedEntityId != null) {
            tag.putUUID("AssignedEntityId", assignedEntityId);
        }
        return tag;
    }

    public static BrainProfile load(CompoundTag tag) {
        UUID id = tag.getUUID("ProfileId");
        String name = tag.getString("ProfileName");
        String key = tag.getString("ApiKey");
        String model = tag.getString("ModelName");
        BrainProfile profile = new BrainProfile(id, name, key, model);
        if (tag.hasUUID("AssignedEntityId")) {
            profile.setAssignedEntityId(tag.getUUID("AssignedEntityId"));
        }
        return profile;
    }
}
