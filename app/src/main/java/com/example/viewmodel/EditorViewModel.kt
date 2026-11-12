package com.example.viewmodel

import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.ai.NaraPromptBuilder
import com.example.ai.StreamEvent
import com.example.data.local.ChapterEntity
import com.example.data.local.ChapterRevisionEntity
import com.example.data.local.NovelEntity
import com.example.data.local.StoryBibleEntryEntity
import com.example.data.model.AiContextScope
import com.example.data.model.EditingIntensity
import com.example.data.model.NaraAiTool
import com.example.data.model.RichSpan
import com.example.data.model.TextAlignOption
import com.example.editor.EditorSnapshot
import com.example.editor.RichTextEngine
import com.example.editor.UndoRedoManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class AutosaveStatus(val labelId: String) {
    SAVED("Tersimpan"),
    SAVING("Menyimpan..."),
    UNSAVED("Mengedit...")
}

data class EditorUiState(
    val novel: NovelEntity? = null,
    val chapter: ChapterEntity? = null,
    val allChapters: List<ChapterEntity> = emptyList(),
    val revisions: List<ChapterRevisionEntity> = emptyList(),
    val storyBible: List<StoryBibleEntryEntity> = emptyList(),
    val title: String = "",
    val textFieldValue: TextFieldValue = TextFieldValue(""),
    val spans: List<RichSpan> = emptyList(),
    val alignment: TextAlignOption = TextAlignOption.START,
    val notes: String = "",
    val activeBold: Boolean = false,
    val activeItalic: Boolean = false,
    val activeUnderline: Boolean = false,
    val wordCount: Int = 0,
    val charCount: Int = 0,
    val paragraphCount: Int = 0,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val autosaveStatus: AutosaveStatus = AutosaveStatus.SAVED,
    val isFocusMode: Boolean = false,
    val isFindReplaceVisible: Boolean = false,
    val findQuery: String = "",
    val replaceQuery: String = "",
    val matchIndices: List<Int> = emptyList(),
    val currentMatchIndex: Int = -1,
    val isCaseSensitiveSearch: Boolean = false,
    // Inline NARA AI Assist state
    val aiGenerating: Boolean = false,
    val aiResultText: String = "",
    val aiErrorMessage: String? = null,
    val aiLastTool: NaraAiTool? = null,
    val aiIntensity: EditingIntensity = EditingIntensity.PROFESSIONAL,
    val statusBanner: String? = null
)

class EditorViewModel(
    private val container: AppContainer,
    private val novelId: Long,
    initialChapterId: Long
) : ViewModel() {
    private val repo = container.repository
    private val prefsRepo = container.preferencesRepository
    private val aiClient = container.aiClient

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoRedo = UndoRedoManager()
    private var currentChapterId: Long = initialChapterId
    private var autosaveJob: Job? = null
    private var undoPushJob: Job? = null
    private var aiJob: Job? = null
    private var revisionsJob: Job? = null
    private var lastSnapshotTimestamp: Long = 0L

    init {
        viewModelScope.launch {
            launch {
                repo.observeNovel(novelId).collect { novel ->
                    _uiState.value = _uiState.value.copy(novel = novel)
                }
            }
            launch {
                repo.observeChapters(novelId).collect { chapters ->
                    _uiState.value = _uiState.value.copy(allChapters = chapters)
                }
            }
            launch {
                repo.observeStoryBible(novelId).collect { bible ->
                    _uiState.value = _uiState.value.copy(storyBible = bible)
                }
            }
            loadChapter(initialChapterId)
        }
    }

    fun loadChapter(chapterId: Long) {
        viewModelScope.launch {
            if (currentChapterId != chapterId && _uiState.value.chapter != null) {
                persistNow(snapshotLabel = null)
            }
            currentChapterId = chapterId
            val chapter = repo.getChapterOnce(chapterId) ?: return@launch
            val spans = RichTextEngine.deserializeSpans(chapter.richSpansJson)
            val tfv = TextFieldValue(
                text = chapter.content,
                selection = TextRange(chapter.content.length)
            )
            undoRedo.clear(EditorSnapshot(chapter.content, spans, tfv.selection.start, tfv.selection.end))
            lastSnapshotTimestamp = System.currentTimeMillis()

            _uiState.value = _uiState.value.copy(
                chapter = chapter,
                title = chapter.title,
                textFieldValue = tfv,
                spans = spans,
                alignment = TextAlignOption.fromName(chapter.alignment),
                notes = chapter.notes,
                wordCount = RichTextEngine.countWords(chapter.content),
                charCount = chapter.content.length,
                paragraphCount = RichTextEngine.countParagraphs(chapter.content),
                canUndo = false,
                canRedo = false,
                autosaveStatus = AutosaveStatus.SAVED,
                activeBold = false,
                activeItalic = false,
                activeUnderline = false
            )

            revisionsJob?.cancel()
            revisionsJob = viewModelScope.launch {
                repo.observeRevisions(chapterId).collect { revs ->
                    _uiState.value = _uiState.value.copy(revisions = revs)
                }
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.value = _uiState.value.copy(
            title = newTitle,
            autosaveStatus = AutosaveStatus.UNSAVED
        )
        scheduleAutosave()
    }

    fun onNotesChange(newNotes: String) {
        _uiState.value = _uiState.value.copy(
            notes = newNotes,
            autosaveStatus = AutosaveStatus.UNSAVED
        )
        scheduleAutosave()
    }

    fun onAlignmentChange(align: TextAlignOption) {
        _uiState.value = _uiState.value.copy(
            alignment = align,
            autosaveStatus = AutosaveStatus.UNSAVED
        )
        scheduleAutosave()
    }

    fun onTextFieldChange(newValue: TextFieldValue) {
        val state = _uiState.value
        val oldText = state.textFieldValue.text
        val newText = newValue.text

        val newSpans = if (oldText != newText) {
            RichTextEngine.adjustSpansOnTextChange(
                oldText = oldText,
                newText = newText,
                oldSpans = state.spans,
                activeBold = state.activeBold,
                activeItalic = state.activeItalic,
                activeUnderline = state.activeUnderline
            )
        } else {
            state.spans
        }

        // Update active formatting indicators when selection changes without text edit
        val sel = newValue.selection
        val (boldActive, italicActive, underlineActive) = if (!sel.collapsed) {
            Triple(
                RichTextEngine.hasStyleInSelection(newSpans, sel.min, sel.max, RichTextEngine.SpanFormatAttr.BOLD),
                RichTextEngine.hasStyleInSelection(newSpans, sel.min, sel.max, RichTextEngine.SpanFormatAttr.ITALIC),
                RichTextEngine.hasStyleInSelection(newSpans, sel.min, sel.max, RichTextEngine.SpanFormatAttr.UNDERLINE)
            )
        } else if (oldText == newText) {
            val probe = (sel.start - 1).coerceAtLeast(0)
            if (sel.start > 0 && newText.isNotEmpty()) {
                Triple(
                    RichTextEngine.hasStyleInSelection(newSpans, probe, probe + 1, RichTextEngine.SpanFormatAttr.BOLD),
                    RichTextEngine.hasStyleInSelection(newSpans, probe, probe + 1, RichTextEngine.SpanFormatAttr.ITALIC),
                    RichTextEngine.hasStyleInSelection(newSpans, probe, probe + 1, RichTextEngine.SpanFormatAttr.UNDERLINE)
                )
            } else {
                Triple(state.activeBold, state.activeItalic, state.activeUnderline)
            }
        } else {
            Triple(state.activeBold, state.activeItalic, state.activeUnderline)
        }

        val changed = oldText != newText
        _uiState.value = state.copy(
            textFieldValue = newValue,
            spans = newSpans,
            activeBold = boldActive,
            activeItalic = italicActive,
            activeUnderline = underlineActive,
            wordCount = if (changed) RichTextEngine.countWords(newText) else state.wordCount,
            charCount = newText.length,
            paragraphCount = if (changed) RichTextEngine.countParagraphs(newText) else state.paragraphCount,
            autosaveStatus = if (changed) AutosaveStatus.UNSAVED else state.autosaveStatus
        )

        if (changed) {
            scheduleUndoPush(newText, newSpans, newValue.selection)
            scheduleAutosave()
            if (state.isFindReplaceVisible && state.findQuery.isNotEmpty()) {
                recomputeMatches()
            }
        }
    }

    fun toggleFormatting(attr: RichTextEngine.SpanFormatAttr) {
        val state = _uiState.value
        val sel = state.textFieldValue.selection
        if (!sel.collapsed) {
            val updated = RichTextEngine.toggleStyle(
                spans = state.spans,
                textLength = state.textFieldValue.text.length,
                selStart = sel.min,
                selEnd = sel.max,
                attr = attr
            )
            undoRedo.push(EditorSnapshot(state.textFieldValue.text, updated, sel.start, sel.end))
            _uiState.value = state.copy(
                spans = updated,
                activeBold = RichTextEngine.hasStyleInSelection(updated, sel.min, sel.max, RichTextEngine.SpanFormatAttr.BOLD),
                activeItalic = RichTextEngine.hasStyleInSelection(updated, sel.min, sel.max, RichTextEngine.SpanFormatAttr.ITALIC),
                activeUnderline = RichTextEngine.hasStyleInSelection(updated, sel.min, sel.max, RichTextEngine.SpanFormatAttr.UNDERLINE),
                canUndo = undoRedo.canUndo(),
                canRedo = undoRedo.canRedo(),
                autosaveStatus = AutosaveStatus.UNSAVED
            )
            scheduleAutosave()
        } else {
            _uiState.value = when (attr) {
                RichTextEngine.SpanFormatAttr.BOLD -> state.copy(activeBold = !state.activeBold)
                RichTextEngine.SpanFormatAttr.ITALIC -> state.copy(activeItalic = !state.activeItalic)
                RichTextEngine.SpanFormatAttr.UNDERLINE -> state.copy(activeUnderline = !state.activeUnderline)
            }
        }
    }

    private fun scheduleUndoPush(text: String, spans: List<RichSpan>, sel: TextRange) {
        undoPushJob?.cancel()
        undoPushJob = viewModelScope.launch {
            delay(450)
            undoRedo.push(EditorSnapshot(text, spans, sel.start, sel.end))
            _uiState.value = _uiState.value.copy(
                canUndo = undoRedo.canUndo(),
                canRedo = undoRedo.canRedo()
            )
        }
    }

    fun undo() {
        val snap = undoRedo.undo() ?: return
        applySnapshot(snap)
    }

    fun redo() {
        val snap = undoRedo.redo() ?: return
        applySnapshot(snap)
    }

    private fun applySnapshot(snap: EditorSnapshot) {
        val len = snap.text.length
        _uiState.value = _uiState.value.copy(
            textFieldValue = TextFieldValue(
                text = snap.text,
                selection = TextRange(snap.selectionStart.coerceIn(0, len), snap.selectionEnd.coerceIn(0, len))
            ),
            spans = snap.spans,
            wordCount = RichTextEngine.countWords(snap.text),
            charCount = len,
            paragraphCount = RichTextEngine.countParagraphs(snap.text),
            canUndo = undoRedo.canUndo(),
            canRedo = undoRedo.canRedo(),
            autosaveStatus = AutosaveStatus.UNSAVED
        )
        scheduleAutosave()
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            val intervalMs = (prefsRepo.state.first().editorSettings.autoSaveIntervalSec * 1000L).coerceIn(2000L, 15000L)
            delay(intervalMs)
            val now = System.currentTimeMillis()
            val label = if (now - lastSnapshotTimestamp > 90_000L) "Autosave berkala" else null
            persistNow(label)
        }
    }

    fun persistNow(snapshotLabel: String? = null) {
        val state = _uiState.value
        val chId = state.chapter?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(autosaveStatus = AutosaveStatus.SAVING)
            repo.saveChapterContent(
                chapterId = chId,
                title = state.title,
                content = state.textFieldValue.text,
                spans = state.spans,
                alignment = state.alignment.name,
                notes = state.notes,
                createRevisionLabel = snapshotLabel
            )
            if (snapshotLabel != null) lastSnapshotTimestamp = System.currentTimeMillis()
            _uiState.value = _uiState.value.copy(autosaveStatus = AutosaveStatus.SAVED)
        }
    }

    fun createManualSnapshot(label: String = "Snapshot Manual") {
        viewModelScope.launch {
            val state = _uiState.value
            val chId = state.chapter?.id ?: return@launch
            repo.saveChapterContent(
                chapterId = chId,
                title = state.title,
                content = state.textFieldValue.text,
                spans = state.spans,
                alignment = state.alignment.name,
                notes = state.notes,
                createRevisionLabel = label
            )
            lastSnapshotTimestamp = System.currentTimeMillis()
            _uiState.value = _uiState.value.copy(
                autosaveStatus = AutosaveStatus.SAVED,
                statusBanner = "Versi revisi tersimpan"
            )
        }
    }

    fun restoreRevision(rev: ChapterRevisionEntity) {
        val state = _uiState.value
        // Save current before overwriting
        viewModelScope.launch {
            state.chapter?.id?.let { chId ->
                repo.createManualRevisionSnapshot(chId, "Sebelum Pulihkan Revisi")
            }
            val restoredSpans = RichTextEngine.deserializeSpans(rev.richSpansJson)
            val tfv = TextFieldValue(rev.contentSnapshot, TextRange(rev.contentSnapshot.length))
            undoRedo.push(EditorSnapshot(rev.contentSnapshot, restoredSpans, tfv.selection.start, tfv.selection.end))
            _uiState.value = _uiState.value.copy(
                title = rev.titleSnapshot,
                textFieldValue = tfv,
                spans = restoredSpans,
                wordCount = RichTextEngine.countWords(rev.contentSnapshot),
                charCount = rev.contentSnapshot.length,
                paragraphCount = RichTextEngine.countParagraphs(rev.contentSnapshot),
                canUndo = undoRedo.canUndo(),
                canRedo = undoRedo.canRedo(),
                statusBanner = "Revisi dipulihkan (${rev.wordCount} kata)"
            )
            persistNow(null)
        }
    }

    fun toggleFocusMode() {
        _uiState.value = _uiState.value.copy(isFocusMode = !_uiState.value.isFocusMode)
    }

    // Find & Replace
    fun toggleFindReplace() {
        val next = !_uiState.value.isFindReplaceVisible
        _uiState.value = _uiState.value.copy(isFindReplaceVisible = next)
        if (next) recomputeMatches()
    }

    fun updateFindQuery(q: String) {
        _uiState.value = _uiState.value.copy(findQuery = q)
        recomputeMatches()
    }

    fun updateReplaceQuery(q: String) {
        _uiState.value = _uiState.value.copy(replaceQuery = q)
    }

    fun toggleCaseSensitiveSearch() {
        _uiState.value = _uiState.value.copy(isCaseSensitiveSearch = !_uiState.value.isCaseSensitiveSearch)
        recomputeMatches()
    }

    private fun recomputeMatches() {
        val state = _uiState.value
        val q = state.findQuery
        if (q.isEmpty()) {
            _uiState.value = state.copy(matchIndices = emptyList(), currentMatchIndex = -1)
            return
        }
        val text = state.textFieldValue.text
        val indices = mutableListOf<Int>()
        var idx = text.indexOf(q, startIndex = 0, ignoreCase = !state.isCaseSensitiveSearch)
        while (idx >= 0) {
            indices.add(idx)
            idx = text.indexOf(q, startIndex = idx + q.length.coerceAtLeast(1), ignoreCase = !state.isCaseSensitiveSearch)
        }
        _uiState.value = state.copy(
            matchIndices = indices,
            currentMatchIndex = if (indices.isNotEmpty()) 0 else -1
        )
    }

    fun findNext() {
        val state = _uiState.value
        if (state.matchIndices.isEmpty()) return
        val next = (state.currentMatchIndex + 1) % state.matchIndices.size
        val pos = state.matchIndices[next]
        _uiState.value = state.copy(
            currentMatchIndex = next,
            textFieldValue = state.textFieldValue.copy(
                selection = TextRange(pos, (pos + state.findQuery.length).coerceAtMost(state.textFieldValue.text.length))
            )
        )
    }

    fun findPrev() {
        val state = _uiState.value
        if (state.matchIndices.isEmpty()) return
        val prev = if (state.currentMatchIndex <= 0) state.matchIndices.lastIndex else state.currentMatchIndex - 1
        val pos = state.matchIndices[prev]
        _uiState.value = state.copy(
            currentMatchIndex = prev,
            textFieldValue = state.textFieldValue.copy(
                selection = TextRange(pos, (pos + state.findQuery.length).coerceAtMost(state.textFieldValue.text.length))
            )
        )
    }

    fun replaceCurrent() {
        val state = _uiState.value
        val q = state.findQuery
        if (q.isEmpty() || state.matchIndices.isEmpty()) return
        val idx = state.matchIndices.getOrNull(state.currentMatchIndex.coerceAtLeast(0)) ?: return
        val oldText = state.textFieldValue.text
        if (idx + q.length > oldText.length) return
        val newText = oldText.replaceRange(idx, idx + q.length, state.replaceQuery)
        val newSpans = RichTextEngine.adjustSpansOnTextChange(oldText, newText, state.spans, false, false, false)
        val cursor = idx + state.replaceQuery.length
        undoRedo.push(EditorSnapshot(newText, newSpans, cursor, cursor))
        _uiState.value = state.copy(
            textFieldValue = TextFieldValue(newText, TextRange(cursor)),
            spans = newSpans,
            wordCount = RichTextEngine.countWords(newText),
            charCount = newText.length,
            paragraphCount = RichTextEngine.countParagraphs(newText),
            canUndo = undoRedo.canUndo(),
            canRedo = undoRedo.canRedo(),
            autosaveStatus = AutosaveStatus.UNSAVED
        )
        recomputeMatches()
        scheduleAutosave()
    }

    fun replaceAll() {
        val state = _uiState.value
        val q = state.findQuery
        if (q.isEmpty() || state.matchIndices.isEmpty()) return
        val oldText = state.textFieldValue.text
        val newText = oldText.replace(q, state.replaceQuery, ignoreCase = !state.isCaseSensitiveSearch)
        val newSpans = RichTextEngine.normalizeSpans(state.spans, newText.length)
        undoRedo.push(EditorSnapshot(newText, newSpans, 0, 0))
        val count = state.matchIndices.size
        _uiState.value = state.copy(
            textFieldValue = TextFieldValue(newText, TextRange(newText.length)),
            spans = newSpans,
            wordCount = RichTextEngine.countWords(newText),
            charCount = newText.length,
            paragraphCount = RichTextEngine.countParagraphs(newText),
            canUndo = undoRedo.canUndo(),
            canRedo = undoRedo.canRedo(),
            autosaveStatus = AutosaveStatus.UNSAVED,
            statusBanner = "$count kata berhasil diganti"
        )
        recomputeMatches()
        scheduleAutosave()
    }

    // Inline NARA AI Assist
    fun setAiIntensity(intensity: EditingIntensity) {
        _uiState.value = _uiState.value.copy(aiIntensity = intensity)
    }

    fun runInlineAiTool(tool: NaraAiTool, customInstruction: String = "") {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            val state = _uiState.value
            val prefs = prefsRepo.state.first()
            val sel = state.textFieldValue.selection
            val selectedText = if (!sel.collapsed && sel.max <= state.textFieldValue.text.length) {
                state.textFieldValue.text.substring(sel.min, sel.max)
            } else ""

            val scope = if (selectedText.isNotBlank()) AiContextScope.SELECTION else AiContextScope.ACTIVE_CHAPTER
            val virtualChapter = state.chapter?.copy(
                title = state.title,
                content = state.textFieldValue.text,
                notes = state.notes,
                wordCount = state.wordCount
            )
            val contextBlock = NaraPromptBuilder.buildContextBlock(
                scope = scope,
                selectedText = selectedText,
                activeChapter = virtualChapter,
                selectedChapters = emptyList(),
                allChapters = state.allChapters
            )
            val systemInstruction = NaraPromptBuilder.buildSystemInstruction(
                authorPenName = prefs.profile.penName,
                novel = state.novel,
                storyBible = state.storyBible,
                contextTextBlock = contextBlock,
                intensity = state.aiIntensity
            )
            val prompt = NaraPromptBuilder.buildToolPrompt(tool, state.aiIntensity, customInstruction)

            _uiState.value = state.copy(
                aiGenerating = true,
                aiResultText = "",
                aiErrorMessage = null,
                aiLastTool = tool
            )

            aiClient.streamChatOrTool(
                systemInstruction = systemInstruction,
                history = emptyList(),
                userPrompt = prompt,
                engineMode = prefs.aiConfig.engineMode,
                tool = tool,
                autoFallback = prefs.aiConfig.autoFallbackEnabled,
                isMultiTurnChat = false
            ).collect { ev ->
                when (ev) {
                    is StreamEvent.Chunk -> {
                        _uiState.value = _uiState.value.copy(aiResultText = ev.accumulatedText)
                    }
                    is StreamEvent.Completed -> {
                        _uiState.value = _uiState.value.copy(
                            aiGenerating = false,
                            aiResultText = ev.finalText
                        )
                    }
                    is StreamEvent.Error -> {
                        _uiState.value = _uiState.value.copy(
                            aiGenerating = false,
                            aiErrorMessage = ev.message
                        )
                    }
                }
            }
        }
    }

    fun applyAiResultToEditor(replaceSelectionOrAll: Boolean) {
        val state = _uiState.value
        val result = state.aiResultText.trim()
        if (result.isEmpty()) return

        viewModelScope.launch {
            state.chapter?.id?.let { repo.createManualRevisionSnapshot(it, "Sebelum Terapkan NARA") }

            val oldText = state.textFieldValue.text
            val sel = state.textFieldValue.selection
            val newText: String
            val newCursor: Int
            if (replaceSelectionOrAll) {
                if (!sel.collapsed && sel.max <= oldText.length) {
                    newText = oldText.replaceRange(sel.min, sel.max, result)
                    newCursor = sel.min + result.length
                } else {
                    newText = result
                    newCursor = result.length
                }
            } else {
                // Append / insert at cursor or end
                val insertAt = sel.max.coerceIn(0, oldText.length)
                val prefix = if (insertAt > 0 && oldText[insertAt - 1] != '\n') "\n\n" else ""
                newText = oldText.substring(0, insertAt) + prefix + result + oldText.substring(insertAt)
                newCursor = insertAt + prefix.length + result.length
            }

            val newSpans = RichTextEngine.adjustSpansOnTextChange(oldText, newText, state.spans, false, false, false)
            undoRedo.push(EditorSnapshot(newText, newSpans, newCursor, newCursor))
            _uiState.value = _uiState.value.copy(
                textFieldValue = TextFieldValue(newText, TextRange(newCursor)),
                spans = newSpans,
                wordCount = RichTextEngine.countWords(newText),
                charCount = newText.length,
                paragraphCount = RichTextEngine.countParagraphs(newText),
                canUndo = undoRedo.canUndo(),
                canRedo = undoRedo.canRedo(),
                statusBanner = "Hasil NARA diterapkan ke editor"
            )
            persistNow("Setelah Suntingan NARA")
        }
    }

    // Export / Import single chapter
    fun exportChapterToUri(uri: Uri, format: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val ch = state.chapter?.copy(
                title = state.title,
                content = state.textFieldValue.text,
                richSpansJson = RichTextEngine.serializeSpans(state.spans)
            ) ?: return@launch
            val output = when (format) {
                "html" -> repo.backupManager.exportChapterAsHtml(ch)
                "md" -> "${ch.title}\n\n${ch.content}"
                else -> "${ch.title}\n\n${ch.content}"
            }
            val ok = repo.backupManager.writeTextToUri(uri, output)
            _uiState.value = _uiState.value.copy(
                statusBanner = if (ok) "Bab berhasil diekspor" else "Gagal mengekspor bab"
            )
        }
    }

    fun importTextIntoChapter(uri: Uri) {
        viewModelScope.launch {
            val imported = repo.backupManager.readTextFromUri(uri)
            if (imported == null) {
                _uiState.value = _uiState.value.copy(statusBanner = "Gagal membaca dokumen")
                return@launch
            }
            val state = _uiState.value
            state.chapter?.id?.let { repo.createManualRevisionSnapshot(it, "Sebelum Impor Dokumen") }
            val merged = if (state.textFieldValue.text.isBlank()) imported else "${state.textFieldValue.text}\n\n$imported"
            undoRedo.push(EditorSnapshot(merged, state.spans, merged.length, merged.length))
            _uiState.value = state.copy(
                textFieldValue = TextFieldValue(merged, TextRange(merged.length)),
                wordCount = RichTextEngine.countWords(merged),
                charCount = merged.length,
                paragraphCount = RichTextEngine.countParagraphs(merged),
                canUndo = undoRedo.canUndo(),
                canRedo = undoRedo.canRedo(),
                statusBanner = "Dokumen berhasil diimpor"
            )
            persistNow("Impor Dokumen")
        }
    }

    fun clearBanner() {
        _uiState.value = _uiState.value.copy(statusBanner = null)
    }

    override fun onCleared() {
        super.onCleared()
        autosaveJob?.cancel()
        val state = _uiState.value
        val chId = state.chapter?.id
        if (chId != null && state.autosaveStatus != AutosaveStatus.SAVED) {
            // Fire and forget on application scope is avoided; best effort synchronous trigger via MainAppViewModel or coroutine
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                repo.saveChapterContent(
                    chapterId = chId,
                    title = state.title,
                    content = state.textFieldValue.text,
                    spans = state.spans,
                    alignment = state.alignment.name,
                    notes = state.notes,
                    createRevisionLabel = null
                )
            }
        }
    }

    companion object {
        fun factory(
            container: AppContainer,
            novelId: Long,
            chapterId: Long
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EditorViewModel(container, novelId, chapterId) as T
            }
        }
    }
}
