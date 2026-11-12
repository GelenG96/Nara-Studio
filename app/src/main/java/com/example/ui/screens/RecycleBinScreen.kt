package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.ChapterEntity
import com.example.data.local.FolderEntity
import com.example.data.local.NovelEntity
import com.example.data.model.WallpaperPageTarget
import com.example.data.prefs.AppPreferencesState
import com.example.ui.components.NaraPanelCard
import com.example.ui.components.WallpaperBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    preferences: AppPreferencesState,
    deletedNovels: List<NovelEntity>,
    deletedChapters: List<ChapterEntity>,
    deletedFolders: List<FolderEntity>,
    onBack: () -> Unit,
    onRestoreNovel: (Long) -> Unit,
    onDeleteNovelPermanently: (Long) -> Unit,
    onRestoreChapter: (Long) -> Unit,
    onDeleteChapterPermanently: (Long) -> Unit,
    onRestoreFolder: (Long) -> Unit,
    onDeleteFolderPermanently: (Long) -> Unit,
    onPurgeAll: () -> Unit
) {
    BackHandler { onBack() }
    val wpConfig = preferences.resolveWallpaperFor(WallpaperPageTarget.LIBRARY)
    val isEmpty = deletedNovels.isEmpty() && deletedChapters.isEmpty() && deletedFolders.isEmpty()

    WallpaperBackdrop(config = wpConfig) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = wpConfig.panelOpacity)
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("recycle_bin_back")) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
                        }
                    },
                    title = { Text("Tong Sampah") },
                    actions = {
                        if (!isEmpty) {
                            TextButton(onClick = onPurgeAll) {
                                Text("Kosongkan", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                if (isEmpty) {
                    NaraPanelCard(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .padding(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tong Sampah Kosong", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Novel, bab, atau folder yang dihapus akan muncul di sini sebelum dihapus permanen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 860.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (deletedNovels.isNotEmpty()) {
                            item { Text("Novel Terhapus", style = MaterialTheme.typography.labelLarge) }
                            items(deletedNovels, key = { "dn_${it.id}" }) { n ->
                                RecycleItemCard(
                                    title = n.title,
                                    subtitle = "Novel • ${n.genre}",
                                    onRestore = { onRestoreNovel(n.id) },
                                    onDeleteForever = { onDeleteNovelPermanently(n.id) }
                                )
                            }
                        }
                        if (deletedChapters.isNotEmpty()) {
                            item { Text("Bab Terhapus", style = MaterialTheme.typography.labelLarge) }
                            items(deletedChapters, key = { "dc_${it.id}" }) { ch ->
                                RecycleItemCard(
                                    title = ch.title,
                                    subtitle = "Bab • ${ch.wordCount} kata",
                                    onRestore = { onRestoreChapter(ch.id) },
                                    onDeleteForever = { onDeleteChapterPermanently(ch.id) }
                                )
                            }
                        }
                        if (deletedFolders.isNotEmpty()) {
                            item { Text("Folder Terhapus", style = MaterialTheme.typography.labelLarge) }
                            items(deletedFolders, key = { "df_${it.id}" }) { f ->
                                RecycleItemCard(
                                    title = f.name,
                                    subtitle = "Folder",
                                    onRestore = { onRestoreFolder(f.id) },
                                    onDeleteForever = { onDeleteFolderPermanently(f.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecycleItemCard(
    title: String,
    subtitle: String,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRestore) {
                Icon(Icons.Rounded.RestoreFromTrash, contentDescription = "Pulihkan", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onDeleteForever) {
                Icon(Icons.Rounded.DeleteForever, contentDescription = "Hapus permanen", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
