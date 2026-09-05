package com.aicivilization.mod.memory;

import com.aicivilization.mod.AICivilizationMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 文明全体の出来事（結婚・誕生・死亡・戦争・法律制定等）を時系列で記録するログ。
 * <p>
 * 保存場所: <ワールドセーブ>/aicivilization/civilization_log.jsonl
 * 1行1イベントのJSON Lines形式にして、追記コストを低く保つ
 * （全体を読み込んで書き直す必要がない）。
 */
public final class CivilizationLog {

    private static final Gson GSON = new GsonBuilder().create();

    private CivilizationLog() {
    }

    public record LogEntry(long timestamp, String category, String message) {
    }

    private static Path getLogFile(ServerLevel level) {
        Path worldDir = level.getServer().getWorldPath(LevelResource.ROOT);
        Path dir = worldDir.resolve("aicivilization");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            AICivilizationMod.LOGGER.error("[AICivilization] ログフォルダの作成に失敗しました。", e);
        }
        return dir.resolve("civilization_log.jsonl");
    }

    /** category例: "marriage", "divorce", "birth", "death", "war", "law", "religion", "economy" */
    public static void record(ServerLevel level, String category, String message) {
        LogEntry entry = new LogEntry(Instant.now().getEpochSecond(), category, message);
        Path file = getLogFile(level);
        try {
            Files.writeString(file, GSON.toJson(entry) + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            AICivilizationMod.LOGGER.error("[AICivilization] 文明ログの記録に失敗しました。", e);
        }
    }

    /** 直近N件のログを読み込む（管理ダッシュボード表示用）。 */
    public static List<LogEntry> readRecent(ServerLevel level, int limit) {
        Path file = getLogFile(level);
        List<LogEntry> result = new ArrayList<>();
        if (!Files.exists(file)) {
            return result;
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            int start = Math.max(0, lines.size() - limit);
            for (int i = start; i < lines.size(); i++) {
                if (lines.get(i).isBlank()) continue;
                result.add(GSON.fromJson(lines.get(i), LogEntry.class));
            }
        } catch (IOException e) {
            AICivilizationMod.LOGGER.error("[AICivilization] 文明ログの読み込みに失敗しました。", e);
        }
        return result;
    }
}
