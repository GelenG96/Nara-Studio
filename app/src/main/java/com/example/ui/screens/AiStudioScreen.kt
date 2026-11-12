package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.TtsUiState
import com.example.data.local.ChapterEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.NovelEntity
import com.example.data.local.StoryBibleEntryEntity
import com.example.data.model.AiContextScope
import com.example.data.model.EditingIntensity
import com.example.data.model.NaraAiTool
import com.example.data.model.WallpaperPageTarget
import com.example.data.prefs.ApiKeyState
import com.example.data.prefs.AppPreferencesState
import com.example.ui.components.NaraPanelCard
import com.example.ui.components.NaraTtsFloatingBar
import com.example.ui.components.WallpaperBackdrop
import com.example.viewmodel.NaraStudioUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiStudioScreen(
    preferences: AppPreferencesState,
    apiKeyState: ApiKeyState,
    studioState: NaraStudioUiState,
    novels: List<NovelEntity>,
    chapters: List<ChapterEntity>,
    storyBible: List<StoryBibleEntryEntity>,
    messages: List<ChatMessageEntity>,
    ttsState: TtsUiState,
    onSelectNovel: (Long?) -> Unit,
    onSelectChapter: (Long?) -> Unit,
    onToggleMultiChapter: (Long) -> Unit,
    onSetContextScope: (AiContextScope) -> Unit,
    onSetEditingIntensity: (EditingIntensity) -> Unit,
    onSetSnippet: (String) -> Unit,
    onSendMessage: (String, NaraAiTool?) -> Unit,
    onSendReplyToChapter: (String, Boolean) -> Unit,
    onClearChat: () -> Unit,
    onSpeakText: (String, String) -> Unit,
    onStopTts: () -> Unit,
    onDismissTtsError: () -> Unit,
    onOpenAiSettings: () -> Unit,
    onClearBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(studioState.feedbackBanner) {
        studioState.feedbackBanner?.let {
            snackbarHostState.showSnackbar(it)
            onClearBanner()
        }
    }

    var inputText by remember { mutableStateOf("") }
    var showToolsSheet by remember { mutableStateOf(false) }
    var showNovelDropdown by remember { mutableStateOf(false) }
    var showChapterPickerDialog by remember { mutableStateOf(false) }
    var showSnippetDialog by remember { mutableStateOf(false) }
    var sendTargetMessage by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val selectedNovel = novels.firstOrNull { it.id == studioState.selectedNovelId }
    val selectedChapter = chapters.firstOrNull { it.id == studioState.selectedChapterId } ?: chapters.firstOrNull()
    val wpConfig = preferences.resolveWallpaperFor(WallpaperPageTarget.AI)
    val clipboard = LocalClipboardManager.current

    WallpaperBackdrop(config = wpConfig, modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = wpConfig.panelOpacity),
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_nara_avatar),
                                contentDescription = "NARA",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "NARA",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (selectedNovel != null) {
                                        "${selectedNovel.title} • ${storyBible.size} referensi"
                                    } else {
                                        "Editor Novel & Teman Diskusi"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { showToolsSheet = true },
                                modifier = Modifier.testTag("ai_open_tools_button")
                            ) {
                                Icon(Icons.Rounded.AutoFixHigh, contentDescription = "Alat Editor NARA")
                            }
                            IconButton(
                                onClick = onClearChat,
                                modifier = Modifier.testTag("ai_clear_chat_button")
                            ) {
                                Icon(Icons.Rounded.DeleteSweep, contentDescription = "Bersihkan percakapan")
                            }
                        }

                        // Project & Context Bar
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Box {
                                    FilterChip(
                                        selected = selectedNovel != null,
                                        onClick = { showNovelDropdown = true },
                                        label = {
                                            Text(
                                                text = selectedNovel?.title ?: "Pilih Novel",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.MenuBook, null, modifier = Modifier.size(16.dp)) },
                                        trailingIcon = { Icon(Icons.Rounded.ExpandMore, null, modifier = Modifier.size(16.dp)) }
                                    )
                                    DropdownMenu(
                                        expanded = showNovelDropdown,
                                        onDismissRequest = { showNovelDropdown = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Diskusi Umum (Tanpa Proyek)") },
                                            onClick = {
                                                showNovelDropdown = false
                                                onSelectNovel(null)
                                            }
                                        )
                                        novels.filter { !it.isArchived }.forEach { n ->
                                            DropdownMenuItem(
                                                text = { Text(n.title) },
                                                onClick = {
                                                    showNovelDropdown = false
                                                    onSelectNovel(n.id)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            items(AiContextScope.entries) { scope ->
                                FilterChip(
                                    selected = studioState.contextScope == scope,
                                    onClick = {
                                        onSetContextScope(scope)
                                        if (scope == AiContextScope.ACTIVE_CHAPTER || scope == AiContextScope.MULTIPLE_CHAPTERS) {
                                            if (chapters.isNotEmpty()) showChapterPickerDialog = true
                                        } else if (scope == AiContextScope.SELECTION) {
                                            showSnippetDialog = true
                                        }
                                    },
                                    label = {
                                        val suffix = when {
                                            scope == AiContextScope.ACTIVE_CHAPTER && selectedChapter != null ->
                                                ": ${selectedChapter.title.take(14)}"
                                            scope == AiContextScope.MULTIPLE_CHAPTERS && studioState.selectedMultiChapterIds.isNotEmpty() ->
                                                " (${studioState.selectedMultiChapterIds.size})"
                                            else -> ""
                                        }
                                        Text(scope.labelId + suffix)
                                    }
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = wpConfig.panelOpacity),
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                ) {
                    Column {
                        NaraTtsFloatingBar(
                            ttsState = ttsState,
                            onStop = onStopTts,
                            onDismissError = onDismissTtsError
                        )
                        // Intensity strip
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(EditingIntensity.entries) { intensity ->
                                FilterChip(
                                    selected = studioState.editingIntensity == intensity,
                                    onClick = { onSetEditingIntensity(intensity) },
                                    label = { Text(intensity.labelId, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text("Ajak NARA diskusi, kritik bab, atau sunting adegan...") },
                                maxLines = 4,
                                shape = RoundedCornerShape(22.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_chat_input")
                            )
                            FilledIconButton(
                                onClick = {
                                    val msg = inputText.trim()
                                    if (msg.isNotEmpty()) {
                                        inputText = ""
                                        onSendMessage(msg, null)
                                    }
                                },
                                enabled = inputText.isNotBlank() && !studioState.isStreaming,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("ai_chat_send_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Kirim ke NARA")
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 860.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!apiKeyState.hasKey) {
                        item {
                            NaraPanelCard(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Key, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Kunci Akses AI Belum Terpasang",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    Text(
                                        text = "Masukkan kunci akses Anda di Pengaturan AI agar NARA bisa membaca, menyunting, dan berdiskusi secara langsung.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Button(
                                        onClick = onOpenAiSettings,
                                        modifier = Modifier.testTag("ai_setup_key_cta")
                                    ) {
                                        Text("Buka Pengaturan AI")
                                    }
                                }
                            }
                        }
                    }

                    if (messages.isEmpty()) {
                        item {
                            NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Hai, aku NARA.",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "Aku editor novel bawaanmu. Kirimkan adegan yang terasa mentok, minta kritik jujur soal pacing bab, atau cek konsistensi tokoh dengan catatan duniamu.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Akses Cepat Alat Editor:",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(
                                            listOf(
                                                NaraAiTool.PRO_EDIT,
                                                NaraAiTool.CHAPTER_SCORE,
                                                NaraAiTool.CONSISTENCY_CHECK,
                                                NaraAiTool.POLISH_DIALOGUE,
                                                NaraAiTool.BRAINSTORM
                                            )
                                        ) { tool ->
                                            OutlinedButton(
                                                onClick = { onSendMessage(tool.labelId, tool) }
                                            ) {
                                                Text(tool.labelId)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(messages, key = { it.id }) { msg ->
                            val isUser = msg.role == "user"
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                if (isUser) {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
                                        modifier = Modifier.widthIn(max = 560.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            if (msg.contextScopeLabel.isNotBlank()) {
                                                Text(
                                                    text = msg.contextScopeLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                            }
                                            Text(
                                                text = msg.content,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                } else {
                                    NaraPanelCard(
                                        containerColor = if (msg.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.widthIn(max = 640.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "NARA",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (msg.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (msg.toolTag.isNotBlank()) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "• ${msg.toolTag}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = msg.content,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (msg.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (!msg.isError && msg.content.length > 20 && !studioState.isStreaming) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    TextButton(
                                                        onClick = { onSpeakText(msg.content, "NARA") }
                                                    ) {
                                                        Icon(Icons.Rounded.VolumeUp, null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Bacakan")
                                                    }
                                                    TextButton(
                                                        onClick = { clipboard.setText(AnnotatedString(msg.content)) }
                                                    ) {
                                                        Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Salin")
                                                    }
                                                    if (selectedNovel != null && chapters.isNotEmpty()) {
                                                        TextButton(
                                                            onClick = { sendTargetMessage = msg.content }
                                                        ) {
                                                            Icon(Icons.Rounded.EditNote, null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Ke Bab")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 15 AI Tools Sheet
    if (showToolsSheet) {
        ModalBottomSheet(onDismissRequest = { showToolsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("15 Kemampuan Editor NARA", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Pilih alat untuk langsung dianalisis atau disunting sesuai konteks & tingkat penyuntingan aktif.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(NaraAiTool.entries) { tool ->
                        NaraPanelCard(
                            onClick = {
                                showToolsSheet = false
                                onSendMessage(tool.labelId, tool)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("nara_tool_${tool.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tool.labelId, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = tool.shortHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    // Chapter Picker for ACTIVE_CHAPTER or MULTIPLE_CHAPTERS
    if (showChapterPickerDialog) {
        val isMulti = studioState.contextScope == AiContextScope.MULTIPLE_CHAPTERS
        AlertDialog(
            onDismissRequest = { showChapterPickerDialog = false },
            title = { Text(if (isMulti) "Pilih Beberapa Bab" else "Pilih Bab Aktif") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(chapters, key = { it.id }) { ch ->
                        val checked = if (isMulti) ch.id in studioState.selectedMultiChapterIds else ch.id == selectedChapter?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isMulti) {
                                        onToggleMultiChapter(ch.id)
                                    } else {
                                        onSelectChapter(ch.id)
                                        showChapterPickerDialog = false
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    if (isMulti) onToggleMultiChapter(ch.id)
                                    else {
                                        onSelectChapter(ch.id)
                                        showChapterPickerDialog = false
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${ch.title} (${ch.wordCount} kata)", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showChapterPickerDialog = false }) { Text("Selesai") }
            }
        )
    }

    // Snippet input dialog when SELECTION scope is chosen
    if (showSnippetDialog) {
        var snippetText by remember { mutableStateOf(studioState.selectedTextSnippet) }
        AlertDialog(
            onDismissRequest = { showSnippetDialog = false },
            title = { Text("Teks Pilihan untuk NARA") },
            text = {
                OutlinedTextField(
                    value = snippetText,
                    onValueChange = { snippetText = it },
                    placeholder = { Text("Tempel paragraf atau adegan tertentu di sini...") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetSnippet(snippetText)
                        showSnippetDialog = false
                    }
                ) { Text("Gunakan Teks") }
            },
            dismissButton = {
                TextButton(onClick = { showSnippetDialog = false }) { Text("Batal") }
            }
        )
    }

    // Send NARA reply to active chapter dialog
    sendTargetMessage?.let { textToSend ->
        AlertDialog(
            onDismissRequest = { sendTargetMessage = null },
            title = { Text("Kirim ke Bab \"${selectedChapter?.title ?: ""}\"") },
            text = { Text("Pilih cara memasukkan hasil tulisan NARA ke dalam bab:") },
            confirmButton = {
                Button(
                    onClick = {
                        onSendReplyToChapter(textToSend, false)
                        sendTargetMessage = null
                    }
                ) { Text("Tambahkan di Akhir") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            onSendReplyToChapter(textToSend, true)
                            sendTargetMessage = null
                        }
                    ) { Text("Ganti Isi Bab") }
                    TextButton(onClick = { sendTargetMessage = null }) { Text("Batal") }
                }
            }
        )
    }
}
