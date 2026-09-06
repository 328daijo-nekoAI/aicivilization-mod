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

    public static final String DEFAULT_MODEL = "openai/gpt-oss-120b";

    /** GUIのドロップダウンに出すおすすめモデル一覧。 */
    public static final List<String> RECOMMENDED = List.of(
            "openai/gpt-oss-120b",   // 高性能・重要な判断向け（旧llama-3.3-70b-versatile相当）
            "openai/gpt-oss-20b",     // 軽量・高速・日常会話向け（旧llama-3.1-8b-instant相当）
            "qwen/qwen3.6-27b",       // バランス型
            "meta-llama/llama-4-scout-17b-16e-instruct" // 長文コンテキスト向け
    );
}
