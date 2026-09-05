package com.aicivilization.mod.name;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AIの名前が被らないように一元管理するレジストリ。
 * 使用済み名前をワールドセーブに永続化し、新規AI生成のたびに重複チェックする。
 */
public class NameRegistry extends SavedData {

    private static final String DATA_NAME = "aicivilization_name_registry";

    private final Set<String> usedNames = new HashSet<>();

    /**
     * 候補名リスト。和風・現代風どちらも混ぜておく。
     * ゲーム内GUIから独自の名前をこのリストに追加することも想定する。
     */
    private static final List<String> NAME_POOL = new ArrayList<>(List.of(
            "ハルト", "ミナト", "ユウキ", "ソウタ", "レン", "アオイ", "ユイ", "サクラ",
            "ヒナタ", "アカリ", "リク", "ダイキ", "ケンジ", "ショウ", "タクミ", "カズキ",
            "ミオ", "コトネ", "ナナミ", "リンカ", "アカネ", "ハルカ", "マユ", "ユメ",
            "タロウ", "ジロウ", "ハナ", "サキ", "ツバサ", "カエデ", "モモカ", "イブキ",
            "アレン", "エマ", "リオ", "ノア", "ミラ", "ソラ", "ツキ", "ホシ"
    ));

    public NameRegistry() {
    }

    /**
     * 未使用の名前を1つ払い出す。候補が尽きた場合は連番付きでフォールバック生成する。
     */
    public String generateUniqueName() {
        List<String> shuffled = new ArrayList<>(NAME_POOL);
        java.util.Collections.shuffle(shuffled);

        for (String candidate : shuffled) {
            if (!usedNames.contains(candidate)) {
                usedNames.add(candidate);
                setDirty();
                return candidate;
            }
        }

        // 候補が尽きた場合: 名前+連番でフォールバック
        int suffix = 2;
        String base = shuffled.isEmpty() ? "AI市民" : shuffled.get(0);
        String fallback = base + suffix;
        while (usedNames.contains(fallback)) {
            suffix++;
            fallback = base + suffix;
        }
        usedNames.add(fallback);
        setDirty();
        return fallback;
    }

    /** GUIやコマンドから独自の名前を候補プールに追加する。 */
    public void addCustomNameCandidate(String name) {
        if (name != null && !name.isBlank() && !NAME_POOL.contains(name)) {
            NAME_POOL.add(name);
        }
    }

    /** AIが死亡・削除された場合に名前を解放し、再利用可能にする。 */
    public void releaseName(String name) {
        if (usedNames.remove(name)) {
            setDirty();
        }
    }

    public boolean isNameUsed(String name) {
        return usedNames.contains(name);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (String name : usedNames) {
            list.add(StringTag.valueOf(name));
        }
        tag.put("UsedNames", list);
        return tag;
    }

    public static NameRegistry load(CompoundTag tag) {
        NameRegistry registry = new NameRegistry();
        ListTag list = tag.getList("UsedNames", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            registry.usedNames.add(list.getString(i));
        }
        return registry;
    }

    public static String getDataName() {
        return DATA_NAME;
    }
}
