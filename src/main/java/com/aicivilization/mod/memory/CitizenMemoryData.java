package com.aicivilization.mod.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 脳プロファイル1個分の記憶ファイルの中身（JSONにそのままシリアライズされる）。
 * <p>
 * events: 直近の出来事ログ（詳細）。一定件数を超えたら古いものから summary に圧縮する。
 * summary: 古い記憶をダイジェスト化した要約文（プロンプト肥大化を防ぐため）。
 * relationships: 他のAI（名前をキーにする）への好感度・信頼度。
 */
public class CitizenMemoryData {

    public static final int MAX_RAW_EVENTS = 30;

    public String profileId = "";
    public String citizenName = "";
    public String occupation = "";
    public String residence = "";

    public List<String> events = new ArrayList<>();
    public String summary = "";

    public Map<String, RelationshipEntry> relationships = new HashMap<>();

    public float happiness = 50f;
    public float wealth = 0f;

    public static class RelationshipEntry {
        public float affection = 0f;   // 好感度
        public float trust = 0f;       // 信頼度
        public String relationType = "知人"; // 知人/恋人/配偶者/親/子/元配偶者 等
    }

    /**
     * 新しい出来事を追加する。件数が上限を超えたら、
     * 古いイベント群をざっくり要約文に圧縮してsummaryへ追記する。
     * （本格的な要約生成はGroq API側で行う想定。ここでは単純な件数管理のみ行う）
     */
    public void addEvent(String eventText) {
        events.add(eventText);
        if (events.size() > MAX_RAW_EVENTS) {
            List<String> toCompress = events.subList(0, events.size() - MAX_RAW_EVENTS);
            StringBuilder sb = new StringBuilder(summary);
            for (String e : toCompress) {
                sb.append(" / ").append(e);
            }
            summary = sb.toString();
            events = new ArrayList<>(events.subList(events.size() - MAX_RAW_EVENTS, events.size()));
        }
    }
}
