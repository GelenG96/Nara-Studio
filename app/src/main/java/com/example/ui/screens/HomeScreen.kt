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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ChapterEntity
import com.example.data.local.FolderEntity
import com.example.data.local.NovelEntity
import com.example.data.local.WritingStatEntity
import com.example.data.model.NovelStatus
import com.example.data.model.WallpaperPageTarget
import com.example.data.prefs.AppPreferencesState
import com.example.ui.components.CreateOrEditNovelDialog
import com.example.ui.components.NaraPanelCard
import com.example.ui.components.NovelCoverArt
import com.example.ui.components.WallpaperBackdrop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    preferences: AppPreferencesState,
    novels: List<NovelEntity>,
    chapters: List<ChapterEntity>,
    folders: List<FolderEntity>,
    recentStats: List<WritingStatEntity>,
    onOpenNovel: (Long) -> Unit,
    onOpenEditor: (Long, Long) -> Unit,
    onOpenAiStudio: (Long?, Long?) -> Unit,
    onCreateNovel: (String, String, String, String, NovelStatus, Long?, android.net.Uri?, Int) -> Unit,
    onRestoreAutoBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    val nonArchivedNovels = remember(novels) { novels.filter { !it.isArchived } }
    val lastOpenedNovel = nonArchivedNovels.firstOrNull()
    val favoriteNovels = remember(nonArchivedNovels) { nonArchivedNovels.filter { it.isFavorite } }
    val chaptersByNovel = remember(chapters) { chapters.groupBy { it.novelId } }

    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val todayWords = remember(recentStats, todayKey) {
        recentStats.filter { it.dateKey == todayKey }.sumOf { it.wordsWritten }
    }
    val totalLibraryWords = remember(chapters) { chapters.sumOf { it.wordCount } }
    val dailyGoal = preferences.profile.dailyWordGoal.coerceAtLeast(1)
    val goalProgress = (todayWords.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)

    WallpaperBackdrop(
        config = preferences.resolveWallpaperFor(WallpaperPageTarget.HOME),
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Studio Penulis NARA",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Halo, ${preferences.profile.penName}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { onOpenAiStudio(lastOpenedNovel?.id, null) }
                                .testTag("home_nara_avatar_button")
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_nara_avatar),
                                contentDescription = "Buka NARA",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                    }
                }

                // Daily Progress & Stats Card
                item {
                    NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Target Menulis Hari Ini",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Text(
                                    text = "$todayWords / $dailyGoal kata",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { goalProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${nonArchivedNovels.size} proyek aktif",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$totalLibraryWords total kata tersimpan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Continue Writing Hero or Clean Empty Onboarding State
                item {
                    if (lastOpenedNovel != null) {
                        val novelChapters = chaptersByNovel[lastOpenedNovel.id].orEmpty()
                            .sortedBy { it.sortOrder }
                        val latestChapter = novelChapters.maxByOrNull { it.updatedAt } ?: novelChapters.firstOrNull()
                        val novelWords = novelChapters.sumOf { it.wordCount }
                        val target = lastOpenedNovel.targetWordCount.coerceAtLeast(1)
                        val progress = (novelWords.toFloat() / target.toFloat()).coerceIn(0f, 1f)

                        NaraPanelCard(
                            onClick = { onOpenNovel(lastOpenedNovel.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_continue_card")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "LANJUTKAN MENULIS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    NovelCoverArt(
                                        title = lastOpenedNovel.title,
                                        genre = lastOpenedNovel.genre,
                                        coverPath = lastOpenedNovel.coverPath,
                                        modifier = Modifier
                                            .width(82.dp)
                                            .aspectRatio(0.7f)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = lastOpenedNovel.title,
                                            style = MaterialTheme.typography.titleLarge,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (latestChapter != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Bab terakhir: ${latestChapter.title}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$novelWords / $target kata (${(progress * 100).toInt()}%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (latestChapter != null) {
                                                onOpenEditor(lastOpenedNovel.id, latestChapter.id)
                                            } else {
                                                onOpenNovel(lastOpenedNovel.id)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("home_resume_editor_button")
                                    ) {
                                        Icon(Icons.Rounded.EditNote, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Buka Editor")
                                    }
                                    OutlinedButton(
                                        onClick = { onOpenAiStudio(lastOpenedNovel.id, latestChapter?.id) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("home_discuss_nara_button")
                                    ) {
                                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Diskusi NARA")
                                    }
                                }
                            }
                        }
                    } else {
                        NaraPanelCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoStories,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Meja Tulis Masih Kosong",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Mulai proyek novel pertamamu sekarang. Semua bab, catatan dunia cerita, dan revisi tersimpan permanen di perangkat.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = { showCreateDialog = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("home_create_first_novel_button")
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Novel Baru")
                                    }
                                    OutlinedButton(
                                        onClick = onRestoreAutoBackup,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Restore, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pulihkan")
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Actions Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NaraPanelCard(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_quick_new_novel")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Buat Novel Baru", style = MaterialTheme.typography.titleSmall)
                            }
                        }
                        NaraPanelCard(
                            onClick = { onOpenAiStudio(lastOpenedNovel?.id, null) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_quick_nara")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Brainstorm NARA", style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }

                // Favorites Horizontal Shelf
                if (favoriteNovels.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Novel Favorit",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(favoriteNovels, key = { it.id }) { novel ->
                                val wc = chaptersByNovel[novel.id].orEmpty().sumOf { it.wordCount }
                                NaraPanelCard(
                                    onClick = { onOpenNovel(novel.id) },
                                    modifier = Modifier.width(136.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        NovelCoverArt(
                                            title = novel.title,
                                            genre = novel.genre,
                                            coverPath = novel.coverPath,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(0.75f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = novel.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$wc kata",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Recent Projects
                if (nonArchivedNovels.size > 1) {
                    item {
                        Text(
                            text = "Terakhir Dibuka",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    items(nonArchivedNovels.drop(1).take(5), key = { it.id }) { novel ->
                        val chs = chaptersByNovel[novel.id].orEmpty()
                        val wc = chs.sumOf { it.wordCount }
                        NaraPanelCard(
                            onClick = { onOpenNovel(novel.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NovelCoverArt(
                                    title = novel.title,
                                    genre = novel.genre,
                                    coverPath = novel.coverPath,
                                    modifier = Modifier
                                        .width(50.dp)
                                        .aspectRatio(0.72f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = novel.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${novel.genre} • ${chs.size} bab • $wc kata",
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
    }

    if (showCreateDialog) {
        CreateOrEditNovelDialog(
            folders = folders,
            defaultAuthor = preferences.profile.penName,
            defaultFolderId = null,
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, author, synopsis, genre, status, folderId, coverUri, target ->
                showCreateDialog = false
                onCreateNovel(title, author, synopsis, genre, status, folderId, coverUri, target)
            }
        )
    }
}
