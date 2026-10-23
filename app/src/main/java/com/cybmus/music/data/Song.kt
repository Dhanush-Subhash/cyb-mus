package com.cybmus.music.data

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val uri: Uri,
    val albumArtUri: Uri?,
    val fileType: String, // mp3, flac, m4a, etc.
    val path: String,
    val sizeBytes: Long
) {
    val durationFormatted: String
        get() {
            val totalSec = durationMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return "%d:%02d".format(min, sec)
        }

    val fileTypeDisplay: String
        get() = fileType.uppercase()

    val sizeFormatted: String
        get() = if (sizeBytes <= 0) "—" else "%.1f MB".format(sizeBytes / (1024f * 1024f))
}
