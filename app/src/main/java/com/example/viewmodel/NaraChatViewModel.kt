package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.ai.NaraPromptBuilder
import com.example.ai.StreamEvent
import com.example.data.local.ChapterEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.NovelEntity
import com.example.data.local.StoryBibleEntryEntity
import com.example.data.model.AiContextScope
import com.example.data.model.EditingIntensity
import com.example.data.model.NaraAiTool
import com.example.editor.RichTextEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NaraStudioUiState(
    val selectedNovelId: Long? = null,
    val selectedChapterId: Long? = null,
    val selectedMultiChapterIds: Set<Long> = emptySet(),
    val contextScope: AiContextScope = AiContextScope.ACTIVE_CHAPTER,
    val editingIntensity: EditingIntensity = EditingIntensity.PROFESSIONAL,
    val selectedTextSnippet: String = "",
    val isStreaming: Boolean = false,
    val feedbackBanner: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class NaraChatViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.repository
    private val prefsRepo = container.preferencesRepository
    private val aiClient = container.aiClient

    private val _studioState = MutableStateFlow(NaraStudioUiState())
    val studioState: StateFlow<NaraStudioUiState> = _studioState.asStateFlow()

    val novels: StateFlow<List<NovelEntity>> = repo.activeNovels
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val selectedNovelIdFlow = MutableStateFlow<Long?>(null)

    val chaptersForSelectedNovel: StateFlow<List<ChapterEntity>> = selectedNovelIdFlow
        .flatMapLatest { id -> if (id != null) repo.observeChapters(id) else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val storyBibleForSelectedNovel: StateFlow<List<StoryBibleEntryEntity>> = selectedNovelIdFlow
        .flatMapLatest { id -> if (id != null) repo.observeStoryBible(id) else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val messages: StateFlow<List<ChatMessageEntity>> = selectedNovelIdFlow
        .flatMapLatest { id -> repo.observeChatMessages(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var activeStreamJob: Job? = null

    fun syncFromGlobalContext(novelId: Long?, chapterId: Long?, snippet: String) {
        val current = _studioState.value
        val resolvedNovel = novelId ?: current.selectedNovelId ?: novels.value.firstOrNull()?.id
        if (resolvedNovel != current.selectedNovelId) {
            selectedNovelIdFlow.value = resolvedNovel
        }
        _studioState.value = current.copy(
            selectedNovelId = resolvedNovel,
            selectedChapterId = chapterId ?: current.selectedChapterId,
            selectedTextSnippet = if (snippet.isNotBlank()) snippet else current.selectedTextSnippet
        )
    }

    fun selectNovel(novelId: Long?) {
        selectedNovelIdFlow.value = novelId
        _studioState.value = _studioState.value.copy(
            selectedNovelId = novelId,
            selectedChapterId = null,
            selectedMultiChapterIds = emptySet()
        )
    }

    fun selectChapter(chapterId: Long?) {
        _studioState.value = _studioState.value.copy(selectedChapterId = chapterId)
    }

    fun toggleMultiChapter(chapterId: Long) {
        val cur = _studioState.value.selectedMultiChapterIds
        val next = if (chapterId in cur) cur - chapterId else cur + chapterId
        _studioState.value = _studioState.value.copy(selectedMultiChapterIds = next)
    }

    fun setContextScope(scope: AiContextScope) {
        _studioState.value = _studioState.value.copy(contextScope = scope)
    }

    fun setEditingIntensity(intensity: EditingIntensity) {
        _studioState.value = _studioState.value.copy(editingIntensity = intensity)
    }

    fun setSelectedTextSnippet(snippet: String) {
        _studioState.value = _studioState.value.copy(selectedTextSnippet = snippet)
    }

    fun sendUserMessage(messageText: String, tool: NaraAiTool? = null) {
        val cleaned = messageText.trim()
        if (cleaned.isEmpty() || _studioState.value.isStreaming) return

        activeStreamJob?.cancel()
        activeStreamJob = viewModelScope.launch {
            val st = _studioState.value
            val prefs = prefsRepo.state.first()
            val novel = st.selectedNovelId?.let { repo.getNovelOnce(it) }
            val chapters = st.selectedNovelId?.let { repo.getChaptersForNovelOnce(it) }.orEmpty()
            val bible = st.selectedNovelId?.let { repo.getStoryBibleForNovelOnce(it) }.orEmpty()
            val activeChapter = chapters.firstOrNull { it.id == st.selectedChapterId } ?: chapters.firstOrNull()
            val multiChapters = chapters.filter { it.id in st.selectedMultiChapterIds }

            val contextBlock = NaraPromptBuilder.buildContextBlock(
                scope = st.contextScope,
                selectedText = st.selectedTextSnippet,
                activeChapter = activeChapter,
                selectedChapters = multiChapters,
                allChapters = chapters
            )

            val systemInstruction = NaraPromptBuilder.buildSystemInstruction(
                authorPenName = prefs.profile.penName,
                novel = novel,
                storyBible = bible,
                contextTextBlock = contextBlock,
                intensity = st.editingIntensity
            )

            val historySnapshot = messages.value
            val displayUserText = if (tool != null && cleaned == tool.labelId) {
                "${tool.labelId} (${st.editingIntensity.labelId})"
            } else {
                cleaned
            }
            val actualPrompt = if (tool != null) {
                NaraPromptBuilder.buildToolPrompt(tool, st.editingIntensity, if (cleaned == tool.labelId) "" else cleaned)
            } else {
                cleaned
            }

            repo.insertChatMessage(
                novelId = st.selectedNovelId,
                role = "user",
                content = displayUserText,
                toolTag = tool?.labelId.orEmpty(),
                contextScopeLabel = st.contextScope.labelId
            )

            val placeholderId = repo.insertChatMessage(
                novelId = st.selectedNovelId,
                role = "model",
                content = "NARA sedang membaca naskah...",
                toolTag = tool?.labelId.orEmpty(),
                contextScopeLabel = st.contextScope.labelId
            )

            _studioState.value = _studioState.value.copy(isStreaming = true)

            aiClient.streamChatOrTool(
                systemInstruction = systemInstruction,
                history = historySnapshot,
                userPrompt = actualPrompt,
                engineMode = prefs.aiConfig.engineMode,
                tool = tool,
                autoFallback = prefs.aiConfig.autoFallbackEnabled,
                isMultiTurnChat = tool == null
            ).collect { ev ->
                when (ev) {
                    is StreamEvent.Chunk -> {
                        repo.updateChatMessage(placeholderId, ev.accumulatedText, isError = false)
                    }
                    is StreamEvent.Completed -> {
                        repo.updateChatMessage(placeholderId, ev.finalText, isError = false)
                        _studioState.value = _studioState.value.copy(isStreaming = false)
                    }
                    is StreamEvent.Error -> {
                        repo.updateChatMessage(placeholderId, ev.message, isError = true)
                        _studioState.value = _studioState.value.copy(isStreaming = false)
                    }
                }
            }
        }
    }

    fun sendReplyToActiveChapter(replyText: String, replaceExisting: Boolean) {
        viewModelScope.launch {
            val st = _studioState.value
            val chapters = chaptersForSelectedNovel.value
            val targetChapter = chapters.firstOrNull { it.id == st.selectedChapterId } ?: chapters.firstOrNull()
            if (targetChapter == null) {
                _studioState.value = st.copy(feedbackBanner = "Pilih proyek novel dan bab terlebih dahulu.")
                return@launch
            }
            repo.createManualRevisionSnapshot(targetChapter.id, "Sebelum Kirim dari Studio NARA")
            val newContent = if (replaceExisting) {
                replyText.trim()
            } else {
                if (targetChapter.content.isBlank()) replyText.trim()
                else "${targetChapter.content.trimEnd()}\n\n${replyText.trim()}"
            }
            val spans = RichTextEngine.normalizeSpans(
                RichTextEngine.deserializeSpans(targetChapter.richSpansJson),
                newContent.length
            )
            repo.saveChapterContent(
                chapterId = targetChapter.id,
                title = targetChapter.title,
                content = newContent,
                spans = spans,
                alignment = targetChapter.alignment,
                notes = targetChapter.notes,
                createRevisionLabel = "Disisipkan dari NARA"
            )
            _studioState.value = st.copy(
                feedbackBanner = "Teks berhasil dikirim ke \"${targetChapter.title}\""
            )
        }
    }

    fun clearCurrentConversation() {
        viewModelScope.launch {
            repo.clearChat(_studioState.value.selectedNovelId)
            _studioState.value = _studioState.value.copy(feedbackBanner = "Percakapan dibersihkan")
        }
    }

    fun clearBanner() {
        _studioState.value = _studioState.value.copy(feedbackBanner = null)
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NaraChatViewModel(container) as T
                }
            }
    }
}
