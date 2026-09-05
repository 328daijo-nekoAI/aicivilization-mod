package com.aicivilization.mod.memory;

import com.aicivilization.mod.AICivilizationMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerLevel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 「脳（APIプロファイル）1つにつき1つの記憶ファイル」を管理するクラス。
 * <p>
 * 保存場所: <ワールドセーブ>/aicivilization/memories/<key>.json
 * ワールドフォルダの中に置くことで、ワールドをコピーするだけで文明データごと
 * 引き継げるようにする。
 * <p>
 * キーについて: 通常は脳プロファイルのUUID(BrainProfile#getProfileId)を使うが、
 * 脳をまだ持たない子供の間は、暫定キーとしてエンティティ自身のUUIDを使って
 * 記憶を積み立てておく（BirthSystem参照）。成長時にtransferMemoryで
 * 本物の脳プロファイルIDへ引き継ぐ。
 */
public final class CitizenMemoryStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private CitizenMemoryStore() {
    }

    private static Path getMemoriesDir(ServerLevel level) {
        Path worldDir = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        Path dir = worldDir.resolve("aicivilization").resolve("memories");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            AICivilizationMod.LOGGER.error("[AICivilization] 記憶フォルダの作成に失敗しました。", e);
        }
        return dir;
    }

    private static Path getMemoryFile(ServerLevel level, UUID profileId) {
        return getMemoriesDir(level).resolve(profileId.toString() + ".json");
    }

    /** 新規AI生成時に、空の記憶ファイルを作成する。 */
    public static void initializeMemoryFile(ServerLevel level, UUID profileId, String citizenName) {
        Path file = getMemoryFile(level, profileId);
        if (Files.exists(file)) {
            return; // 既存の記憶を上書きしない（子供成長時の引き継ぎに対応するため）
        }
        CitizenMemoryData data = new CitizenMemoryData();
        data.citizenName = citizenName;
        data.profileId = profileId.toString();
        save(level, profileId, data);
    }

    public static CitizenMemoryData load(ServerLevel level, UUID profileId) {
        Path file = getMemoryFile(level, profileId);
        if (!Files.exists(file)) {
            return null;
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            return GSON.fromJson(json, CitizenMemoryData.class);
        } catch (IOException e) {
            AICivilizationMod.LOGGER.error("[AICivilization] 記憶ファイルの読み込みに失敗しました: {}", profileId, e);
            return null;
        }
    }

    public static void save(ServerLevel level, UUID profileId, CitizenMemoryData data) {
        Path file = getMemoryFile(level, profileId);
        try {
            String json = GSON.toJson(data);
            Files.writeString(file, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            AICivilizationMod.LOGGER.error("[AICivilization] 記憶ファイルの保存に失敗しました: {}", profileId, e);
        }
    }

    /**
     * 子供が成長して新しいAPIプロファイルを消費する際、
     * 旧プロファイル（子供時代）の記憶データを新プロファイルへコピーする。
     */
    public static void transferMemory(ServerLevel level, UUID fromProfileId, UUID toProfileId) {
        CitizenMemoryData oldData = load(level, fromProfileId);
        if (oldData == null) {
            return;
        }
        oldData.profileId = toProfileId.toString();
        oldData.events.add("（成長して大人になりました。子供時代の記憶を引き継いでいます）");
        save(level, toProfileId, oldData);
    }
}
