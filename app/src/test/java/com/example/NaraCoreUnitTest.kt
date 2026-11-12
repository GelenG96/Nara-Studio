package com.example

import com.example.ai.NaraModelRegistry
import com.example.ai.NaraResponseSanitizer
import com.example.data.model.AiEngineMode
import com.example.data.model.NaraAiTool
import com.example.editor.EditorSnapshot
import com.example.editor.RichTextEngine
import com.example.editor.UndoRedoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NaraCoreUnitTest {

    @Test
    fun richTextEngine_togglesAndAdjustsSpansOnEdit() {
        val text = "Senja di pelabuhan"
        val spans = RichTextEngine.toggleStyle(
            spans = emptyList(),
            textLength = text.length,
            selStart = 0,
            selEnd = 5,
            attr = RichTextEngine.SpanFormatAttr.BOLD
        )
        assertEquals(1, spans.size)
        assertTrue(spans[0].bold)
        assertEquals(0, spans[0].start)
        assertEquals(5, spans[0].end)

        val edited = "Malam Senja di pelabuhan"
        val adjusted = RichTextEngine.adjustSpansOnTextChange(
            oldText = text,
            newText = edited,
            oldSpans = spans,
            activeBold = false,
            activeItalic = false,
            activeUnderline = false
        )
        assertEquals(1, adjusted.size)
        assertEquals(6, adjusted[0].start)
        assertEquals(11, adjusted[0].end)

        val json = RichTextEngine.serializeSpans(adjusted)
        val restored = RichTextEngine.deserializeSpans(json)
        assertEquals(adjusted, restored)
    }

    @Test
    fun richTextEngine_countsWordsAndParagraphsAccurately() {
        val sample = "Paragraf pertama punya empat kata.\n\nParagraf kedua lima kata sekarang."
        assertEquals(10, RichTextEngine.countWords(sample))
        assertEquals(2, RichTextEngine.countParagraphs(sample))
    }

    @Test
    fun undoRedoManager_restoresSnapshotsCorrectly() {
        val mgr = UndoRedoManager()
        mgr.clear(EditorSnapshot("Satu", emptyList(), 4, 4))
        mgr.push(EditorSnapshot("Satu dua", emptyList(), 8, 8))
        mgr.push(EditorSnapshot("Satu dua tiga", emptyList(), 13, 13))

        assertTrue(mgr.canUndo())
        assertFalse(mgr.canRedo())

        val prev = mgr.undo()
        assertNotNull(prev)
        assertEquals("Satu dua", prev?.text)
        assertTrue(mgr.canRedo())

        val redone = mgr.redo()
        assertEquals("Satu dua tiga", redone?.text)
    }

    @Test
    fun responseSanitizer_stripsMarkdownHeadingsCodeFencesAndProviderLeaks() {
        val raw = """
            ### Evaluasi Bab
            ```markdown
            Bagian pembuka sudah kuat.
            ```
            Saya sebagai Gemini dari Google AI menyarankan dialog lebih ringkas.
        """.trimIndent()
        val clean = NaraResponseSanitizer.sanitize(raw)
        assertFalse(clean.contains("###"))
        assertFalse(clean.contains("```"))
        assertFalse(clean.contains("Gemini", ignoreCase = true))
        assertFalse(clean.contains("Google AI", ignoreCase = true))
        assertTrue(clean.contains("Evaluasi Bab"))
    }

    @Test
    fun modelRegistry_resolvesPriorityAndFallbackModels() {
        val standardChain = NaraModelRegistry.resolveModelChain(AiEngineMode.STANDARD, null, false, true)
        assertEquals("gemini-3.8-flash", standardChain.first())
        assertTrue(standardChain.contains("gemini-3.5-flash-lite"))

        val deepChain = NaraModelRegistry.resolveModelChain(AiEngineMode.STANDARD, NaraAiTool.CONSISTENCY_CHECK, false, true)
        assertEquals("gemini-4-carbon", deepChain.first())

        val ttsChain = NaraModelRegistry.resolveTtsChain(true)
        assertEquals("gemini-3.8-flash-tts", ttsChain.first())
    }
}
