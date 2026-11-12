package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SubdirectoryArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.local.ChapterEntity
import com.example.data.local.FolderEntity
import com.example.data.local.NovelEntity
import com.example.data.local.StoryBibleEntryEntity
import com.example.data.model.NovelStatus
import com.example.data.model.StoryBibleCategory
import com.example.data.model.WallpaperPageTarget
import com.example.data.prefs.AppPreferencesState
import com.example.ui.components.CreateOrEditNovelDialog
import com.example.ui.components.NaraPanelCard
import com.example.ui.components.NovelCoverArt
import com.example.ui.components.WallpaperBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelDetailScreen(
    preferences: AppPreferencesState,
    novel: NovelEntity?,
    chapters: List<ChapterEntity>,
    storyBible: List<StoryBibleEntryEntity>,
    folders: List<FolderEntity>,
    onBack: () -> Unit,
    onOpenEditor: (Long) -> Unit,
    onOpenAiStudio: (Long?) -> Unit,
    onUpdateNovel: (NovelEntity, Uri?) -> Unit,
    onCreateChapter: (String, Long?) -> Unit,
    onReorderChapters: (List<Long>) -> Unit,
    onSoftDeleteChapter: (Long) -> Unit,
    onSaveBibleEntry: (Long, StoryBibleCategory, String, String, String, String) -> Unit,
    onDeleteBibleEntry: (Long) -> Unit,
    onExportNovelMarkdown: (Uri) -> Unit,
    onImportDocumentAsChapter: (String, Uri) -> Unit
) {
    BackHandler { onBack() }

    if (novel == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Memuat proyek novel...")
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditNovelDialog by remember { mutableStateOf(false) }
    var showCreateChapterDialog by remember { mutableStateOf<Pair<Boolean, Long?>>(false to null) }
    var editingBibleEntry by remember { mutableStateOf<StoryBibleEntryEntity?>(null) }
    var showNewBibleDialog by remember { mutableStateOf(false) }
    var selectedBibleCategory by remember { mutableStateOf<StoryBibleCategory?>(null) }

    val context = LocalContext.current
    val exportNovelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) onExportNovelMarkdown(uri)
    }
    val importChapterLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Bab Impor"
            onImportDocumentAsChapter(fileName, uri)
        }
    }

    // Local mutable copy for smooth drag-and-drop chapter reordering
    val orderedChapters = remember { mutableStateListOf<ChapterEntity>() }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragDeltaY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(chapters) {
        if (draggingId == null) {
            orderedChapters.clear()
            orderedChapters.addAll(chapters.sortedBy { it.sortOrder })
        }
    }

    val totalWords = remember(orderedChapters.toList()) { orderedChapters.sumOf { it.wordCount } }
    val targetWords = novel.targetWordCount.coerceAtLeast(1)
    val progress = (totalWords.toFloat() / targetWords.toFloat()).coerceIn(0f, 1f)

    WallpaperBackdrop(config = preferences.resolveWallpaperFor(WallpaperPageTarget.LIBRARY)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = preferences.resolveWallpaperFor(WallpaperPageTarget.LIBRARY).panelOpacity)
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("novel_detail_back")) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
                        }
                    },
                    title = {
                        Text(
                            text = novel.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { onOpenAiStudio(orderedChapters.firstOrNull()?.id) },
                            modifier = Modifier.testTag("novel_detail_ai_button")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = "Buka Studio NARA")
                        }
                        IconButton(onClick = { showEditNovelDialog = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit Info Novel")
                        }
                    }
                )
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(
                        onClick = { showCreateChapterDialog = true to null },
                        modifier = Modifier
                            .navigationBarsPadding()
                            .testTag("fab_add_chapter")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Tambah Bab")
                    }
                } else if (selectedTab == 1) {
                    FloatingActionButton(
                        onClick = { showNewBibleDialog = true },
                        modifier = Modifier
                            .navigationBarsPadding()
                            .testTag("fab_add_bible_entry")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Tambah Referensi Cerita")
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 880.dp)
                ) {
                    // Header Summary Card
                    NaraPanelCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                NovelCoverArt(
                                    title = novel.title,
                                    genre = novel.genre,
                                    coverPath = novel.coverPath,
                                    modifier = Modifier
                                        .width(64.dp)
                                        .aspectRatio(0.7f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${novel.genre} • ${NovelStatus.fromName(novel.status).labelId}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (novel.author.isNotBlank()) {
                                        Text(
                                            text = "Penulis: ${novel.author}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${orderedChapters.size} bab • $totalWords / $targetWords kata",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (novel.synopsis.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = novel.synopsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val safeName = novel.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                                        exportNovelLauncher.launch("${safeName}_naskah.md")
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ekspor", maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = {
                                        importChapterLauncher.launch(arrayOf("text/plain", "text/markdown", "*/*"))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Impor Bab", maxLines = 1)
                                }
                            }
                        }
                    }

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Bab & Subbab") },
                            modifier = Modifier.testTag("tab_chapters")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Dunia Cerita (${storyBible.size})") },
                            modifier = Modifier.testTag("tab_story_bible")
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Statistik") },
                            modifier = Modifier.testTag("tab_novel_stats")
                        )
                    }

                    when (selectedTab) {
                        0 -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    Text(
                                        text = "Tahan ikon garis di kanan bab untuk menggeser urutan bab.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                itemsIndexed(orderedChapters, key = { _, ch -> ch.id }) { index, chapter ->
                                    val isDragging = draggingId == chapter.id
                                    val isSubchapter = chapter.parentChapterId != null
                                    var menuOpen by remember { mutableStateOf(false) }

                                    NaraPanelCard(
                                        onClick = { onOpenEditor(chapter.id) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = if (isSubchapter) 24.dp else 0.dp)
                                            .zIndex(if (isDragging) 10f else 0f)
                                            .graphicsLayer {
                                                translationY = if (isDragging) dragDeltaY else 0f
                                                scaleX = if (isDragging) 1.02f else 1f
                                                scaleY = if (isDragging) 1.02f else 1f
                                            }
                                            .testTag("chapter_row_${chapter.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSubchapter) {
                                                Icon(
                                                    imageVector = Icons.Rounded.SubdirectoryArrowRight,
                                                    contentDescription = "Subbab",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = chapter.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${chapter.wordCount} kata • ${chapter.paragraphCount} paragraf",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Box {
                                                IconButton(onClick = { menuOpen = true }) {
                                                    Icon(Icons.Rounded.MoreVert, contentDescription = "Menu bab")
                                                }
                                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                                    if (!isSubchapter) {
                                                        DropdownMenuItem(
                                                            text = { Text("Tambah Subbab") },
                                                            onClick = {
                                                                menuOpen = false
                                                                showCreateChapterDialog = true to chapter.id
                                                            }
                                                        )
                                                    }
                                                    if (index > 0) {
                                                        DropdownMenuItem(
                                                            text = { Text("Geser Naik") },
                                                            onClick = {
                                                                menuOpen = false
                                                                val item = orderedChapters.removeAt(index)
                                                                orderedChapters.add(index - 1, item)
                                                                onReorderChapters(orderedChapters.map { it.id })
                                                            }
                                                        )
                                                    }
                                                    if (index < orderedChapters.lastIndex) {
                                                        DropdownMenuItem(
                                                            text = { Text("Geser Turun") },
                                                            onClick = {
                                                                menuOpen = false
                                                                val item = orderedChapters.removeAt(index)
                                                                orderedChapters.add(index + 1, item)
                                                                onReorderChapters(orderedChapters.map { it.id })
                                                            }
                                                        )
                                                    }
                                                    DropdownMenuItem(
                                                        text = { Text("Hapus ke Tong Sampah", color = MaterialTheme.colorScheme.error) },
                                                        onClick = {
                                                            menuOpen = false
                                                            onSoftDeleteChapter(chapter.id)
                                                        }
                                                    )
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .pointerInput(chapter.id, orderedChapters.size) {
                                                        detectDragGestures(
                                                            onDragStart = {
                                                                draggingId = chapter.id
                                                                dragDeltaY = 0f
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                dragDeltaY += dragAmount.y
                                                                val rowHeightPx = 170f
                                                                val curIdx = orderedChapters.indexOfFirst { it.id == chapter.id }
                                                                if (curIdx >= 0) {
                                                                    if (dragDeltaY > rowHeightPx && curIdx < orderedChapters.lastIndex) {
                                                                        val moved = orderedChapters.removeAt(curIdx)
                                                                        orderedChapters.add(curIdx + 1, moved)
                                                                        dragDeltaY -= rowHeightPx
                                                                    } else if (dragDeltaY < -rowHeightPx && curIdx > 0) {
                                                                        val moved = orderedChapters.removeAt(curIdx)
                                                                        orderedChapters.add(curIdx - 1, moved)
                                                                        dragDeltaY += rowHeightPx
                                                                    }
                                                                }
                                                            },
                                                            onDragEnd = {
                                                                draggingId = null
                                                                dragDeltaY = 0f
                                                                onReorderChapters(orderedChapters.map { it.id })
                                                            },
                                                            onDragCancel = {
                                                                draggingId = null
                                                                dragDeltaY = 0f
                                                            }
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DragHandle,
                                                    contentDescription = "Geser urutan bab",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            val filteredEntries = remember(storyBible, selectedBibleCategory) {
                                if (selectedBibleCategory == null) storyBible
                                else storyBible.filter { it.category == selectedBibleCategory?.name }
                            }
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    Text(
                                        text = "Semua data di sini otomatis dipahami NARA saat membaca dan menyunting naskahmu.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                item {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        item {
                                            FilterChip(
                                                selected = selectedBibleCategory == null,
                                                onClick = { selectedBibleCategory = null },
                                                label = { Text("Semua (${storyBible.size})") }
                                            )
                                        }
                                        items(StoryBibleCategory.entries) { cat ->
                                            FilterChip(
                                                selected = selectedBibleCategory == cat,
                                                onClick = { selectedBibleCategory = cat },
                                                label = { Text(cat.labelId) }
                                            )
                                        }
                                    }
                                }
                                if (filteredEntries.isEmpty()) {
                                    item {
                                        NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.padding(20.dp)) {
                                                Text(
                                                    text = "Belum ada catatan referensi cerita",
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Catat profil karakter, hubungan, lokasi, kronologi, konflik, dan aturan dunia di sini agar NARA selalu menjaga konsistensi novelmu.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    items(filteredEntries, key = { it.id }) { entry ->
                                        val cat = StoryBibleCategory.fromName(entry.category)
                                        NaraPanelCard(
                                            onClick = { editingBibleEntry = entry },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.secondaryContainer
                                                    ) {
                                                        Text(
                                                            text = cat.labelId,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                    IconButton(onClick = { onDeleteBibleEntry(entry.id) }) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.DeleteOutline,
                                                            contentDescription = "Hapus catatan",
                                                            tint = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = entry.title,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                if (entry.subtitle.isNotBlank()) {
                                                    Text(
                                                        text = entry.subtitle,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                if (entry.content.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = entry.content,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            val avgWords = if (orderedChapters.isNotEmpty()) totalWords / orderedChapters.size else 0
                            val readMinutes = (totalWords / 200).coerceAtLeast(1)
                            val maxChapterWords = (orderedChapters.maxOfOrNull { it.wordCount } ?: 1).coerceAtLeast(1)
                            val primaryColor = MaterialTheme.colorScheme.primary

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("Ringkasan Naskah", style = MaterialTheme.typography.titleMedium)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                StatBlock("Total Kata", "$totalWords")
                                                StatBlock("Jumlah Bab", "${orderedChapters.size}")
                                                StatBlock("Rata-rata/Bab", "$avgWords")
                                                StatBlock("Est. Baca", "$readMinutes mnt")
                                            }
                                        }
                                    }
                                }
                                if (orderedChapters.isNotEmpty()) {
                                    item {
                                        NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text("Distribusi Kata per Bab", style = MaterialTheme.typography.titleMedium)
                                                orderedChapters.forEach { ch ->
                                                    val ratio = (ch.wordCount.toFloat() / maxChapterWords.toFloat()).coerceIn(0.03f, 1f)
                                                    Column {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(
                                                                text = ch.title,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            Text(
                                                                text = "${ch.wordCount} kata",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Canvas(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(8.dp)
                                                        ) {
                                                            drawRoundRect(
                                                                color = primaryColor.copy(alpha = 0.16f),
                                                                size = size,
                                                                cornerRadius = CornerRadius(8f, 8f)
                                                            )
                                                            drawRoundRect(
                                                                color = primaryColor,
                                                                topLeft = Offset.Zero,
                                                                size = Size(size.width * ratio, size.height),
                                                                cornerRadius = CornerRadius(8f, 8f)
                                                            )
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

    if (showEditNovelDialog) {
        CreateOrEditNovelDialog(
            initialNovel = novel,
            folders = folders,
            defaultAuthor = preferences.profile.penName,
            defaultFolderId = novel.folderId,
            onDismiss = { showEditNovelDialog = false },
            onConfirm = { title, author, synopsis, genre, status, folderId, coverUri, target ->
                showEditNovelDialog = false
                onUpdateNovel(
                    novel.copy(
                        title = title.ifBlank { novel.title },
                        author = author,
                        synopsis = synopsis,
                        genre = genre.ifBlank { novel.genre },
                        status = status.name,
                        folderId = folderId,
                        targetWordCount = target
                    ),
                    coverUri
                )
            }
        )
    }

    if (showCreateChapterDialog.first) {
        val parentId = showCreateChapterDialog.second
        var titleText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateChapterDialog = false to null },
            title = { Text(if (parentId == null) "Tambah Bab Baru" else "Tambah Subbab Baru") },
            text = {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text(if (parentId == null) "Judul Bab (cth: Bab 2: Hujan di Pelabuhan)" else "Judul Subbab") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chapter_title_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalTitle = titleText.trim().ifEmpty {
                            if (parentId == null) "Bab ${orderedChapters.size + 1}" else "Subbab Baru"
                        }
                        showCreateChapterDialog = false to null
                        onCreateChapter(finalTitle, parentId)
                    },
                    modifier = Modifier.testTag("confirm_add_chapter_button")
                ) {
                    Text("Buat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateChapterDialog = false to null }) { Text("Batal") }
            }
        )
    }

    if (showNewBibleDialog || editingBibleEntry != null) {
        StoryBibleEntryDialog(
            initial = editingBibleEntry,
            onDismiss = {
                showNewBibleDialog = false
                editingBibleEntry = null
            },
            onConfirm = { cat, title, subtitle, content, tags ->
                val id = editingBibleEntry?.id ?: 0L
                showNewBibleDialog = false
                editingBibleEntry = null
                onSaveBibleEntry(id, cat, title, subtitle, content, tags)
            }
        )
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StoryBibleEntryDialog(
    initial: StoryBibleEntryEntity?,
    onDismiss: () -> Unit,
    onConfirm: (StoryBibleCategory, String, String, String, String) -> Unit
) {
    var category by remember { mutableStateOf(StoryBibleCategory.fromName(initial?.category)) }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var subtitle by remember { mutableStateOf(initial?.subtitle ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }
    var tags by remember { mutableStateOf(initial?.tags ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Referensi Cerita" else "Edit Referensi Cerita") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Kategori", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(StoryBibleCategory.entries) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.labelId, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama / Judul Catatan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Peran / Ringkasan Singkat") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Detail Lengkap") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tag (pisahkan koma)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) onConfirm(category, title, subtitle, content, tags)
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
