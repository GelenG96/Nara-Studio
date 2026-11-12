package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.FolderEntity
import com.example.data.local.NovelEntity
import com.example.data.model.NovelStatus
import java.io.File
import kotlin.math.abs

val PRESET_GENRES = listOf(
    "Fantasi", "Romansa", "Misteri & Thriller", "Sci-Fi", "Horor",
    "Drama Keluarga", "Sejarah", "Petualangan", "Sastra", "Fiksi Umum"
)

@Composable
fun NovelCoverArt(
    title: String,
    genre: String,
    coverPath: String,
    modifier: Modifier = Modifier
) {
    val coverFile = remember(coverPath) { if (coverPath.isNotBlank()) File(coverPath) else null }
    val palette = remember(title, genre) {
        val hash = abs((title + genre).hashCode())
        val pairs = listOf(
            Color(0xFF283747) to Color(0xFFB8532A),
            Color(0xFF213C31) to Color(0xFF6B8E78),
            Color(0xFF3E2723) to Color(0xFFD48806),
            Color(0xFF242847) to Color(0xFF6C7BB8),
            Color(0xFF4A2334) to Color(0xFFC06C84)
        )
        pairs[hash % pairs.size]
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(palette.first, palette.second)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (coverFile != null && coverFile.exists()) {
            AsyncImage(
                model = coverFile,
                contentDescription = "Sampul novel $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = genre.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Rounded.AutoStories,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = title.ifBlank { "Novel" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun NovelListCard(
    novel: NovelEntity,
    chapterCount: Int,
    totalWords: Int,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleArchive: () -> Unit,
    onMoveToFolder: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val statusObj = NovelStatus.fromName(novel.status)

    NaraPanelCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("novel_card_${novel.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NovelCoverArt(
                title = novel.title,
                genre = novel.genre,
                coverPath = novel.coverPath,
                modifier = Modifier
                    .width(74.dp)
                    .aspectRatio(0.7f)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = novel.genre,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = statusObj.labelId,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = novel.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (novel.author.isNotBlank()) {
                    Text(
                        text = "oleh ${novel.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (novel.synopsis.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = novel.synopsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$chapterCount bab • $totalWords kata",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("fav_novel_${novel.id}")
                ) {
                    Icon(
                        imageVector = if (novel.isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (novel.isFavorite) "Hapus dari favorit" else "Tandai favorit",
                        tint = if (novel.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Menu novel",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Pindahkan ke Folder") },
                            onClick = {
                                menuExpanded = false
                                onMoveToFolder()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (novel.isArchived) "Keluarkan dari Arsip" else "Arsipkan Novel") },
                            onClick = {
                                menuExpanded = false
                                onToggleArchive()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Pindah ke Tong Sampah", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateOrEditNovelDialog(
    initialNovel: NovelEntity? = null,
    folders: List<FolderEntity>,
    defaultAuthor: String,
    defaultFolderId: Long?,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        author: String,
        synopsis: String,
        genre: String,
        status: NovelStatus,
        folderId: Long?,
        coverUri: Uri?,
        targetWordCount: Int
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialNovel?.title ?: "") }
    var author by remember { mutableStateOf(initialNovel?.author?.ifBlank { defaultAuthor } ?: defaultAuthor) }
    var synopsis by remember { mutableStateOf(initialNovel?.synopsis ?: "") }
    var genre by remember { mutableStateOf(initialNovel?.genre ?: "Fiksi Umum") }
    var status by remember { mutableStateOf(NovelStatus.fromName(initialNovel?.status)) }
    var folderId by remember { mutableStateOf(initialNovel?.folderId ?: defaultFolderId) }
    var targetWordsText by remember { mutableStateOf((initialNovel?.targetWordCount ?: 50000).toString()) }
    var pickedCoverUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pickedCoverUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialNovel == null) "Buat Proyek Novel Baru" else "Edit Informasi Novel",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 64.dp, height = 90.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            pickedCoverUri != null -> {
                                AsyncImage(
                                    model = pickedCoverUri,
                                    contentDescription = "Pratinjau sampul",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            !initialNovel?.coverPath.isNullOrBlank() -> {
                                AsyncImage(
                                    model = File(initialNovel!!.coverPath),
                                    contentDescription = "Sampul saat ini",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Rounded.Image,
                                    contentDescription = "Pilih sampul",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("pick_novel_cover_button")
                    ) {
                        Text("Unggah Sampul dari Galeri")
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Novel") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("novel_title_input")
                )

                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Nama Penulis") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("Genre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PRESET_GENRES.take(6).forEach { g ->
                        FilterChip(
                            selected = genre.equals(g, ignoreCase = true),
                            onClick = { genre = g },
                            label = { Text(g, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text("Status Penulisan", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NovelStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.labelId, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                if (folders.isNotEmpty()) {
                    Text("Folder", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = folderId == null,
                            onClick = { folderId = null },
                            label = { Text("Utama (Tanpa Folder)") }
                        )
                        folders.forEach { f ->
                            FilterChip(
                                selected = folderId == f.id,
                                onClick = { folderId = f.id },
                                label = { Text(f.name) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = targetWordsText,
                    onValueChange = { targetWordsText = it.filter { c -> c.isDigit() } },
                    label = { Text("Target Total Kata") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = synopsis,
                    onValueChange = { synopsis = it },
                    label = { Text("Sinopsis / Premis Cerita") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetWordsText.toIntOrNull()?.coerceAtLeast(1000) ?: 50000
                    onConfirm(title, author, synopsis, genre, status, folderId, pickedCoverUri, target)
                },
                modifier = Modifier.testTag("confirm_save_novel_button")
            ) {
                Text(if (initialNovel == null) "Buat Novel" else "Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
