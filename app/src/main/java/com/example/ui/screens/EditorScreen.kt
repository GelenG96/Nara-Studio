package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.automirrored.rounded.FormatAlignRight
import androidx.compose.material.icons.automirrored.rounded.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FindReplace
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.FormatAlignJustify
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.FormatUnderlined
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.TtsUiState
import com.example.data.model.EditingIntensity
import com.example.data.model.EditorSettings
import com.example.data.model.NaraAiTool
import com.example.data.model.TextAlignOption
import com.example.data.model.WallpaperPageTarget
import com.example.data.prefs.AppPreferencesState
import com.example.editor.RichTextEngine
import com.example.ui.components.NaraPanelCard
import com.example.ui.components.NaraTtsFloatingBar
import com.example.ui.components.WallpaperBackdrop
import com.example.ui.theme.EditorFontChoice
import com.example.viewmodel.AutosaveStatus
import com.example.viewmodel.EditorUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    state: EditorUiState,
    preferences: AppPreferencesState,
    ttsState: TtsUiState,
    onBack: () -> Unit,
    onOpenAiStudio: (String) -> Unit,
    onSpeakText: (String, String) -> Unit,
    onStopTts: () -> Unit,
    onDismissTtsError: () -> Unit,
    onTitleChange: (String) -> Unit,
    onTextChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onToggleFormat: (RichTextEngine.SpanFormatAttr) -> Unit,
    onAlignmentChange: (TextAlignOption) -> Unit,
    onNotesChange: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onPersistNow: () -> Unit,
    onCreateSnapshot: () -> Unit,
    onRestoreRevision: (com.example.data.local.ChapterRevisionEntity) -> Unit,
    onSwitchChapter: (Long) -> Unit,
    onToggleFocusMode: () -> Unit,
    onToggleFindReplace: () -> Unit,
    onUpdateFindQuery: (String) -> Unit,
    onUpdateReplaceQuery: (String) -> Unit,
    onToggleCaseSensitive: () -> Unit,
    onFindNext: () -> Unit,
    onFindPrev: () -> Unit,
    onReplaceCurrent: () -> Unit,
    onReplaceAll: () -> Unit,
    onUpdateEditorSettings: (EditorSettings) -> Unit,
    onSetAiIntensity: (EditingIntensity) -> Unit,
    onRunAiTool: (NaraAiTool, String) -> Unit,
    onApplyAiResult: (Boolean) -> Unit,
    onExportChapter: (android.net.Uri, String) -> Unit,
    onImportIntoChapter: (android.net.Uri) -> Unit,
    onClearBanner: () -> Unit
) {
    BackHandler {
        if (state.isFocusMode) {
            onToggleFocusMode()
        } else {
            onPersistNow()
            onBack()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.statusBanner) {
        state.statusBanner?.let {
            snackbarHostState.showSnackbar(it)
            onClearBanner()
        }
    }

    var showMoreMenu by remember { mutableStateOf(false) }
    var showChapterSwitcher by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var showRevisionsSheet by remember { mutableStateOf(false) }
    var showTypographySheet by remember { mutableStateOf(false) }
    var showNaraSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var pendingExportFormat by remember { mutableStateOf("txt") }

    val exportChapterLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) onExportChapter(uri, pendingExportFormat)
    }
    val importChapterLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportIntoChapter(uri)
    }

    val wpConfig = preferences.resolveWallpaperFor(WallpaperPageTarget.EDITOR)
    val edSet = preferences.editorSettings
    val editorFont = remember(edSet.fontKey) { EditorFontChoice.fromKey(edSet.fontKey) }
    val visualTransformation = remember(state.spans) {
        RichTextEngine.createVisualTransformation(state.spans)
    }
    val textAlign = when (state.alignment) {
        TextAlignOption.START -> TextAlign.Start
        TextAlignOption.CENTER -> TextAlign.Center
        TextAlignOption.END -> TextAlign.End
        TextAlignOption.JUSTIFY -> TextAlign.Justify
    }

    WallpaperBackdrop(config = wpConfig) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                AnimatedVisibility(visible = !state.isFocusMode) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = wpConfig.panelOpacity),
                        tonalElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.statusBarsPadding()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        onPersistNow()
                                        onBack()
                                    },
                                    modifier = Modifier.testTag("editor_back_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showChapterSwitcher = true }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = state.title.ifBlank { "Tanpa Judul" },
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Icon(
                                            Icons.Rounded.ExpandMore,
                                            contentDescription = "Pindah bab",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "${state.novel?.title ?: ""} • ${state.autosaveStatus.labelId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (state.autosaveStatus == AutosaveStatus.SAVED) {
                                            MaterialTheme.colorScheme.tertiary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = onUndo,
                                    enabled = state.canUndo,
                                    modifier = Modifier.testTag("editor_undo_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Undo")
                                }
                                IconButton(
                                    onClick = onRedo,
                                    enabled = state.canRedo,
                                    modifier = Modifier.testTag("editor_redo_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Redo, contentDescription = "Redo")
                                }
                                IconButton(
                                    onClick = onToggleFocusMode,
                                    modifier = Modifier.testTag("editor_focus_mode_button")
                                ) {
                                    Icon(Icons.Rounded.CenterFocusStrong, contentDescription = "Mode Fokus")
                                }
                                IconButton(
                                    onClick = {
                                        val sel = state.textFieldValue.selection
                                        val snippet = if (!sel.collapsed && sel.max <= state.textFieldValue.text.length) {
                                            state.textFieldValue.text.substring(sel.min, sel.max)
                                        } else state.textFieldValue.text
                                        onSpeakText(snippet, state.title)
                                    },
                                    modifier = Modifier.testTag("editor_tts_button")
                                ) {
                                    Icon(Icons.Rounded.VolumeUp, contentDescription = "Bacakan bab dengan suara NARA")
                                }

                                Box {
                                    IconButton(onClick = { showMoreMenu = true }) {
                                        Icon(Icons.Rounded.MoreVert, contentDescription = "Menu editor")
                                    }
                                    DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                        DropdownMenuItem(
                                            text = { Text("Cari & Ganti Kata") },
                                            leadingIcon = { Icon(Icons.Rounded.FindReplace, null) },
                                            onClick = {
                                                showMoreMenu = false
                                                onToggleFindReplace()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Catatan Bab") },
                                            leadingIcon = { Icon(Icons.Rounded.NoteAlt, null) },
                                            onClick = {
                                                showMoreMenu = false
                                                showNotesSheet = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Riwayat Revisi (${state.revisions.size})") },
                                            leadingIcon = { Icon(Icons.Rounded.History, null) },
                                            onClick = {
                                                showMoreMenu = false
                                                showRevisionsSheet = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Simpan Snapshot Revisi") },
                                            leadingIcon = { Icon(Icons.Rounded.Save, null) },
                                            onClick = {
                                                showMoreMenu = false
                                                onCreateSnapshot()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Tipografi & Paragraf") },
                                            leadingIcon = { Icon(Icons.Rounded.FormatSize, null) },
                                            onClick = {
                                                showMoreMenu = false
                                                showTypographySheet = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Ekspor Bab (.txt/.md/.html)") },
                                            onClick = {
                                                showMoreMenu = false
                                                showExportDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Impor Dokumen ke Bab") },
                                            onClick = {
                                                showMoreMenu = false
                                                importChapterLauncher.launch(arrayOf("text/plain", "text/markdown", "*/*"))
                                            }
                                        )
                                    }
                                }
                            }

                            // Find & Replace Bar
                            AnimatedVisibility(visible = state.isFindReplaceVisible) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = state.findQuery,
                                            onValueChange = onUpdateFindQuery,
                                            placeholder = { Text("Cari kata...") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = if (state.matchIndices.isEmpty()) "0" else "${state.currentMatchIndex + 1}/${state.matchIndices.size}",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        IconButton(onClick = onFindPrev) {
                                            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Sebelumnya")
                                        }
                                        IconButton(onClick = onFindNext) {
                                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Berikutnya")
                                        }
                                        IconButton(onClick = onToggleFindReplace) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Tutup cari")
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = state.replaceQuery,
                                            onValueChange = onUpdateReplaceQuery,
                                            placeholder = { Text("Ganti dengan...") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = state.isCaseSensitiveSearch,
                                            onClick = onToggleCaseSensitive,
                                            label = { Text("Aa") }
                                        )
                                        TextButton(onClick = onReplaceCurrent) { Text("Ganti") }
                                        TextButton(onClick = onReplaceAll) { Text("Semua") }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                AnimatedVisibility(visible = !state.isFocusMode) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = wpConfig.panelOpacity),
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        Column {
                            NaraTtsFloatingBar(
                                ttsState = ttsState,
                                onStop = onStopTts,
                                onDismissError = onDismissTtsError
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                FormatToggleButton(
                                    active = state.activeBold,
                                    icon = Icons.Rounded.FormatBold,
                                    description = "Tebal",
                                    tag = "format_bold_button",
                                    onClick = { onToggleFormat(RichTextEngine.SpanFormatAttr.BOLD) }
                                )
                                FormatToggleButton(
                                    active = state.activeItalic,
                                    icon = Icons.Rounded.FormatItalic,
                                    description = "Miring",
                                    tag = "format_italic_button",
                                    onClick = { onToggleFormat(RichTextEngine.SpanFormatAttr.ITALIC) }
                                )
                                FormatToggleButton(
                                    active = state.activeUnderline,
                                    icon = Icons.Rounded.FormatUnderlined,
                                    description = "Garis bawah",
                                    tag = "format_underline_button",
                                    onClick = { onToggleFormat(RichTextEngine.SpanFormatAttr.UNDERLINE) }
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                val nextAlign = when (state.alignment) {
                                    TextAlignOption.START -> TextAlignOption.CENTER
                                    TextAlignOption.CENTER -> TextAlignOption.END
                                    TextAlignOption.END -> TextAlignOption.JUSTIFY
                                    TextAlignOption.JUSTIFY -> TextAlignOption.START
                                }
                                val alignIcon = when (state.alignment) {
                                    TextAlignOption.START -> Icons.AutoMirrored.Rounded.FormatAlignLeft
                                    TextAlignOption.CENTER -> Icons.Rounded.FormatAlignCenter
                                    TextAlignOption.END -> Icons.AutoMirrored.Rounded.FormatAlignRight
                                    TextAlignOption.JUSTIFY -> Icons.Rounded.FormatAlignJustify
                                }
                                IconButton(
                                    onClick = { onAlignmentChange(nextAlign) },
                                    modifier = Modifier.testTag("format_align_button")
                                ) {
                                    Icon(alignIcon, contentDescription = "Perataan: ${state.alignment.labelId}")
                                }

                                IconButton(
                                    onClick = {
                                        val nextIndent = if (edSet.firstLineIndentDp > 1f) 0f else 22f
                                        onUpdateEditorSettings(edSet.copy(firstLineIndentDp = nextIndent))
                                    },
                                    modifier = Modifier.testTag("format_indent_button")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.FormatIndentIncrease,
                                        contentDescription = "Indentasi paragraf",
                                        tint = if (edSet.firstLineIndentDp > 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                IconButton(onClick = { showTypographySheet = true }) {
                                    Icon(Icons.Rounded.FormatSize, contentDescription = "Pengaturan huruf")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                ) {
                                    Text(
                                        text = "${state.wordCount} kata • ${state.charCount} kar • ${state.paragraphCount} par",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { showNaraSheet = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("editor_nara_button")
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("NARA", style = MaterialTheme.typography.labelLarge)
                                }
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
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(
                        alpha = if (state.isFocusMode) 0.96f else wpConfig.panelOpacity.coerceAtLeast(0.78f)
                    ),
                    shape = RoundedCornerShape(if (state.isFocusMode) 0.dp else 18.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 780.dp)
                        .padding(
                            horizontal = if (state.isFocusMode) 0.dp else 10.dp,
                            vertical = if (state.isFocusMode) 0.dp else 8.dp
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                horizontal = edSet.horizontalMarginDp.dp,
                                vertical = if (state.isFocusMode) 40.dp else 20.dp
                            )
                    ) {
                        // Chapter Title input
                        BasicTextField(
                            value = state.title,
                            onValueChange = onTitleChange,
                            textStyle = TextStyle(
                                fontFamily = editorFont.fontFamily,
                                fontSize = (edSet.fontSizeSp + 6f).sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = textAlign
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_title_input"),
                            decorationBox = { inner ->
                                Box {
                                    if (state.title.isEmpty()) {
                                        Text(
                                            text = "Judul Bab...",
                                            style = TextStyle(
                                                fontFamily = editorFont.fontFamily,
                                                fontSize = (edSet.fontSizeSp + 6f).sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                    inner()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(18.dp))

                        // Main Rich Text Manuscript Editor
                        BasicTextField(
                            value = state.textFieldValue,
                            onValueChange = onTextChange,
                            visualTransformation = visualTransformation,
                            textStyle = TextStyle(
                                fontFamily = editorFont.fontFamily,
                                fontSize = edSet.fontSizeSp.sp,
                                lineHeight = (edSet.fontSizeSp * edSet.lineSpacingMultiplier).sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = textAlign,
                                textIndent = if (edSet.firstLineIndentDp > 0f) {
                                    TextIndent(firstLine = edSet.firstLineIndentDp.sp)
                                } else TextIndent.None
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 420.dp)
                                .testTag("editor_body_input"),
                            decorationBox = { inner ->
                                Box {
                                    if (state.textFieldValue.text.isEmpty()) {
                                        Text(
                                            text = "Mulai menulis kisahmu di sini...\n\nGunakan toolbar di bawah untuk teks tebal, miring, garis bawah, perataan paragraf, atau panggil NARA untuk diskusi dan penyuntingan.",
                                            style = TextStyle(
                                                fontFamily = editorFont.fontFamily,
                                                fontSize = edSet.fontSizeSp.sp,
                                                lineHeight = (edSet.fontSizeSp * edSet.lineSpacingMultiplier).sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(140.dp))
                    }
                }

                // Focus mode subtle floating exit & word counter pill
                if (state.isFocusMode) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                        ) {
                            Text(
                                text = "${state.wordCount} kata",
                                style = MaterialTheme.typography.labelMedium
                            )
                            IconButton(
                                onClick = onToggleFocusMode,
                                modifier = Modifier.testTag("exit_focus_mode_button")
                            ) {
                                Icon(Icons.Rounded.FullscreenExit, contentDescription = "Keluar mode fokus")
                            }
                        }
                    }
                }
            }
        }
    }

    // Chapter Switcher Sheet
    if (showChapterSwitcher) {
        ModalBottomSheet(onDismissRequest = { showChapterSwitcher = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Navigasi Bab", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(state.allChapters, key = { it.id }) { ch ->
                        val isCurrent = ch.id == state.chapter?.id
                        NaraPanelCard(
                            onClick = {
                                showChapterSwitcher = false
                                onSwitchChapter(ch.id)
                            },
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = (if (ch.parentChapterId != null) "   ↳ " else "") + ch.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${ch.wordCount} kata",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Chapter Notes Sheet
    if (showNotesSheet) {
        ModalBottomSheet(onDismissRequest = { showNotesSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Catatan Bab", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Catatan ini ikut terbaca oleh NARA saat menyunting bab ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = onNotesChange,
                    placeholder = { Text("Tulis outline adegan, detail riset, atau pengingat emosi untuk bab ini...") },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        onPersistNow()
                        showNotesSheet = false
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Simpan Catatan")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Revision History Sheet
    if (showRevisionsSheet) {
        val sdf = remember { SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()) }
        ModalBottomSheet(onDismissRequest = { showRevisionsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Riwayat Revisi", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onCreateSnapshot) {
                        Text("+ Snapshot Baru")
                    }
                }
                if (state.revisions.isEmpty()) {
                    Text(
                        text = "Belum ada riwayat revisi tersimpan untuk bab ini. Perubahan otomatis tersimpan secara berkala.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.revisions, key = { it.id }) { rev ->
                            NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(rev.summaryLabel, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "${sdf.format(Date(rev.createdAt))} • ${rev.wordCount} kata",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = rev.contentSnapshot.take(90).replace("\n", " "),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            onRestoreRevision(rev)
                                            showRevisionsSheet = false
                                        }
                                    ) {
                                        Icon(Icons.Rounded.Restore, contentDescription = "Pulihkan versi ini")
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    // Typography & Paragraph Settings Sheet
    if (showTypographySheet) {
        ModalBottomSheet(onDismissRequest = { showTypographySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Tipografi & Tata Letak Editor", style = MaterialTheme.typography.titleLarge)
                Text("Jenis Huruf", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EditorFontChoice.entries) { choice ->
                        FilterChip(
                            selected = edSet.fontKey == choice.key,
                            onClick = { onUpdateEditorSettings(edSet.copy(fontKey = choice.key)) },
                            label = { Text(choice.label) }
                        )
                    }
                }
                SliderRow(
                    label = "Ukuran Huruf (${edSet.fontSizeSp.toInt()} sp)",
                    value = edSet.fontSizeSp,
                    range = 12f..26f,
                    onChange = { onUpdateEditorSettings(edSet.copy(fontSizeSp = it)) }
                )
                SliderRow(
                    label = "Jarak Baris (${String.format(Locale.US, "%.1f", edSet.lineSpacingMultiplier)}x)",
                    value = edSet.lineSpacingMultiplier,
                    range = 1.2f..2.2f,
                    onChange = { onUpdateEditorSettings(edSet.copy(lineSpacingMultiplier = it)) }
                )
                SliderRow(
                    label = "Margin Samping (${edSet.horizontalMarginDp.toInt()} dp)",
                    value = edSet.horizontalMarginDp,
                    range = 10f..44f,
                    onChange = { onUpdateEditorSettings(edSet.copy(horizontalMarginDp = it)) }
                )
                SliderRow(
                    label = "Indentasi Awal Paragraf (${edSet.firstLineIndentDp.toInt()} dp)",
                    value = edSet.firstLineIndentDp,
                    range = 0f..36f,
                    onChange = { onUpdateEditorSettings(edSet.copy(firstLineIndentDp = it)) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Inline NARA AI Editor Sheet
    if (showNaraSheet) {
        val clipboard = LocalClipboardManager.current
        var customPrompt by remember { mutableStateOf("") }

        ModalBottomSheet(onDismissRequest = { showNaraSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Asisten Editor NARA", style = MaterialTheme.typography.titleLarge)
                        val hasSelection = !state.textFieldValue.selection.collapsed
                        Text(
                            text = if (hasSelection) "Konteks: Teks yang disorot • ${state.storyBible.size} catatan dunia" else "Konteks: Bab aktif • ${state.storyBible.size} catatan dunia",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(
                        onClick = {
                            showNaraSheet = false
                            val sel = state.textFieldValue.selection
                            val snippet = if (!sel.collapsed && sel.max <= state.textFieldValue.text.length) {
                                state.textFieldValue.text.substring(sel.min, sel.max)
                            } else ""
                            onOpenAiStudio(snippet)
                        }
                    ) {
                        Text("Buka Obrolan")
                    }
                }

                Text("Tingkat Penyuntingan", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EditingIntensity.entries) { level ->
                        FilterChip(
                            selected = state.aiIntensity == level,
                            onClick = { onSetAiIntensity(level) },
                            label = { Text(level.labelId) }
                        )
                    }
                }

                OutlinedTextField(
                    value = customPrompt,
                    onValueChange = { customPrompt = it },
                    placeholder = { Text("Arahan tambahan opsional untuk NARA...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Pilih Kemampuan NARA", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(NaraAiTool.entries) { tool ->
                        FilterChip(
                            selected = state.aiLastTool == tool,
                            onClick = { onRunAiTool(tool, customPrompt) },
                            label = { Text(tool.labelId) },
                            modifier = Modifier.testTag("editor_ai_tool_${tool.name}")
                        )
                    }
                }

                if (state.aiGenerating) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("NARA sedang menyunting...", style = MaterialTheme.typography.bodySmall)
                    }
                }

                state.aiErrorMessage?.let { err ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (state.aiResultText.isNotBlank()) {
                    NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = state.aiResultText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onApplyAiResult(true)
                                        showNaraSheet = false
                                    }
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Terapkan / Ganti")
                                }
                                OutlinedButton(
                                    onClick = {
                                        onApplyAiResult(false)
                                        showNaraSheet = false
                                    }
                                ) {
                                    Text("Sisipkan")
                                }
                                OutlinedButton(
                                    onClick = { onSpeakText(state.aiResultText, "Saran NARA") }
                                ) {
                                    Icon(Icons.Rounded.VolumeUp, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bacakan")
                                }
                                OutlinedButton(
                                    onClick = { clipboard.setText(AnnotatedString(state.aiResultText)) }
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Salin")
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showExportDialog) {
        val safeTitle = state.title.ifBlank { "bab" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Ekspor Bab") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pilih format ekspor dokumen:", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            pendingExportFormat = "txt"
                            exportChapterLauncher.launch("$safeTitle.txt")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Teks Biasa (.txt)") }
                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            pendingExportFormat = "md"
                            exportChapterLauncher.launch("$safeTitle.md")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Naskah Markdown (.md)") }
                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            pendingExportFormat = "html"
                            exportChapterLauncher.launch("$safeTitle.html")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Dokumen Kaya Teks (.html)") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Tutup") }
            }
        )
    }
}

@Composable
private fun FormatToggleButton(
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.testTag(tag)
    ) {
        Icon(imageVector = icon, contentDescription = description)
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
