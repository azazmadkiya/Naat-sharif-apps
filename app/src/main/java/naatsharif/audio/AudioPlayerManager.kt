package naatsharif.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AudioPlayerState(
    val currentUrl: String = "",
    val title: String = "",
    val isPlaying: Boolean = false,
    val isPrepared: Boolean = false,
    val isLoading: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isLooping: Boolean = false,
    val errorMessage: String? = null
)

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressTrackerJob: Job? = null

    private val _playerState = MutableStateFlow(AudioPlayerState())
    val playerState: StateFlow<AudioPlayerState> = _playerState.asStateFlow()

    fun playAudio(url: String, title: String) {
        if (url.isEmpty()) {
            _playerState.update { it.copy(errorMessage = "No audio stream available for this Kalam") }
            return
        }

        if (_playerState.value.currentUrl == url && mediaPlayer != null) {
            if (_playerState.value.isPlaying) {
                pause()
            } else {
                resume()
            }
            return
        }

        stopAndRelease()

        _playerState.update {
            it.copy(
                currentUrl = url,
                title = title,
                isLoading = true,
                isPlaying = false,
                isPrepared = false,
                errorMessage = null
            )
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                isLooping = _playerState.value.isLooping
                setOnPreparedListener { mp ->
                    _playerState.update {
                        it.copy(
                            isLoading = false,
                            isPrepared = true,
                            isPlaying = true,
                            durationMs = mp.duration.toLong()
                        )
                    }
                    mp.start()
                    startProgressTracker()
                }
                setOnCompletionListener {
                    if (!_playerState.value.isLooping) {
                        _playerState.update { it.copy(isPlaying = false, currentPositionMs = 0L) }
                        stopProgressTracker()
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayerManager", "MediaPlayer error: what=$what, extra=$extra")
                    _playerState.update {
                        it.copy(
                            isLoading = false,
                            isPlaying = false,
                            errorMessage = "Audio playback error"
                        )
                    }
                    stopProgressTracker()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to start audio: ${e.message}")
            _playerState.update {
                it.copy(isLoading = false, errorMessage = "Could not load audio: ${e.message}")
            }
        }
    }

    fun pause() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                _playerState.update { it.copy(isPlaying = false) }
                stopProgressTracker()
            }
        }
    }

    fun resume() {
        mediaPlayer?.let { mp ->
            if (!mp.isPlaying && _playerState.value.isPrepared) {
                mp.start()
                _playerState.update { it.copy(isPlaying = true) }
                startProgressTracker()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { mp ->
            if (_playerState.value.isPrepared) {
                mp.seekTo(positionMs.toInt())
                _playerState.update { it.copy(currentPositionMs = positionMs) }
            }
        }
    }

    fun toggleLoop() {
        val newLoop = !_playerState.value.isLooping
        mediaPlayer?.isLooping = newLoop
        _playerState.update { it.copy(isLooping = newLoop) }
    }

    fun stopAndRelease() {
        stopProgressTracker()
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error releasing player: ${e.message}")
        } finally {
            mediaPlayer = null
            _playerState.update { AudioPlayerState() }
        }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressTrackerJob = scope.launch {
            while (true) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _playerState.update { it.copy(currentPositionMs = mp.currentPosition.toLong()) }
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = null
    }

    fun clearError() {
        _playerState.update { it.copy(errorMessage = null) }
    }
}
