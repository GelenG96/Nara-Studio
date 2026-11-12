package com.example.ai

import com.example.data.model.AiEngineMode
import com.example.data.model.NaraAiTool

/**
 * Internal AI backend routing configuration.
 * Provider, endpoint, and model identifiers are strictly internal and must NEVER be surfaced in UI.
 */
internal object NaraModelRegistry {
    const val OFFICIAL_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models"

    const val MODEL_PRIMARY = "gemini-3.8-flash"
    const val MODEL_FALLBACK_LITE = "gemini-3.5-flash-lite"
    const val MODEL_DEEP_REASONING = "gemini-4-carbon"
    const val MODEL_COMPLEX_INTERACTIVE = "gemini-4-argon"
    const val MODEL_GENERAL_CHAT = "gemini-4-hydrogen"
    const val MODEL_FAST_LITE = "gemini-3.1-flash-lite"
    const val MODEL_TTS_PRIMARY = "gemini-3.8-flash-tts"
    const val MODEL_TTS_FALLBACK = "gemini-3.1-flash-tts-preview"

    fun resolveModelChain(
        engineMode: AiEngineMode,
        tool: NaraAiTool? = null,
        isMultiTurnChat: Boolean = false,
        autoFallback: Boolean = true
    ): List<String> {
        val ordered = when {
            tool?.prefersDeepReasoning == true || engineMode == AiEngineMode.DEEP -> listOf(
                MODEL_DEEP_REASONING,
                MODEL_PRIMARY,
                MODEL_FALLBACK_LITE
            )
            tool?.prefersFastMode == true || engineMode == AiEngineMode.FAST -> listOf(
                MODEL_FAST_LITE,
                MODEL_FALLBACK_LITE,
                MODEL_PRIMARY
            )
            engineMode == AiEngineMode.INTERACTIVE -> listOf(
                MODEL_COMPLEX_INTERACTIVE,
                MODEL_GENERAL_CHAT,
                MODEL_PRIMARY,
                MODEL_FALLBACK_LITE
            )
            isMultiTurnChat -> listOf(
                MODEL_PRIMARY,
                MODEL_GENERAL_CHAT,
                MODEL_FALLBACK_LITE
            )
            else -> listOf(
                MODEL_PRIMARY,
                MODEL_FALLBACK_LITE,
                MODEL_FAST_LITE
            )
        }.distinct()

        return if (autoFallback) ordered else ordered.take(1)
    }

    fun resolveTtsChain(autoFallback: Boolean): List<String> {
        val chain = listOf(MODEL_TTS_PRIMARY, MODEL_TTS_FALLBACK)
        return if (autoFallback) chain else chain.take(1)
    }
}
