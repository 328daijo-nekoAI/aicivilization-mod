package com.aicivilization.mod.brain;

import java.util.List;

/**
 * Groq APIで選択可能なモデルの一覧。
 * <p>
 * 実際に利用可能なモデルはGroq側の提供状況により変わるため、
 * ここは「よく使われる代表例」としてGUIの選択肢に出す。
 * 管理画面/GUIからは自由入力でこれ以外のモデル名を指定することも可能にする。
 */
public final class GroqModels {

    private GroqModels() {
    }

    public static final String DEFAULT_MODEL = "llama-3.3-70b-versatile";

    /** GUIのドロップダウンに出すおすすめモデル一覧。 */
    public static final List<String> RECOMMENDED = List.of(
            "llama-3.3-70b-versatile",   // 高性能・重要な判断向け
            "llama-3.1-8b-instant",       // 軽量・高速・日常会話向け
            "gemma2-9b-it",                // バランス型
            "mixtral-8x7b-32768"           // 長文コンテキスト向け（記憶量が多いAI向け）
    );
}
