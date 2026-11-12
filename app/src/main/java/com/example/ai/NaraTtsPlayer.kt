package com.example.ai

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TtsPlaybackStatus {
    IDLE, LOADING, PLAYING, ERROR
}

data class TtsUiState(
    val status: TtsPlaybackStatus = TtsPlaybackStatus.IDLE,
    val activeLabel: String = "",
    val errorMessage: String? = null
)

class NaraTtsPlayer(
    private val aiClient: NaraAiClient,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(TtsUiState())
    val state: StateFlow<TtsUiState> = _state.asStateFlow()

    private var playJob: Job? = null
    private var activeTrack: AudioTrack? = null

    fun speak(text: String, label: String = "Membacakan naskah", autoFallback: Boolean = true) {
        stop()
        playJob = scope.launch {
            _state.value = TtsUiState(status = TtsPlaybackStatus.LOADING, activeLabel = label)
            val result = aiClient.generateTtsAudio(text, autoFallback)
            result.fold(
                onSuccess = { audioData ->
                    playAudioBytes(audioData, label)
                },
                onFailure = { err ->
                    _state.value = TtsUiState(
                        status = TtsPlaybackStatus.ERROR,
                        activeLabel = label,
                        errorMessage = err.message ?: "Gagal memutar suara NARA."
                    )
                }
            )
        }
    }

    private suspend fun playAudioBytes(audioData: GeneratedAudioData, label: String) = withContext(Dispatchers.IO) {
        var track: AudioTrack? = null
        try {
            val raw = audioData.audioBytes
            val isWav = raw.size > 44 && raw[0] == 'R'.code.toByte() && raw[1] == 'I'.code.toByte() &&
                raw[2] == 'F'.code.toByte() && raw[3] == 'F'.code.toByte()
            val sampleRate = parseSampleRate(audioData.mimeType, raw, isWav)
            val pcmData = if (isWav) raw.copyOfRange(44, raw.size) else raw
            if (pcmData.isEmpty()) {
                _state.value = TtsUiState(TtsPlaybackStatus.ERROR, label, "Data audio kosong.")
                return@withContext
            }

            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            activeTrack = track
            _state.value = TtsUiState(status = TtsPlaybackStatus.PLAYING, activeLabel = label)
            track.play()

            var offset = 0
            val chunkSize = 4096
            while (offset < pcmData.size) {
                val count = minOf(chunkSize, pcmData.size - offset)
                val written = track.write(pcmData, offset, count)
                if (written <= 0) break
                offset += written
            }
            track.stop()
            _state.value = TtsUiState(status = TtsPlaybackStatus.IDLE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = TtsUiState(
                status = TtsPlaybackStatus.ERROR,
                activeLabel = label,
                errorMessage = e.localizedMessage ?: "Gagal memutar audio."
            )
        } finally {
            runCatching { track?.release() }
            if (activeTrack == track) activeTrack = null
        }
    }

    private fun parseSampleRate(mimeType: String, raw: ByteArray, isWav: Boolean): Int {
        if (isWav && raw.size >= 28) {
            val sr = (raw[24].toInt() and 0xFF) or
                ((raw[25].toInt() and 0xFF) shl 8) or
                ((raw[26].toInt() and 0xFF) shl 16) or
                ((raw[27].toInt() and 0xFF) shl 24)
            if (sr in 8000..48000) return sr
        }
        val match = Regex("rate=(\\d+)").find(mimeType)
        val parsed = match?.groupValues?.getOrNull(1)?.toIntOrNull()
        return if (parsed != null && parsed in 8000..48000) parsed else 24000
    }

    fun stop() {
        playJob?.cancel()
        playJob = null
        runCatching {
            activeTrack?.pause()
            activeTrack?.flush()
            activeTrack?.stop()
            activeTrack?.release()
        }
        activeTrack = null
        _state.value = TtsUiState(status = TtsPlaybackStatus.IDLE)
    }

    fun clearError() {
        if (_state.value.status == TtsPlaybackStatus.ERROR) {
            _state.value = TtsUiState(status = TtsPlaybackStatus.IDLE)
        }
    }
}
