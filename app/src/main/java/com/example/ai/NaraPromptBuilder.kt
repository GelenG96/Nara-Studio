package com.example.ai

import com.example.data.local.ChapterEntity
import com.example.data.local.NovelEntity
import com.example.data.local.StoryBibleEntryEntity
import com.example.data.model.AiContextScope
import com.example.data.model.EditingIntensity
import com.example.data.model.NaraAiTool
import com.example.data.model.StoryBibleCategory

object NaraPromptBuilder {

    fun buildSystemInstruction(
        authorPenName: String,
        novel: NovelEntity?,
        storyBible: List<StoryBibleEntryEntity>,
        contextTextBlock: String,
        intensity: EditingIntensity? = null
    ): String {
        val sb = StringBuilder()
        sb.appendLine("IDENTITAS MUTLAK:")
        sb.appendLine("Kamu adalah NARA, editor novel profesional dan rekan diskusi kreatif bawaan aplikasi penulisan novel ini.")
        sb.appendLine("ATURAN KERAS IDENTITAS: Kamu TIDAK PERNAH menyebut dirimu Gemini, Google, Google AI, model bahasa besar (LLM), atau nama penyedia/model teknis apa pun. Jika pengguna bertanya siapa dirimu, model apa yang dipakai, atau siapa pembuatmu, jawab dengan santai bahwa kamu adalah NARA, editor novel bawaan aplikasi.")
        sb.appendLine()
        sb.appendLine("KEPRIBADIAN & GAYA KOMUNIKASI:")
        sb.appendLine("- Santai, hangat, cerdas, sesekali humoris secara natural, tetapi tajam dan kritis dalam menilai karya sastra.")
        sb.appendLine("- Jangan seperti robot atau asisten layanan pelanggan. Jangan menggunakan kalimat pembuka/penutup klise yang berulang.")
        sb.appendLine("- Berani mengkritik dengan jujur bila tulisan terasa klise, datar, dialognya kaku, pacing-nya tersendat, atau motivasinya lemah—sambil memberi solusi konkret.")
        sb.appendLine("- Tidak asal setuju dengan pengguna jika ide atau adegan memang bermasalah secara naratif.")
        sb.appendLine("- Pahami dan pertahankan suara khas penulis ($authorPenName) saat menyunting; jangan mengubah maksud cerita, karakter, atau alur tanpa izin.")
        sb.appendLine("- Jangan pernah mengarang fakta atau konteks cerita yang belum diberikan penulis. Jika detail tidak ada di konteks, katakan dengan jujur atau tanyakan.")
        sb.appendLine()
        sb.appendLine("ATURAN FORMAT OUTPUT (WAJIB DIPATUHI):")
        sb.appendLine("- DILARANG menggunakan heading markdown dengan simbol #, ##, atau ###.")
        sb.appendLine("- DILARANG menampilkan JSON mentah, blok kode (```), atau format teknis backend.")
        sb.appendLine("- Gunakan paragraf alami yang enak dibaca, pemisah baris yang wajar, dan penekanan seperlunya.")
        sb.appendLine("- Jika diminta menyunting, menulis ulang, atau melanjutkan cerita, berikan teks prosa yang bersih dan langsung siap disalin ke editor novel tanpa embel-embel format aneh.")

        if (intensity != null) {
            sb.appendLine()
            sb.appendLine("TINGKAT PENYUNTINGAN AKTIF: ${intensity.labelId}")
            sb.appendLine(intensity.instructionId)
        }

        if (novel != null) {
            sb.appendLine()
            sb.appendLine("PROYEK NOVEL YANG SEDANG DIBUKA:")
            sb.appendLine("Judul: ${novel.title}")
            if (novel.author.isNotBlank()) sb.appendLine("Penulis: ${novel.author}")
            sb.appendLine("Genre: ${novel.genre}")
            if (novel.synopsis.isNotBlank()) sb.appendLine("Sinopsis: ${novel.synopsis}")
        }

        if (storyBible.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("DATABASE REFERENSI CERITA (STORY BIBLE):")
            val grouped = storyBible.groupBy { StoryBibleCategory.fromName(it.category) }
            grouped.forEach { (cat, entries) ->
                sb.appendLine("[${cat.labelId}]")
                entries.take(15).forEach { entry ->
                    val sub = if (entry.subtitle.isNotBlank()) " (${entry.subtitle})" else ""
                    sb.appendLine("- ${entry.title}$sub: ${entry.content.take(400)}")
                }
            }
        }

        if (contextTextBlock.isNotBlank()) {
            sb.appendLine()
            sb.appendLine("KONTEKS NASKAH YANG SEDANG DIBAWA:")
            sb.appendLine(contextTextBlock.take(28000))
        }

        return sb.toString()
    }

    fun buildContextBlock(
        scope: AiContextScope,
        selectedText: String,
        activeChapter: ChapterEntity?,
        selectedChapters: List<ChapterEntity>,
        allChapters: List<ChapterEntity>
    ): String {
        return when (scope) {
            AiContextScope.SELECTION -> {
                if (selectedText.isNotBlank()) {
                    "Teks yang dipilih dari bab '${activeChapter?.title ?: "Aktif"}':\n\"\"\"\n$selectedText\n\"\"\""
                } else {
                    activeChapter?.let {
                        "Tidak ada sorotan teks khusus; berikut isi bab aktif '${it.title}':\n${it.content}"
                    }.orEmpty()
                }
            }
            AiContextScope.ACTIVE_CHAPTER -> {
                activeChapter?.let {
                    buildString {
                        appendLine("Bab Aktif: ${it.title} (${it.wordCount} kata)")
                        if (it.notes.isNotBlank()) appendLine("Catatan Bab Penulis: ${it.notes}")
                        appendLine("Isi Bab:")
                        appendLine(it.content)
                    }
                }.orEmpty()
            }
            AiContextScope.MULTIPLE_CHAPTERS -> {
                val list = selectedChapters.ifEmpty { listOfNotNull(activeChapter) }
                list.joinToString("\n\n---\n\n") { ch ->
                    "Bab: ${ch.title} (${ch.wordCount} kata)\nCatatan: ${ch.notes}\n${ch.content.take(7000)}"
                }
            }
            AiContextScope.WHOLE_NOVEL -> {
                if (allChapters.isEmpty()) ""
                else {
                    val perChapterBudget = (24000 / allChapters.size.coerceAtLeast(1)).coerceIn(800, 5000)
                    allChapters.joinToString("\n\n===\n\n") { ch ->
                        "Bab: ${ch.title} (${ch.wordCount} kata)\nCatatan: ${ch.notes}\n${ch.content.take(perChapterBudget)}"
                    }
                }
            }
        }
    }

    fun buildToolPrompt(tool: NaraAiTool, intensity: EditingIntensity, extraNotes: String = ""): String {
        val base = when (tool) {
            NaraAiTool.PROOFREAD ->
                "Lakukan koreksi ejaan (EYD V), tanda baca, typo, dan tata bahasa pada naskah di konteks sesuai tingkat ${intensity.labelId}. Berikan hasil naskah yang sudah diperbaiki secara bersih agar langsung siap disalin."
            NaraAiTool.PRO_EDIT ->
                "Lakukan penyuntingan novel profesional pada naskah di konteks (${intensity.labelId}). Pertahankan suara penulis dan alur cerita, rapikan alur kalimat, kejelasan adegan, dan kekuatan diksi. Berikan teks hasil suntingan yang bersih."
            NaraAiTool.REWRITE ->
                "Tulis ulang (rewrite) bagian naskah pada konteks dengan tingkat ${intensity.labelId} agar kalimat lebih hidup, ritmis, dan ekspresif tanpa mengubah fakta plot maupun karakter."
            NaraAiTool.EXPAND_NARRATION ->
                "Kembangkan narasi dan deskripsi adegan pada konteks dengan detail sensorik (visual, suara, atmosfer, bahasa tubuh) yang kuat dan imersif tanpa bertele-tele. Berikan hasil teks siap pakai."
            NaraAiTool.POLISH_DIALOGUE ->
                "Sunting dialog-dialog pada konteks agar terasa lebih natural, memiliki subtext, dan mencerminkan karakter masing-masing tokoh. Berikan naskah hasil perbaikan yang bersih."
            NaraAiTool.CHARACTER_ANALYSIS ->
                "Berikan analisis karakter mendalam berdasarkan naskah dan database referensi cerita: evaluasi konsistensi sifat, motivasi, suara tokoh, serta tunjukkan jika ada perilaku tokoh yang terasa janggal atau kurang meyakinkan."
            NaraAiTool.PLOT_ANALYSIS ->
                "Analisis alur cerita pada konteks: nilai kekuatan hook, sebab-akibat antaradegan, titik balik, dan identifikasi bagian yang terasa menggantung atau lemah secara dramatis."
            NaraAiTool.CONSISTENCY_CHECK ->
                "Periksa konsistensi cerita secara teliti antara naskah dan database referensi (kronologi, sifat tokoh, lokasi, aturan dunia, detail kecil). Sebutkan temuan ketidakkonsistenan secara spesifik."
            NaraAiTool.PACING_CHECK ->
                "Evaluasi pacing (tempo cerita) dalam naskah konteks. Tunjukkan bagian mana yang terlalu terburu-buru hingga emosi tak sempat tumbuh, dan bagian mana yang terlalu lambat atau repetitif."
            NaraAiTool.CONFLICT_DEVELOPMENT ->
                "Evaluasi konflik dalam konteks dan tawarkan cara menajamkan ketegangan, taruhan emosional, serta rintangan karakter agar pembaca terus penasaran."
            NaraAiTool.BRAINSTORM ->
                "Ajak aku brainstorming arah cerita yang segar, tidak klise, namun tetap masuk akal dengan karakter dan dunia cerita yang sudah ada."
            NaraAiTool.CONTINUE_STORY ->
                "Lanjutkan cerita tepat dari akhir naskah konteks sekitar 3 sampai 5 paragraf yang natural, selaras dengan gaya bahasa, nada emosi, dan fakta cerita penulis. Langsung berikan prosa lanjutannya tanpa pembukaan basa-basi."
            NaraAiTool.CHAPTER_SCORE ->
                "Berikan penilaian jujur layaknya editor senior terhadap bab ini (aspek pembuka, karakter, dialog, deskripsi, emosi, dan ketegangan), lengkap dengan kritik tajam dan saran perbaikan paling berdampak."
            NaraAiTool.REPETITION_DETECT ->
                "Deteksi kata yang terlalu sering berulang, struktur kalimat yang monoton/kaku, kalimat pasif berlebihan, dan ungkapan klise dalam konteks. Tunjukkan contoh spesifik dan alternatif perbaikannya."
            NaraAiTool.MOOD_EMOTION ->
                "Analisis suasana, atmosfer, dan kurva emosi dalam adegan di konteks. Evaluasi apakah emosi adegan sampai ke pembaca dan bagaimana memperkuat resonansinya."
        }
        return if (extraNotes.isNotBlank()) "$base\n\nArahan tambahan penulis: $extraNotes" else base
    }
}

object NaraResponseSanitizer {
    private val markdownHeadingRegex = Regex("(?m)^#{1,6}\\s*")
    private val codeFenceRegex = Regex("```[a-zA-Z]*\\n?")

    fun sanitize(raw: String): String {
        var out = raw
            .replace(codeFenceRegex, "")
            .replace("```", "")
            .replace(markdownHeadingRegex, "")
            .trim()
        // Safeguard against accidental backend/provider mentions
        out = out
            .replace(Regex("(?i)\\bgemini[-\\w.]*\\b"), "NARA")
            .replace(Regex("(?i)\\bgoogle\\s*ai\\b"), "NARA")
        return out
    }
}
