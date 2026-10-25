package com.cybmus.music.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.net.URLDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    suspend fun loadSongs(folderUris: List<Uri> = emptyList()): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.SIZE
        )

        val folderPrefixes = folderUris.mapNotNull(::folderPath)
            .map { it.trimEnd('/') + "/%" }
        val folderSelection = if (folderPrefixes.isEmpty()) "" else {
            " AND (" + folderPrefixes.joinToString(" OR ") {
                "${MediaStore.Audio.Media.DATA} LIKE ?"
            } + ")"
        }
        // Some devices do not mark FLAC/Opus/WAV downloads as IS_MUSIC. Include
        // every MediaStore audio MIME type plus common audio extensions too.
        val audioSelection = "(" + listOf(
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            "${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%'",
            "${MediaStore.Audio.Media.DATA} LIKE '%.flac'",
            "${MediaStore.Audio.Media.DATA} LIKE '%.opus'",
            "${MediaStore.Audio.Media.DATA} LIKE '%.ogg'",
            "${MediaStore.Audio.Media.DATA} LIKE '%.wav'",
            "${MediaStore.Audio.Media.DATA} LIKE '%.aac'",
            "${MediaStore.Audio.Media.DATA} LIKE '%.m4a'",
            "${MediaStore.Audio.Media.DATA} LIKE '%.mp3'",
            "${MediaStore.Audio.Media.DATA} LIKE '%.webm'"
        ).joinToString(" OR ") + ")"
        val selection = "$audioSelection$folderSelection"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            folderPrefixes.toTypedArray().takeIf { it.isNotEmpty() },
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Unknown"
                val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                val duration = cursor.getLong(durationCol)
                val path = cursor.getString(dataCol) ?: ""
                val mime = cursor.getString(mimeCol) ?: "audio/*"
                val albumId = cursor.getLong(albumIdCol)
                val sizeBytes = cursor.getLong(sizeCol)

                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )
                val albumArtUri = if (albumId > 0L) {
                    ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"), albumId
                    )
                } else null

                val fileType = when {
                    mime.contains("mpeg") || path.endsWith(".mp3", true) -> "mp3"
                    mime.contains("flac") || path.endsWith(".flac", true) -> "flac"
                    mime.contains("mp4") || mime.contains("m4a") || path.endsWith(".m4a", true) -> "m4a"
                    mime.contains("ogg") || path.endsWith(".ogg", true) -> "ogg"
                    mime.contains("wav") || path.endsWith(".wav", true) -> "wav"
                    mime.contains("aac") -> "aac"
                    mime.contains("opus") || path.endsWith(".opus", true) -> "opus"
                    mime.contains("webm") || path.endsWith(".webm", true) -> "webm"
                    mime.contains("amr") || path.endsWith(".amr", true) -> "amr"
                    else -> mime.substringAfterLast('/').take(4).ifEmpty { "audio" }
                }

                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        durationMs = duration,
                        uri = uri,
                        albumArtUri = albumArtUri,
                        fileType = fileType,
                        path = path,
                        sizeBytes = sizeBytes
                    )
                )
            }
        }
        songs
    }

    private fun folderPath(uri: Uri): String? {
        val documentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }
            .getOrNull() ?: return null
        val decodedId = runCatching { URLDecoder.decode(documentId, "UTF-8") }
            .getOrDefault(documentId)
        return when {
            decodedId.startsWith("raw:") -> decodedId.removePrefix("raw:")
            decodedId.startsWith("primary:") -> {
                val relativePath = decodedId.removePrefix("primary:").trim('/')
                Environment.getExternalStorageDirectory().resolve(relativePath).path
            }
            else -> {
                val separator = decodedId.indexOf(':')
                if (separator <= 0) null else {
                    val volume = decodedId.substring(0, separator)
                    val relativePath = decodedId.substring(separator + 1).trim('/')
                    "/storage/$volume/$relativePath"
                }
            }
        }
    }
}
