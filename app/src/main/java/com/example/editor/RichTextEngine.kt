package com.example.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import com.example.data.model.RichSpan
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object RichTextEngine {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, RichSpan::class.java)
    private val adapter = moshi.adapter<List<RichSpan>>(listType)

    fun serializeSpans(spans: List<RichSpan>): String =
        runCatching { adapter.toJson(normalizeSpans(spans)) }.getOrDefault("[]")

    fun deserializeSpans(json: String?): List<RichSpan> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching { adapter.fromJson(json).orEmpty() }.getOrDefault(emptyList())
    }

    fun normalizeSpans(spans: List<RichSpan>, maxLength: Int = Int.MAX_VALUE): List<RichSpan> {
        return spans
            .mapNotNull { span ->
                val s = span.start.coerceAtLeast(0).coerceAtMost(maxLength)
                val e = span.end.coerceAtLeast(0).coerceAtMost(maxLength)
                if (e > s && (span.bold || span.italic || span.underline)) {
                    span.copy(start = s, end = e)
                } else null
            }
            .sortedWith(compareBy({ it.start }, { it.end }))
    }

    fun buildAnnotatedString(text: String, spans: List<RichSpan>): AnnotatedString {
        val builder = AnnotatedString.Builder(text)
        val len = text.length
        spans.forEach { span ->
            val start = span.start.coerceIn(0, len)
            val end = span.end.coerceIn(0, len)
            if (end > start) {
                builder.addStyle(
                    style = SpanStyle(
                        fontWeight = if (span.bold) FontWeight.Bold else null,
                        fontStyle = if (span.italic) FontStyle.Italic else null,
                        textDecoration = if (span.underline) TextDecoration.Underline else null
                    ),
                    start = start,
                    end = end
                )
            }
        }
        return builder.toAnnotatedString()
    }

    fun createVisualTransformation(spans: List<RichSpan>): VisualTransformation {
        return VisualTransformation { text ->
            TransformedText(
                buildAnnotatedString(text.text, spans),
                OffsetMapping.Identity
            )
        }
    }

    enum class SpanFormatAttr { BOLD, ITALIC, UNDERLINE }

    fun hasStyleInSelection(
        spans: List<RichSpan>,
        selStart: Int,
        selEnd: Int,
        attr: SpanFormatAttr
    ): Boolean {
        if (selEnd <= selStart) return false
        return spans.any { span ->
            span.start < selEnd && span.end > selStart && when (attr) {
                SpanFormatAttr.BOLD -> span.bold
                SpanFormatAttr.ITALIC -> span.italic
                SpanFormatAttr.UNDERLINE -> span.underline
            }
        }
    }

    fun toggleStyle(
        spans: List<RichSpan>,
        textLength: Int,
        selStart: Int,
        selEnd: Int,
        attr: SpanFormatAttr
    ): List<RichSpan> {
        val start = minOf(selStart, selEnd).coerceIn(0, textLength)
        val end = maxOf(selStart, selEnd).coerceIn(0, textLength)
        if (end <= start) return spans

        val currentlyActive = hasStyleInSelection(spans, start, end, attr)
        val targetValue = !currentlyActive

        // Collect all boundary points
        val points = sortedSetOf(0, textLength, start, end)
        spans.forEach {
            points.add(it.start.coerceIn(0, textLength))
            points.add(it.end.coerceIn(0, textLength))
        }
        val sortedPoints = points.toList()
        val result = mutableListOf<RichSpan>()

        for (i in 0 until sortedPoints.size - 1) {
            val segStart = sortedPoints[i]
            val segEnd = sortedPoints[i + 1]
            if (segEnd <= segStart) continue

            var bold = false
            var italic = false
            var underline = false
            spans.forEach { s ->
                if (s.start <= segStart && s.end >= segEnd) {
                    if (s.bold) bold = true
                    if (s.italic) italic = true
                    if (s.underline) underline = true
                }
            }
            if (segStart >= start && segEnd <= end) {
                when (attr) {
                    SpanFormatAttr.BOLD -> bold = targetValue
                    SpanFormatAttr.ITALIC -> italic = targetValue
                    SpanFormatAttr.UNDERLINE -> underline = targetValue
                }
            }
            if (bold || italic || underline) {
                val prev = result.lastOrNull()
                if (prev != null && prev.end == segStart && prev.bold == bold && prev.italic == italic && prev.underline == underline) {
                    result[result.lastIndex] = prev.copy(end = segEnd)
                } else {
                    result.add(RichSpan(segStart, segEnd, bold, italic, underline))
                }
            }
        }
        return result
    }

    fun adjustSpansOnTextChange(
        oldText: String,
        newText: String,
        oldSpans: List<RichSpan>,
        activeBold: Boolean,
        activeItalic: Boolean,
        activeUnderline: Boolean
    ): List<RichSpan> {
        if (oldText == newText) return oldSpans

        // Find common prefix and suffix
        val minLen = minOf(oldText.length, newText.length)
        var prefix = 0
        while (prefix < minLen && oldText[prefix] == newText[prefix]) {
            prefix++
        }
        var suffix = 0
        while (suffix < (minLen - prefix) &&
            oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]
        ) {
            suffix++
        }

        val deletedCount = oldText.length - prefix - suffix
        val insertedCount = newText.length - prefix - suffix
        val delta = insertedCount - deletedCount
        val editEndOld = prefix + deletedCount

        val adjusted = mutableListOf<RichSpan>()
        oldSpans.forEach { span ->
            when {
                span.end <= prefix -> {
                    adjusted.add(span)
                }
                span.start >= editEndOld -> {
                    adjusted.add(
                        span.copy(
                            start = (span.start + delta).coerceAtLeast(0),
                            end = (span.end + delta).coerceAtLeast(0)
                        )
                    )
                }
                else -> {
                    // Span overlaps edited region
                    val newStart = minOf(span.start, prefix)
                    val newEnd = (span.end + delta).coerceAtLeast(newStart)
                    if (newEnd > newStart) {
                        adjusted.add(span.copy(start = newStart, end = newEnd))
                    }
                }
            }
        }

        if (insertedCount > 0 && (activeBold || activeItalic || activeUnderline)) {
            adjusted.add(
                RichSpan(
                    start = prefix,
                    end = prefix + insertedCount,
                    bold = activeBold,
                    italic = activeItalic,
                    underline = activeUnderline
                )
            )
        }

        return mergeAdjacentSpans(normalizeSpans(adjusted, newText.length))
    }

    private fun mergeAdjacentSpans(spans: List<RichSpan>): List<RichSpan> {
        if (spans.isEmpty()) return emptyList()
        val out = mutableListOf<RichSpan>()
        for (s in spans) {
            val prev = out.lastOrNull()
            if (prev != null && prev.end >= s.start &&
                prev.bold == s.bold && prev.italic == s.italic && prev.underline == s.underline
            ) {
                out[out.lastIndex] = prev.copy(end = maxOf(prev.end, s.end))
            } else {
                out.add(s)
            }
        }
        return out
    }

    fun countWords(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return trimmed.split(Regex("\\s+")).count { it.isNotEmpty() }
    }

    fun countParagraphs(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return trimmed.split(Regex("\\n+")).count { it.isNotBlank() }
    }

    fun toHtml(title: String, content: String, spans: List<RichSpan>): String {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>")
        sb.append(escapeHtml(title))
        sb.append("</title><style>body{font-family:Georgia,serif;max-width:720px;margin:40px auto;line-height:1.7;padding:0 20px;color:#1c1b19;}h1{margin-bottom:24px;}</style></head><body>")
        sb.append("<h1>").append(escapeHtml(title)).append("</h1>")
        val normalized = normalizeSpans(spans, content.length)
        var cursor = 0
        // Render character by character with active span tags or per-segment
        val points = sortedSetOf(0, content.length)
        normalized.forEach {
            points.add(it.start)
            points.add(it.end)
        }
        val ptList = points.toList()
        sb.append("<p>")
        for (i in 0 until ptList.size - 1) {
            val s = ptList[i]
            val e = ptList[i + 1]
            if (e <= s) continue
            val chunk = content.substring(s, e)
            var bold = false
            var italic = false
            var underline = false
            normalized.forEach { sp ->
                if (sp.start <= s && sp.end >= e) {
                    if (sp.bold) bold = true
                    if (sp.italic) italic = true
                    if (sp.underline) underline = true
                }
            }
            if (bold) sb.append("<strong>")
            if (italic) sb.append("<em>")
            if (underline) sb.append("<u>")
            sb.append(escapeHtml(chunk).replace("\n", "</p><p>"))
            if (underline) sb.append("</u>")
            if (italic) sb.append("</em>")
            if (bold) sb.append("</strong>")
            cursor = e
        }
        if (cursor < content.length) {
            sb.append(escapeHtml(content.substring(cursor)).replace("\n", "</p><p>"))
        }
        sb.append("</p></body></html>")
        return sb.toString()
    }

    private fun escapeHtml(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}

data class EditorSnapshot(
    val text: String,
    val spans: List<RichSpan>,
    val selectionStart: Int,
    val selectionEnd: Int
)

class UndoRedoManager(private val maxHistory: Int = 80) {
    private val undoStack = ArrayDeque<EditorSnapshot>()
    private val redoStack = ArrayDeque<EditorSnapshot>()

    fun clear(initial: EditorSnapshot) {
        undoStack.clear()
        redoStack.clear()
        undoStack.addLast(initial)
    }

    fun push(snapshot: EditorSnapshot) {
        val last = undoStack.lastOrNull()
        if (last != null && last.text == snapshot.text && last.spans == snapshot.spans) return
        undoStack.addLast(snapshot)
        if (undoStack.size > maxHistory) {
            undoStack.removeFirst()
        }
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.size > 1
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo(): EditorSnapshot? {
        if (undoStack.size <= 1) return null
        val current = undoStack.removeLast()
        redoStack.addLast(current)
        return undoStack.lastOrNull()
    }

    fun redo(): EditorSnapshot? {
        if (redoStack.isEmpty()) return null
        val next = redoStack.removeLast()
        undoStack.addLast(next)
        return next
    }
}
