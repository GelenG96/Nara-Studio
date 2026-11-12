package com.example.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.ChatMessageEntity
import com.example.data.model.AiEngineMode
import com.example.data.model.NaraAiTool
import com.example.data.prefs.SecureKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

sealed class StreamEvent {
    data class Chunk(val accumulatedText: String) : StreamEvent()
    data class Completed(val finalText: String) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
}

data class ConnectionCheckResult(
    val success: Boolean,
    val message: String
)

data class GeneratedAudioData(
    val audioBytes: ByteArray,
    val mimeType: String
)

class NaraAiClient(
    private val context: Context,
    private val secureKeyStore: SecureKeyStore
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun testConnection(
        candidateKey: String? = null,
        engineMode: AiEngineMode,
        autoFallback: Boolean
    ): ConnectionCheckResult = withContext(Dispatchers.IO) {
        val apiKey = candidateKey?.trim()?.takeIf { it.isNotEmpty() }
            ?: secureKeyStore.decryptApiKeyOrNull()
        if (apiKey.isNullOrBlank()) {
            return@withContext ConnectionCheckResult(
                success = false,
                message = "Kunci akses AI belum diisi. Masukkan kunci terlebih dahulu."
            )
        }
        if (!isNetworkAvailable()) {
            return@withContext ConnectionCheckResult(
                success = false,
                message = "Tidak ada koneksi internet. Periksa jaringan Anda lalu coba lagi."
            )
        }

        val models = NaraModelRegistry.resolveModelChain(
            engineMode = engineMode,
            autoFallback = autoFallback
        )
        var lastError = "Koneksi gagal. Periksa kembali kunci akses Anda."

        for (modelId in models) {
            val url = "${NaraModelRegistry.OFFICIAL_API_BASE}/$modelId:generateContent"
            val payload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", "Balas singkat dengan satu kata: Siap.") })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("maxOutputTokens", 16)
                    put("temperature", 0.1)
                })
            }

            val req = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            try {
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        return@withContext ConnectionCheckResult(
                            success = true,
                            message = "Terhubung! Layanan NARA aktif dan siap menyunting."
                        )
                    } else {
                        val code = resp.code
                        lastError = mapHttpError(code)
                        // Retry fallback only for model-not-found / overload / rate-limit
                        if (code != 404 && code != 429 && code != 503 && code != 500) {
                            return@withContext ConnectionCheckResult(false, lastError)
                        }
                    }
                }
            } catch (e: UnknownHostException) {
                return@withContext ConnectionCheckResult(false, "Koneksi terputus atau jaringan tidak terjangkau.")
            } catch (e: SocketTimeoutException) {
                lastError = "Waktu tunggu koneksi habis. Coba beberapa saat lagi."
            } catch (_: Exception) {
                lastError = "Gagal menghubungi layanan NARA."
            }
        }
        ConnectionCheckResult(false, lastError)
    }

    fun streamChatOrTool(
        systemInstruction: String,
        history: List<ChatMessageEntity>,
        userPrompt: String,
        engineMode: AiEngineMode,
        tool: NaraAiTool? = null,
        autoFallback: Boolean = true,
        isMultiTurnChat: Boolean = true
    ): Flow<StreamEvent> = flow {
        val apiKey = secureKeyStore.decryptApiKeyOrNull()
        if (apiKey.isNullOrBlank()) {
            emit(StreamEvent.Error("Kunci akses AI belum diatur. Buka Pengaturan > AI NARA untuk memasukkan kunci akses Anda."))
            return@flow
        }
        if (!isNetworkAvailable()) {
            emit(StreamEvent.Error("Perangkat sedang offline. Sambungkan ke internet untuk berdiskusi dengan NARA."))
            return@flow
        }

        val models = NaraModelRegistry.resolveModelChain(
            engineMode = engineMode,
            tool = tool,
            isMultiTurnChat = isMultiTurnChat,
            autoFallback = autoFallback
        )

        val contentsArray = JSONArray().apply {
            history.takeLast(16).forEach { msg ->
                if (!msg.isError && msg.content.isNotBlank()) {
                    put(JSONObject().apply {
                        put("role", if (msg.role == "model") "model" else "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", msg.content) })
                        })
                    })
                }
            }
            put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", userPrompt) })
                })
            })
        }

        val payload = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                })
            })
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", if (tool?.prefersFastMode == true) 0.35 else 0.75)
                put("maxOutputTokens", 4096)
            })
        }

        var lastErrorMsg = "Layanan NARA sedang tidak dapat diakses."

        for (modelId in models) {
            val url = "${NaraModelRegistry.OFFICIAL_API_BASE}/$modelId:streamGenerateContent?alt=sse"
            val req = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            try {
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        val code = resp.code
                        lastErrorMsg = mapHttpError(code)
                        if (code == 404 || code == 429 || code == 503 || code == 500) {
                            // try next fallback model
                            return@use
                        } else {
                            emit(StreamEvent.Error(lastErrorMsg))
                            return@flow
                        }
                    }

                    val body = resp.body ?: run {
                        lastErrorMsg = "Respons kosong dari layanan NARA."
                        return@use
                    }

                    val accumulated = StringBuilder()
                    BufferedReader(InputStreamReader(body.byteStream(), Charsets.UTF_8)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val currentLine = line?.trim() ?: continue
                            if (currentLine.startsWith("data:")) {
                                val dataJson = currentLine.removePrefix("data:").trim()
                                if (dataJson.isEmpty() || dataJson == "[DONE]") continue
                                val chunkText = extractTextFromSseJson(dataJson)
                                if (chunkText.isNotEmpty()) {
                                    accumulated.append(chunkText)
                                    emit(StreamEvent.Chunk(NaraResponseSanitizer.sanitize(accumulated.toString())))
                                }
                            }
                        }
                    }

                    val finalClean = NaraResponseSanitizer.sanitize(accumulated.toString())
                    if (finalClean.isNotBlank()) {
                        emit(StreamEvent.Completed(finalClean))
                        return@flow
                    } else {
                        lastErrorMsg = "NARA tidak menerima teks balasan. Coba ulangi permintaan Anda."
                    }
                }
            } catch (e: UnknownHostException) {
                emit(StreamEvent.Error("Koneksi internet terputus. Periksa jaringan Anda."))
                return@flow
            } catch (e: SocketTimeoutException) {
                lastErrorMsg = "Koneksi ke layanan NARA habis waktu. Coba lagi."
            } catch (_: Exception) {
                lastErrorMsg = "Terjadi gangguan jaringan saat menghubungi NARA."
            }
        }

        emit(StreamEvent.Error(lastErrorMsg))
    }.flowOn(Dispatchers.IO)

    suspend fun generateTtsAudio(
        textToSpeak: String,
        autoFallback: Boolean = true
    ): Result<GeneratedAudioData> = withContext(Dispatchers.IO) {
        val apiKey = secureKeyStore.decryptApiKeyOrNull()
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Kunci akses AI belum diatur di Pengaturan."))
        }
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(IllegalStateException("Tidak ada koneksi internet untuk membacakan naskah."))
        }

        val cleanedText = textToSpeak.trim().take(2800)
        if (cleanedText.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Tidak ada teks untuk dibacakan."))
        }

        val models = NaraModelRegistry.resolveTtsChain(autoFallback)
        var lastError = "Gagal menghasilkan suara NARA."

        for (modelId in models) {
            val url = "${NaraModelRegistry.OFFICIAL_API_BASE}/$modelId:generateContent"
            val payload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Bacakan teks novel berikut dengan intonasi alami, hangat, dan ekspresif dalam bahasa Indonesia:\n\n$cleanedText")
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply { put("AUDIO") })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", "Kore")
                            })
                        })
                    })
                })
            }

            val req = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            try {
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        val code = resp.code
                        lastError = mapHttpError(code)
                        if (code != 404 && code != 429 && code != 503 && code != 500) {
                            return@withContext Result.failure(IllegalStateException(lastError))
                        }
                    } else {
                        val rawBody = resp.body?.string().orEmpty()
                        val parsed = extractAudioFromResponse(rawBody)
                        if (parsed != null) {
                            return@withContext Result.success(parsed)
                        } else {
                            lastError = "Format audio tidak tersedia pada respons."
                        }
                    }
                }
            } catch (e: SocketTimeoutException) {
                lastError = "Waktu tunggu sintesis suara habis."
            } catch (_: Exception) {
                lastError = "Gangguan koneksi saat menyiapkan suara NARA."
            }
        }
        Result.failure(IllegalStateException(lastError))
    }

    private fun extractTextFromSseJson(jsonStr: String): String {
        return try {
            val obj = JSONObject(jsonStr)
            val candidates = obj.optJSONArray("candidates") ?: return ""
            val sb = StringBuilder()
            for (i in 0 until candidates.length()) {
                val cand = candidates.optJSONObject(i) ?: continue
                val content = cand.optJSONObject("content") ?: continue
                val parts = content.optJSONArray("parts") ?: continue
                for (j in 0 until parts.length()) {
                    val part = parts.optJSONObject(j) ?: continue
                    val t = part.optString("text", "")
                    if (t.isNotEmpty()) sb.append(t)
                }
            }
            sb.toString()
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractAudioFromResponse(rawBody: String): GeneratedAudioData? {
        return try {
            val root = JSONObject(rawBody)
            val candidates = root.optJSONArray("candidates") ?: return null
            for (i in 0 until candidates.length()) {
                val parts = candidates.optJSONObject(i)?.optJSONObject("content")?.optJSONArray("parts") ?: continue
                for (j in 0 until parts.length()) {
                    val inlineData = parts.optJSONObject(j)?.optJSONObject("inlineData") ?: continue
                    val b64 = inlineData.optString("data", "")
                    val mime = inlineData.optString("mimeType", "audio/pcm")
                    if (b64.isNotBlank()) {
                        val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                        return GeneratedAudioData(bytes, mime)
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun mapHttpError(code: Int): String = when (code) {
        400 -> "Permintaan tidak valid atau kunci akses tidak dikenali."
        401, 403 -> "Kunci akses ditolak atau tidak memiliki izin. Periksa kembali kunci Anda di Pengaturan."
        404 -> "Layanan mesin sedang dalam pembaruan, beralih ke jalur cadangan..."
        429 -> "Batas permintaan sementara tercapai. Tunggu sebentar lalu coba lagi."
        500, 503 -> "Server sedang sibuk. Coba beberapa saat lagi."
        else -> "Koneksi gagal (kode $code). Periksa kunci akses atau jaringan Anda."
    }
}
