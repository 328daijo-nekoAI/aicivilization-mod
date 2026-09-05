package com.aicivilization.mod.politics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 政治システム（仕様8.2）。
 * <p>
 * 人口が一定数を超えると村長選挙イベントが発生し、
 * 村長は法律（特定行動の制限等）を制定できる。
 * 法律は文字列のリストとして保持し、内容の解釈・強制はActionInterpreter等が行う
 * （フェーズ1〜6では簡易的に「法律が存在する」という事実の記録・表示を優先する）。
 */
public class PoliticsState extends SavedData {

    private static final String DATA_NAME = "aicivilization_politics";

    /** 村長選挙が発生する人口の閾値。 */
    public static final int ELECTION_POPULATION_THRESHOLD = 8;

    private UUID mayorId = null;
    private final List<String> laws = new ArrayList<>();
    private long lastElectionTick = 0;

    public PoliticsState() {
    }

    public UUID getMayorId() {
        return mayorId;
    }

    public void setMayor(UUID mayorId, long currentTick) {
        this.mayorId = mayorId;
        this.lastElectionTick = currentTick;
        setDirty();
    }

    public boolean hasMayor() {
        return mayorId != null;
    }

    public List<String> getLaws() {
        return new ArrayList<>(laws);
    }

    public void addLaw(String law) {
        laws.add(law);
        setDirty();
    }

    public long getLastElectionTick() {
        return lastElectionTick;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (mayorId != null) {
            tag.putUUID("MayorId", mayorId);
        }
        tag.putLong("LastElectionTick", lastElectionTick);
        ListTag lawList = new ListTag();
        for (String law : laws) {
            lawList.add(StringTag.valueOf(law));
        }
        tag.put("Laws", lawList);
        return tag;
    }

    public static PoliticsState load(CompoundTag tag) {
        PoliticsState state = new PoliticsState();
        if (tag.hasUUID("MayorId")) {
            state.mayorId = tag.getUUID("MayorId");
        }
        state.lastElectionTick = tag.getLong("LastElectionTick");
        ListTag lawList = tag.getList("Laws", Tag.TAG_STRING);
        for (int i = 0; i < lawList.size(); i++) {
            state.laws.add(lawList.getString(i));
        }
        return state;
    }

    public static String getDataName() {
        return DATA_NAME;
    }
}
