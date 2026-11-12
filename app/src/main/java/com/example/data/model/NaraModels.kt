package com.example.data.model

import com.squareup.moshi.JsonClass

enum class NovelStatus(val labelId: String, val labelEn: String) {
    DRAFT("Draf Awal", "Draft"),
    WRITING("Sedang Ditulis", "Writing"),
    EDITING("Tahap Revisi", "Editing"),
    COMPLETED("Selesai", "Completed");

    companion object {
        fun fromName(name: String?): NovelStatus =
            entries.firstOrNull { it.name == name } ?: WRITING
    }
}

enum class TextAlignOption(val labelId: String) {
    START("Rata Kiri"),
    CENTER("Rata Tengah"),
    END("Rata Kanan"),
    JUSTIFY("Rata Kanan-Kiri");

    companion object {
        fun fromName(name: String?): TextAlignOption =
            entries.firstOrNull { it.name == name } ?: START
    }
}

enum class StoryBibleCategory(val labelId: String, val descriptionId: String) {
    CHARACTER("Profil Karakter", "Biodata, sifat, motivasi, dan ciri khas tokoh"),
    RELATIONSHIP("Hubungan Antarkarakter", "Dinamika, aliansi, rivalitas, dan ikatan emosional"),
    LOCATION("Lokasi", "Latar tempat, atmosfer, geografi, dan detail visual"),
    WORLDBUILDING("Dunia Cerita", "Aturan dunia, budaya, sistem sihir/teknologi, sejarah"),
    CHRONOLOGY("Kronologi", "Garis waktu peristiwa penting dari awal hingga akhir"),
    PLOT("Catatan Plot", "Struktur babak, titik balik, dan garis besar alur"),
    CONFLICT("Konflik", "Konflik eksternal, internal, dan taruhan cerita"),
    IMPORTANT_INFO("Informasi Penting", "Fakta kunci, petunjuk (foreshadowing), dan rahasia"),
    AUTHOR_NOTE("Catatan Penulis", "Ide mentah, riset, dan pengingat gaya penulisan");

    companion object {
        fun fromName(name: String?): StoryBibleCategory =
            entries.firstOrNull { it.name == name } ?: CHARACTER
    }
}

@JsonClass(generateAdapter = true)
data class RichSpan(
    val start: Int,
    val end: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false
)

enum class ThemeMode(val labelId: String) {
    SYSTEM("Ikuti Sistem"),
    LIGHT("Mode Terang"),
    DARK("Mode Gelap");

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

enum class WallpaperPageTarget(val key: String, val labelId: String) {
    GLOBAL("global", "Wallpaper Global"),
    HOME("home", "Halaman Utama"),
    LIBRARY("library", "Perpustakaan"),
    EDITOR("editor", "Editor Novel"),
    AI("ai", "Studio NARA");

    companion object {
        fun fromKey(key: String?): WallpaperPageTarget =
            entries.firstOrNull { it.key == key } ?: GLOBAL
    }
}

enum class WallpaperSourceType {
    NONE, BUILTIN, GALLERY;

    companion object {
        fun fromName(name: String?): WallpaperSourceType =
            entries.firstOrNull { it.name == name } ?: BUILTIN
    }
}

enum class BuiltinWallpaper(val id: String, val titleId: String, val isDark: Boolean) {
    PAPER_DAWN("paper_dawn", "Fajar Kertas", false),
    MIDNIGHT_STUDY("midnight_study", "Ruang Tulis Malam", true),
    BOTANICAL_INK("botanical_ink", "Tinta Botani", false);

    companion object {
        fun fromId(id: String?): BuiltinWallpaper =
            entries.firstOrNull { it.id == id } ?: PAPER_DAWN
    }
}

enum class WallpaperScaleMode(val labelId: String) {
    CROP("Penuhi Layar (Crop)"),
    FIT("Sesuaikan Utuh (Fit)"),
    FILL("Regangkan (Fill)");

    companion object {
        fun fromName(name: String?): WallpaperScaleMode =
            entries.firstOrNull { it.name == name } ?: CROP
    }
}

enum class WallpaperAlignment(val labelId: String) {
    CENTER("Tengah"),
    TOP("Atas"),
    BOTTOM("Bawah");

    companion object {
        fun fromName(name: String?): WallpaperAlignment =
            entries.firstOrNull { it.name == name } ?: CENTER
    }
}

data class WallpaperConfig(
    val sourceType: WallpaperSourceType = WallpaperSourceType.BUILTIN,
    val builtinId: String = BuiltinWallpaper.PAPER_DAWN.id,
    val galleryPath: String = "",
    val scaleMode: WallpaperScaleMode = WallpaperScaleMode.CROP,
    val alignment: WallpaperAlignment = WallpaperAlignment.CENTER,
    val zoomScale: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val opacity: Float = 0.42f,
    val blurRadiusDp: Float = 0f,
    val overlayScrimAlpha: Float = 0.28f,
    val panelOpacity: Float = 0.90f
)

data class EditorSettings(
    val fontKey: String = "LITERATA",
    val fontSizeSp: Float = 17f,
    val lineSpacingMultiplier: Float = 1.6f,
    val horizontalMarginDp: Float = 20f,
    val paragraphSpacingDp: Float = 12f,
    val firstLineIndentDp: Float = 18f,
    val autoSaveIntervalSec: Int = 4,
    val autoBackupEnabled: Boolean = true
)

enum class AiEngineMode(val labelId: String, val descriptionId: String) {
    STANDARD("Mode Standar Sastra", "Seimbang untuk penyuntingan novel, dialog, dan diskusi kreatif."),
    DEEP("Mode Penalaran Mendalam", "Analisis struktur plot kompleks, konsistensi dunia, dan penulisan ulang mendalam."),
    INTERACTIVE("Mode Interaktif Kompleks", "Diskusi panjang lintas banyak bab dengan nuansa karakter mendalam."),
    FAST("Mode Respons Cepat", "Koreksi ejaan kilat, tata bahasa, dan pemeriksaan repetisi.");

    companion object {
        fun fromName(name: String?): AiEngineMode =
            entries.firstOrNull { it.name == name } ?: STANDARD
    }
}

enum class AiContextScope(val labelId: String) {
    SELECTION("Teks Terpilih"),
    ACTIVE_CHAPTER("Bab Aktif"),
    MULTIPLE_CHAPTERS("Beberapa Bab"),
    WHOLE_NOVEL("Seluruh Proyek Novel")
}

enum class EditingIntensity(val labelId: String, val instructionId: String) {
    LIGHT("Koreksi Ringan", "Perbaiki ejaan (EYD V), tanda baca, typo, dan tata bahasa tanpa mengubah diksi atau struktur kalimat penulis."),
    PROFESSIONAL("Penyuntingan Profesional", "Haluskan ritme kalimat, perkuat deskripsi dan dialog, rapikan flow narasi sambil tetap mempertahankan suara dan gaya khas penulis."),
    DEEP_REWRITE("Penulisan Ulang Mendalam", "Tingkatkan kedalaman emosi, show-don't-tell, ketegangan, dan kekuatan sastra secara menyeluruh tanpa mengubah maksud cerita atau fakta plot.")
}

enum class NaraAiTool(
    val labelId: String,
    val shortHint: String,
    val prefersDeepReasoning: Boolean = false,
    val prefersFastMode: Boolean = false,
    val isRewriteOutput: Boolean = false
) {
    PROOFREAD("Koreksi Ejaan & Tata Bahasa", "Rapikan EYD, tanda baca, dan kalimat rancu.", prefersFastMode = true, isRewriteOutput = true),
    PRO_EDIT("Penyuntingan Novel Profesional", "Sunting gaya bahasa, ritme, dan kejelasan narasi.", isRewriteOutput = true),
    REWRITE("Rewrite Kalimat & Paragraf", "Susun ulang paragraf agar lebih hidup dan bertenaga.", isRewriteOutput = true),
    EXPAND_NARRATION("Pengembangan Narasi & Deskripsi", "Perkaya detail sensorik, atmosfer, dan emosi adegan.", isRewriteOutput = true),
    POLISH_DIALOGUE("Penyuntingan Dialog Natural", "Buat dialog terasa hidup, berkarakter, dan tidak kaku.", isRewriteOutput = true),
    CHARACTER_ANALYSIS("Analisis Karakter", "Bedah motivasi, konsistensi sifat, dan suara tokoh.", prefersDeepReasoning = true),
    PLOT_ANALYSIS("Analisis Alur Cerita", "Evaluasi struktur adegan, hook, dan perkembangan plot.", prefersDeepReasoning = true),
    CONSISTENCY_CHECK("Pemeriksaan Konsistensi", "Cari kontradiksi timeline, nama, lokasi, dan fakta dunia.", prefersDeepReasoning = true),
    PACING_CHECK("Pemeriksaan Pacing", "Ukur tempo cerita: bagian terlalu lambat atau terburu-buru."),
    CONFLICT_DEVELOPMENT("Pengembangan Konflik", "Perkuat taruhan cerita, rintangan, dan ketegangan dramatis."),
    BRAINSTORM("Brainstorming Ide Cerita", "Diskusi ide segar, twist, dan kemungkinan arah cerita."),
    CONTINUE_STORY("Melanjutkan Cerita", "Tulis kelanjutan adegan yang selaras dengan gaya dan konteks.", isRewriteOutput = true),
    CHAPTER_SCORE("Penilaian Kualitas Bab", "Skor jujur dan kritik konstruktif layaknya editor senior.", prefersDeepReasoning = true),
    REPETITION_DETECT("Deteksi Kalimat Repetitif & Kaku", "Temukan kata berulang, klise, dan kalimat pasif berlebihan.", prefersFastMode = true),
    MOOD_EMOTION("Analisis Suasana & Emosi", "Evaluasi resonansi emosional dan atmosfer adegan.")
}

enum class ConnectionTestStatus {
    IDLE, TESTING, SUCCESS, ERROR
}

@JsonClass(generateAdapter = true)
data class NovelBackupBundle(
    val version: Int = 1,
    val exportedAt: Long,
    val folders: List<FolderBackupDto>,
    val novels: List<NovelBackupDto>,
    val chapters: List<ChapterBackupDto>,
    val storyBibleEntries: List<StoryBibleBackupDto>
)

@JsonClass(generateAdapter = true)
data class FolderBackupDto(
    val id: Long,
    val parentFolderId: Long?,
    val name: String,
    val sortOrder: Int,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class NovelBackupDto(
    val id: Long,
    val folderId: Long?,
    val title: String,
    val author: String,
    val synopsis: String,
    val genre: String,
    val status: String,
    val isFavorite: Boolean,
    val isArchived: Boolean,
    val targetWordCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@JsonClass(generateAdapter = true)
data class ChapterBackupDto(
    val id: Long,
    val novelId: Long,
    val parentChapterId: Long?,
    val title: String,
    val content: String,
    val richSpansJson: String,
    val alignment: String,
    val notes: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@JsonClass(generateAdapter = true)
data class StoryBibleBackupDto(
    val id: Long,
    val novelId: Long,
    val category: String,
    val title: String,
    val subtitle: String,
    val content: String,
    val tags: String,
    val sortOrder: Int,
    val updatedAt: Long
)
