package com.aicivilization.mod.religion;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 宗教システム（仕様8.5）。
 * <p>
 * 特定のAIが「預言者」を自称し、他のAIを信者化する独自信仰が自然発生する。
 * ここでは複数の「信仰(Faith)」が同時に存在しうるデータ構造にする
 * （複数の預言者が現れて宗派が分かれることも許容する）。
 */
public class ReligionState extends SavedData {

    private static final String DATA_NAME = "aicivilization_religion";

    public static class Faith {
        public UUID prophetId;
        public String faithName;
        public List<UUID> followers = new ArrayList<>();

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("ProphetId", prophetId);
            tag.putString("FaithName", faithName);
            ListTag followerList = new ListTag();
            for (UUID f : followers) {
                CompoundTag idTag = new CompoundTag();
                idTag.putUUID("Id", f);
                followerList.add(idTag);
            }
            tag.put("Followers", followerList);
            return tag;
        }

        public static Faith load(CompoundTag tag) {
            Faith faith = new Faith();
            faith.prophetId = tag.getUUID("ProphetId");
            faith.faithName = tag.getString("FaithName");
            ListTag followerList = tag.getList("Followers", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < followerList.size(); i++) {
                faith.followers.add(followerList.getCompound(i).getUUID("Id"));
            }
            return faith;
        }
    }

    private final Map<UUID, Faith> faithsByProphet = new HashMap<>();

    public ReligionState() {
    }

    public boolean hasFaith(UUID prophetId) {
        return faithsByProphet.containsKey(prophetId);
    }

    public Faith createFaith(UUID prophetId, String faithName) {
        Faith faith = new Faith();
        faith.prophetId = prophetId;
        faith.faithName = faithName;
        faithsByProphet.put(prophetId, faith);
        setDirty();
        return faith;
    }

    public void addFollower(UUID prophetId, UUID followerId) {
        Faith faith = faithsByProphet.get(prophetId);
        if (faith != null && !faith.followers.contains(followerId)) {
            faith.followers.add(followerId);
            setDirty();
        }
    }

    public List<Faith> getAllFaiths() {
        return new ArrayList<>(faithsByProphet.values());
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Faith faith : faithsByProphet.values()) {
            list.add(faith.save());
        }
        tag.put("Faiths", list);
        return tag;
    }

    public static ReligionState load(CompoundTag tag) {
        ReligionState state = new ReligionState();
        ListTag list = tag.getList("Faiths", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Faith faith = Faith.load(list.getCompound(i));
            state.faithsByProphet.put(faith.prophetId, faith);
        }
        return state;
    }

    public static String getDataName() {
        return DATA_NAME;
    }
}
