package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SubdirectoryArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ChapterEntity
import com.example.data.local.FolderEntity
import com.example.data.local.NovelEntity
import com.example.data.model.NovelStatus
import com.example.data.model.WallpaperPageTarget
import com.example.data.prefs.AppPreferencesState
import com.example.ui.components.CreateOrEditNovelDialog
import com.example.ui.components.NaraPanelCard
import com.example.ui.components.NovelListCard
import com.example.ui.components.WallpaperBackdrop

enum class LibraryFilterTab(val labelId: String) {
    ALL("Semua"),
    FAVORITES("Favorit"),
    BY_GENRE("Per Genre"),
    ARCHIVED("Arsip")
}

@Composable
fun LibraryScreen(
    preferences: AppPreferencesState,
    folders: List<FolderEntity>,
    novels: List<NovelEntity>,
    chapters: List<ChapterEntity>,
    onOpenNovel: (Long) -> Unit,
    onOpenEditor: (Long, Long) -> Unit,
    onOpenRecycleBin: () -> Unit,
    onCreateFolder: (String, Long?) -> Unit,
    onRenameFolder: (FolderEntity, String) -> Unit,
    onDeleteFolder: (Long) -> Unit,
    onCreateNovel: (String, String, String, String, NovelStatus, Long?, Uri?, Int) -> Unit,
    onToggleFavorite: (NovelEntity) -> Unit,
    onToggleArchive: (NovelEntity) -> Unit,
    onMoveNovelToFolder: (Long, Long?) -> Unit,
    onSoftDeleteNovel: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(LibraryFilterTab.ALL) }
    var currentFolderId by remember { mutableStateOf<Long?>(null) }
    var selectedGenreFilter by remember { mutableStateOf<String?>(null) }

    var showCreateNovelDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var renamingFolder by remember { mutableStateOf<FolderEntity?>(null) }
    var movingNovel by remember { mutableStateOf<NovelEntity?>(null) }

    val chaptersByNovel = remember(chapters) { chapters.groupBy { it.novelId } }
    val currentFolder = remember(folders, currentFolderId) {
        folders.firstOrNull { it.id == currentFolderId }
    }
    val subFolders = remember(folders, currentFolderId, activeTab, searchQuery) {
        if (activeTab != LibraryFilterTab.ALL || searchQuery.isNotBlank()) emptyList()
        else folders.filter { it.parentFolderId == currentFolderId }
    }

    val allGenres = remember(novels) {
        novels.filter { !it.isArchived }.map { it.genre }.distinct().sorted()
    }

    val filteredNovels = remember(novels, activeTab, currentFolderId, selectedGenreFilter, searchQuery) {
        val q = searchQuery.trim()
        if (q.isNotEmpty()) {
            novels.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.author.contains(q, ignoreCase = true) ||
                    it.genre.contains(q, ignoreCase = true) ||
                    it.synopsis.contains(q, ignoreCase = true)
            }
        } else {
            when (activeTab) {
                LibraryFilterTab.ALL -> novels.filter { !it.isArchived && it.folderId == currentFolderId }
                LibraryFilterTab.FAVORITES -> novels.filter { !it.isArchived && it.isFavorite }
                LibraryFilterTab.BY_GENRE -> novels.filter {
                    !it.isArchived && (selectedGenreFilter == null || it.genre.equals(selectedGenreFilter, ignoreCase = true))
                }
                LibraryFilterTab.ARCHIVED -> novels.filter { it.isArchived }
            }
        }
    }

    val matchingChapters = remember(chapters, novels, searchQuery) {
        val q = searchQuery.trim()
        if (q.length < 2) emptyList()
        else {
            val novelMap = novels.associateBy { it.id }
            chapters.filter {
                it.title.contains(q, ignoreCase = true) || it.content.contains(q, ignoreCase = true)
            }.take(20).mapNotNull { ch ->
                novelMap[ch.novelId]?.let { n -> ch to n }
            }
        }
    }

    WallpaperBackdrop(
        config = preferences.resolveWallpaperFor(WallpaperPageTarget.LIBRARY),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 860.dp)
                    .statusBarsPadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Perpustakaan Novel",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "${novels.count { !it.isArchived }} proyek aktif • ${folders.size} folder",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onOpenRecycleBin,
                            modifier = Modifier.testTag("open_recycle_bin_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Buka Tong Sampah",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Search bar for novels & chapters
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari novel, penulis, genre, atau isi bab...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("library_search_input")
                    )
                }

                // Filter Tabs
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(LibraryFilterTab.entries) { tab ->
                            FilterChip(
                                selected = activeTab == tab,
                                onClick = {
                                    activeTab = tab
                                    if (tab != LibraryFilterTab.BY_GENRE) selectedGenreFilter = null
                                },
                                label = { Text(tab.labelId) },
                                modifier = Modifier.testTag("library_tab_${tab.name}")
                            )
                        }
                    }
                }

                // Genre sub-chips when BY_GENRE is active
                if (activeTab == LibraryFilterTab.BY_GENRE && allGenres.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedGenreFilter == null,
                                    onClick = { selectedGenreFilter = null },
                                    label = { Text("Semua Genre") }
                                )
                            }
                            items(allGenres) { g ->
                                FilterChip(
                                    selected = selectedGenreFilter == g,
                                    onClick = { selectedGenreFilter = g },
                                    label = { Text(g) }
                                )
                            }
                        }
                    }
                }

                // Folder navigation breadcrumb bar
                if (activeTab == LibraryFilterTab.ALL && searchQuery.isBlank() && currentFolder != null) {
                    item {
                        NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentFolderId = currentFolder.parentFolderId }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali ke folder induk")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Folder: ${currentFolder.name}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }

                // Subfolders list
                if (subFolders.isNotEmpty()) {
                    item {
                        Text(
                            text = if (currentFolderId == null) "Folder Proyek" else "Subfolder",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(subFolders, key = { "folder_${it.id}" }) { folder ->
                        var folderMenu by remember { mutableStateOf(false) }
                        val childFoldersCount = folders.count { it.parentFolderId == folder.id }
                        val childNovelsCount = novels.count { !it.isArchived && it.folderId == folder.id }

                        NaraPanelCard(
                            onClick = { currentFolderId = folder.id },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("folder_card_${folder.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = folder.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$childNovelsCount novel • $childFoldersCount subfolder",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box {
                                    IconButton(onClick = { folderMenu = true }) {
                                        Icon(Icons.Rounded.MoreVert, contentDescription = "Menu folder")
                                    }
                                    DropdownMenu(expanded = folderMenu, onDismissRequest = { folderMenu = false }) {
                                        DropdownMenuItem(
                                            text = { Text("Ubah Nama") },
                                            onClick = {
                                                folderMenu = false
                                                renamingFolder = folder
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Hapus ke Tong Sampah", color = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                folderMenu = false
                                                onDeleteFolder(folder.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Novels list
                if (filteredNovels.isEmpty() && subFolders.isEmpty() && matchingChapters.isEmpty()) {
                    item {
                        NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "Tidak ada hasil untuk \"$searchQuery\"" else "Belum ada novel di sini",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Tekan tombol + di kanan bawah untuk membuat novel atau folder baru.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredNovels, key = { "novel_${it.id}" }) { novel ->
                        val chs = chaptersByNovel[novel.id].orEmpty()
                        NovelListCard(
                            novel = novel,
                            chapterCount = chs.size,
                            totalWords = chs.sumOf { it.wordCount },
                            onClick = { onOpenNovel(novel.id) },
                            onToggleFavorite = { onToggleFavorite(novel) },
                            onToggleArchive = { onToggleArchive(novel) },
                            onMoveToFolder = { movingNovel = novel },
                            onDelete = { onSoftDeleteNovel(novel.id) }
                        )
                    }
                }

                // Chapter Search Results
                if (matchingChapters.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Bab yang Cocok (${matchingChapters.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(matchingChapters, key = { "ch_match_${it.first.id}" }) { (ch, novel) ->
                        NaraPanelCard(
                            onClick = { onOpenEditor(novel.id, ch.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SubdirectoryArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ch.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${novel.title} • ${ch.wordCount} kata",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating Action Buttons
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { showCreateFolderDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.testTag("fab_create_folder")
                ) {
                    Icon(Icons.Rounded.CreateNewFolder, contentDescription = "Buat Folder Baru")
                }
                FloatingActionButton(
                    onClick = { showCreateNovelDialog = true },
                    modifier = Modifier.testTag("fab_create_novel")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Buat Novel Baru")
                }
            }
        }
    }

    if (showCreateNovelDialog) {
        CreateOrEditNovelDialog(
            folders = folders,
            defaultAuthor = preferences.profile.penName,
            defaultFolderId = currentFolderId,
            onDismiss = { showCreateNovelDialog = false },
            onConfirm = { title, author, synopsis, genre, status, folderId, coverUri, target ->
                showCreateNovelDialog = false
                onCreateNovel(title, author, synopsis, genre, status, folderId, coverUri, target)
            }
        )
    }

    if (showCreateFolderDialog) {
        FolderNameDialog(
            title = if (currentFolderId == null) "Buat Folder Baru" else "Buat Subfolder Baru",
            initialName = "",
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                showCreateFolderDialog = false
                onCreateFolder(name, currentFolderId)
            }
        )
    }

    renamingFolder?.let { folder ->
        FolderNameDialog(
            title = "Ubah Nama Folder",
            initialName = folder.name,
            onDismiss = { renamingFolder = null },
            onConfirm = { newName ->
                renamingFolder = null
                onRenameFolder(folder, newName)
            }
        )
    }

    movingNovel?.let { novel ->
        AlertDialog(
            onDismissRequest = { movingNovel = null },
            title = { Text("Pindahkan \"${novel.title}\"") },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onMoveNovelToFolder(novel.id, null)
                                movingNovel = null
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = novel.folderId == null, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Utama (Tanpa Folder)")
                    }
                    folders.forEach { f ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onMoveNovelToFolder(novel.id, f.id)
                                    movingNovel = null
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = novel.folderId == f.id, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(f.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { movingNovel = null }) { Text("Tutup") }
            }
        )
    }
}

@Composable
private fun FolderNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Folder") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("folder_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                modifier = Modifier.testTag("confirm_folder_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
