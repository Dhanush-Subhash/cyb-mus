package com.cybmus.music.ui

import android.app.Application
import android.content.ComponentName
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.audiofx.Equalizer
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.content.edit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.cybmus.music.data.EqualizerPreset
import com.cybmus.music.data.EqualizerPresets
import com.cybmus.music.data.MusicRepository
import com.cybmus.music.data.Song
import com.cybmus.music.service.PlaybackService
import com.cybmus.music.ui.theme.CybColors
import com.cybmus.music.ui.theme.defaultCybColors
import com.cybmus.music.ui.theme.toCybColors
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.palette.graphics.Palette
import java.io.InputStream
import org.json.JSONArray
import org.json.JSONObject

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)

    var songs by mutableStateOf<List<Song>>(emptyList())
        private set

    var currentSong by mutableStateOf<Song?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var progress by mutableStateOf(0f) // 0f..1f
        private set

    var positionMs by mutableStateOf(0L)
        private set

    var durationMs by mutableStateOf(0L)
        private set

    var queue by mutableStateOf<List<Song>>(emptyList())
        private set

    var playlists by mutableStateOf<Map<String, List<Long>>>(emptyMap())
        private set

    var editingSong by mutableStateOf<Song?>(null)
        private set

    var currentIndex by mutableStateOf(0)
        private set

    var shuffleEnabled by mutableStateOf(false)
        private set

    var repeatEnabled by mutableStateOf(false)
        private set

    var sleepTimerMinutes by mutableStateOf<Int?>(null)
        private set

    var selectedPreset by mutableStateOf(EqualizerPresets.ALL[1]) // default Relax
        private set

    var customPresets by mutableStateOf<List<EqualizerPreset>>(emptyList())
        private set

    var cybColors by mutableStateOf(defaultCybColors())
        private set

    var wallpaperUri by mutableStateOf<Uri?>(null)
        private set

    var customBandLevels by mutableStateOf(
        EqualizerPresets.ALL[1].bandLevels.map { it.toInt() }
    )
        private set

    var musicFolderUris by mutableStateOf<List<Uri>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var equalizerEnabled by mutableStateOf(true)
        private set

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var equalizer: Equalizer? = null
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private val preferences: SharedPreferences = application.getSharedPreferences("cyb-mus", 0)

    init {
        wallpaperUri = preferences.getString("wallpaper_uri", null)?.let(Uri::parse)
        musicFolderUris = preferences.getStringSet("music_folders", emptySet())
            .orEmpty()
            .map(Uri::parse)
        playlists = loadPlaylists()
        customPresets = loadCustomEqPresets()
        shuffleEnabled = preferences.getBoolean("shuffle_enabled", false)
        repeatEnabled = preferences.getBoolean("repeat_enabled", false)
        connectController()
        wallpaperUri?.let(::setWallpaper)
    }

    private fun connectController() {
        val sessionToken = SessionToken(
            getApplication(),
            ComponentName(getApplication(), PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            mediaController?.addListener(playerListener)
            mediaController?.shuffleModeEnabled = shuffleEnabled
            mediaController?.repeatMode = if (repeatEnabled) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            startProgressUpdates()
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let { item ->
                val id = item.mediaId.toLongOrNull()
                currentSong = songs.find { it.id == id }
                currentIndex = songs.indexOfFirst { it.id == id }.coerceAtLeast(0)
                updateQueue()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                durationMs = mediaController?.duration ?: 0L
            }
        }
    }

    fun loadLibrary() {
        viewModelScope.launch {
            isLoading = true
            songs = repository.loadSongs(musicFolderUris).shuffled()
            currentSong?.let { playing ->
                val newIndex = songs.indexOfFirst { it.id == playing.id }
                if (newIndex >= 0) currentIndex = newIndex else {
                    currentSong = null
                    currentIndex = 0
                }
            }
            updateQueue()
            isLoading = false
            if (songs.isNotEmpty() && currentSong == null) {
                // optional: auto-prepare first
            }
        }
    }

    fun playSong(song: Song, fromIndex: Int = -1) {
        val index = if (fromIndex >= 0) fromIndex else songs.indexOfFirst { it.id == song.id }
        if (index < 0) return
        currentIndex = index
        currentSong = song
        updateQueue()

        val items = songs.map { s ->
            MediaItem.Builder()
                .setUri(s.uri)
                .setMediaId(s.id.toString())
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(s.title)
                        .setArtist(s.artist)
                        .build()
                )
                .build()
        }
        mediaController?.setMediaItems(items, index, 0L)
        mediaController?.prepare()
        mediaController?.play()
        attachEqualizer()
    }

    fun playPlaylist(name: String) {
        val playlistSongs = playlists[name].orEmpty().mapNotNull { id -> songs.find { it.id == id } }
        if (playlistSongs.isEmpty()) return

        currentSong = playlistSongs.first()
        currentIndex = songs.indexOfFirst { it.id == currentSong?.id }.coerceAtLeast(0)
        queue = playlistSongs.drop(1).take(20)
        val items = playlistSongs.map { song ->
            MediaItem.Builder().setUri(song.uri).setMediaId(song.id.toString()).setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).build()
            ).build()
        }
        mediaController?.setMediaItems(items, 0, 0L)
        mediaController?.prepare()
        mediaController?.play()
        attachEqualizer()
    }

    fun togglePlayPause() {
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun playNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun playPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }

    fun toggleShuffle() {
        shuffleEnabled = !shuffleEnabled
        mediaController?.shuffleModeEnabled = shuffleEnabled
        preferences.edit { putBoolean("shuffle_enabled", shuffleEnabled) }
    }

    fun toggleRepeat() {
        repeatEnabled = !repeatEnabled
        mediaController?.repeatMode = if (repeatEnabled) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        preferences.edit { putBoolean("repeat_enabled", repeatEnabled) }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        sleepTimerMinutes = minutes
        if (minutes != null) {
            sleepTimerJob = viewModelScope.launch {
                delay(minutes * 60_000L)
                mediaController?.pause()
                sleepTimerMinutes = null
            }
        }
    }

    fun seekTo(fraction: Float) {
        val dur = mediaController?.duration ?: return
        if (dur > 0) {
            mediaController?.seekTo((fraction * dur).toLong())
        }
    }

    private fun updateQueue() {
        if (songs.size < 2 || currentIndex !in songs.indices) {
            queue = emptyList()
            return
        }
        val next = mutableListOf<Song>()
        for (i in 1 until songs.size) {
            val idx = (currentIndex + i) % songs.size
            next.add(songs[idx])
            if (next.size == 20) break
        }
        queue = next
    }

    fun reorderQueue(from: Int, to: Int) {
        if (from !in queue.indices || to !in queue.indices || from == to) return
        val reordered = queue.toMutableList().apply { add(to, removeAt(from)) }
        queue = reordered

        // Keep the media session's next items in the same order as the visible queue.
        currentSong?.let { nowPlaying ->
            val ordered = listOf(nowPlaying) + reordered + songs.filterNot { candidate ->
                candidate.id == nowPlaying.id || reordered.any { it.id == candidate.id }
            }
            val wasPlaying = mediaController?.isPlaying == true
            val position = mediaController?.currentPosition ?: 0L
            val items = ordered.map { song ->
                MediaItem.Builder().setUri(song.uri).setMediaId(song.id.toString()).setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).build()
                ).build()
            }
            mediaController?.setMediaItems(items, 0, position)
            mediaController?.prepare()
            if (wasPlaying) mediaController?.play()
        }
    }

    fun openPlaylistEditor(song: Song) {
        editingSong = song
    }

    fun closePlaylistEditor() {
        editingSong = null
    }

    fun createPlaylist(name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || playlists.containsKey(cleanName)) return
        playlists = playlists + (cleanName to emptyList())
        savePlaylists()
    }

    fun deletePlaylist(name: String) {
        playlists = playlists - name
        savePlaylists()
    }

    fun toggleSongInPlaylist(name: String, songId: Long) {
        val current = playlists[name] ?: return
        val updated = if (songId in current) current - songId else current + songId
        playlists = playlists + (name to updated)
        savePlaylists()
    }

    fun movePlaylistSong(name: String, from: Int, to: Int) {
        val current = playlists[name].orEmpty()
        if (from !in current.indices || to !in current.indices || from == to) return
        val reordered = current.toMutableList().apply { add(to, removeAt(from)) }
        playlists = playlists + (name to reordered)
        savePlaylists()
    }

    private fun loadPlaylists(): Map<String, List<Long>> {
        val raw = preferences.getString("playlists", null) ?: return emptyMap()
        return runCatching {
            val result = linkedMapOf<String, List<Long>>()
            val array = JSONArray(raw)
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val ids = item.optJSONArray("songs") ?: JSONArray()
                result[item.getString("name")] = List(ids.length()) { ids.getLong(it) }
            }
            result
        }.getOrDefault(emptyMap())
    }

    private fun savePlaylists() {
        val array = JSONArray()
        playlists.forEach { (name, ids) ->
            val item = JSONObject().put("name", name)
            item.put("songs", JSONArray().apply { ids.forEach(::put) })
            array.put(item)
        }
        preferences.edit { putString("playlists", array.toString()) }
    }

    fun addMusicFolder(uri: Uri) {
        runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        musicFolderUris = (musicFolderUris + uri).distinct()
        saveMusicFolders()
        loadLibrary()
    }

    fun removeMusicFolder(uri: Uri) {
        musicFolderUris = musicFolderUris.filterNot { it == uri }
        saveMusicFolders()
        loadLibrary()
    }

    private fun saveMusicFolders() {
        preferences.edit {
            putStringSet("music_folders", musicFolderUris.map(Uri::toString).toSet())
        }
    }

    fun applyPreset(preset: EqualizerPreset) {
        selectedPreset = preset
        customBandLevels = preset.bandLevels.map { it.toInt() }
        equalizer?.let { eq ->
            val bands = eq.numberOfBands.toInt()
            val levels = preset.bandLevels
            for (i in 0 until minOf(bands, levels.size)) {
                val range = eq.getBandLevelRange()
                val level = levels[i].coerceIn(range[0], range[1])
                eq.setBandLevel(i.toShort(), level)
            }
        }
    }

    fun saveCustomPreset(name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val nextId = (customPresets.minOfOrNull { it.id } ?: 0) - 1
        val preset = EqualizerPreset(nextId, cleanName, "Custom preset", customBandLevels.map { it.toShort() }.toShortArray())
        customPresets = customPresets + preset
        selectedPreset = preset
        persistCustomEqPresets()
    }

    fun removeCustomPreset(preset: EqualizerPreset) {
        if (preset.id >= 0) return
        customPresets = customPresets.filterNot { it.id == preset.id }
        if (selectedPreset.id == preset.id) applyPreset(EqualizerPresets.ALL[1])
        persistCustomEqPresets()
    }

    private fun persistCustomEqPresets() {
        val array = JSONArray()
        customPresets.forEach { preset ->
            array.put(JSONObject().apply {
                put("id", preset.id)
                put("name", preset.name)
                put("levels", JSONArray(preset.bandLevels.toList()))
            })
        }
        preferences.edit { putString("custom_eq_presets", array.toString()) }
    }

    private fun loadCustomEqPresets(): List<EqualizerPreset> {
        return try {
            val array = JSONArray(preferences.getString("custom_eq_presets", "[]"))
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val levels = item.getJSONArray("levels")
                    if (levels.length() == 5) {
                        add(EqualizerPreset(
                            item.getInt("id"),
                            item.getString("name"),
                            "Custom preset",
                            ShortArray(5) { levels.getInt(it).toShort() }
                        ))
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setCustomBandLevel(index: Int, level: Int) {
        if (index !in customBandLevels.indices) return
        val updated = customBandLevels.toMutableList()
        updated[index] = level.coerceIn(-1500, 1500)
        customBandLevels = updated
        equalizer?.let { eq ->
            if (index < eq.numberOfBands) {
                val range = eq.getBandLevelRange()
                eq.setBandLevel(index.toShort(), level.coerceIn(range[0].toInt(), range[1].toInt()).toShort())
            }
        }
    }

    @JvmName("setEqualizerEnabledMethod")
    fun setEqualizerEnabled(enabled: Boolean) {
        equalizerEnabled = enabled
        equalizer?.enabled = enabled
    }

    private fun attachEqualizer() {
        try {
            val sessionId = mediaController?.sessionExtras?.getInt("audio_session_id", 0) ?: 0
            equalizer?.release()
            equalizer = Equalizer(0, sessionId).apply {
                enabled = equalizerEnabled
            }
            applyPreset(selectedPreset)
        } catch (e: Exception) {
            // Some devices restrict Equalizer
            e.printStackTrace()
        }
    }

    fun setWallpaper(uri: Uri?) {
        wallpaperUri = uri
        if (uri != null) {
            runCatching {
                getApplication<Application>().contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        preferences.edit {
            if (uri == null) remove("wallpaper_uri") else putString("wallpaper_uri", uri.toString())
        }
        if (uri == null) {
            cybColors = defaultCybColors()
            return
        }
        viewModelScope.launch {
            try {
                val input: InputStream? = getApplication<Application>().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(input)
                input?.close()
                if (bitmap != null) {
                    val palette = Palette.from(bitmap).generate()
                    cybColors = palette.toCybColors()
                    // We keep a downscaled version for blur later if needed
                }
            } catch (e: Exception) {
                e.printStackTrace()
                cybColors = defaultCybColors()
            }
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                mediaController?.let { c ->
                    if (c.isPlaying || c.playbackState == Player.STATE_READY) {
                        positionMs = c.currentPosition
                        durationMs = c.duration.coerceAtLeast(0)
                        progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
                    }
                }
                delay(200)
            }
        }
    }

    override fun onCleared() {
        progressJob?.cancel()
        equalizer?.release()
        mediaController?.release()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
