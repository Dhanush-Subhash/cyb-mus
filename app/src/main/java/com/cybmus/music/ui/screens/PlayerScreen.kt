package com.cybmus.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cybmus.music.data.Song
import com.cybmus.music.ui.components.NerdProgressBar
import com.cybmus.music.ui.theme.LocalCybColors

@Composable
fun PlayerScreen(
    currentSong: Song?,
    isPlaying: Boolean,
    progress: Float,
    positionMs: Long,
    durationMs: Long,
    customCoverUri: android.net.Uri?,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    sleepTimerMinutes: Int?,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onSleepTimerSelected: (Int?) -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalCybColors.current
    val fallbackCover = remember(currentSong?.id) { fallbackAlbumCovers.random() }
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 32.dp, bottom = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Box(
                modifier = Modifier.size(290.dp).clip(RoundedCornerShape(20.dp)).background(colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (currentSong != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(customCoverUri ?: currentSong.albumArtUri ?: fallbackCover).crossfade(true).error(fallbackCover).build(),
                        contentDescription = "Cover", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().alpha(.65f)
                    )
                } else Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.primary.copy(alpha = .5f), modifier = Modifier.size(80.dp))
            }

            Spacer(Modifier.height(16.dp))
            NineDotEqualizer(isPlaying = isPlaying, progress = progress, tint = colors.primary)
            Spacer(Modifier.height(28.dp))
            Text(
                text = currentSong?.title ?: "Nothing playing", color = colors.onBackground, fontSize = 22.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = currentSong?.artist ?: "—", color = colors.onSurface.copy(alpha = .7f), fontSize = 15.sp,
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (currentSong != null) {
                Spacer(Modifier.height(4.dp))
                Text(text = currentSong.fileTypeDisplay, color = colors.accent.copy(alpha = .8f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(Modifier.height(28.dp))
            NerdProgressBar(progress = progress, positionMs = positionMs, durationMs = durationMs, onSeek = onSeek)
            Spacer(Modifier.height(36.dp))
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShuffle, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = if (shuffleEnabled) colors.primary else colors.onSurface.copy(alpha = .65f), modifier = Modifier.size(25.dp))
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = colors.onBackground, modifier = Modifier.size(36.dp))
                }
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(colors.primary, colors.secondary)))
                        .clickable(onClick = onPlayPause), contentAlignment = Alignment.Center
                ) {
                    Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = "Play/Pause", tint = colors.onPrimary, modifier = Modifier.size(40.dp))
                }
                IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = colors.onBackground, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = onRepeat, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Repeat, contentDescription = "Repeat", tint = if (repeatEnabled) colors.primary else colors.onSurface.copy(alpha = .65f), modifier = Modifier.size(25.dp))
                }
            }

            Spacer(Modifier.height(18.dp))
            SleepTimerPicker(selectedMinutes = sleepTimerMinutes, onSelected = onSleepTimerSelected)

        }
    }
}

@Composable
private fun SleepTimerPicker(selectedMinutes: Int?, onSelected: (Int?) -> Unit) {
    val colors = LocalCybColors.current
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(5, 10, 15, 20, 25, 30)

    Box(contentAlignment = Alignment.Center) {
        OutlinedButton(onClick = { expanded = true }) {
            Text(
                text = if (selectedMinutes == null) "Sleep timer" else "Sleep: ${selectedMinutes} min",
                color = colors.onSurface
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { minutes ->
                DropdownMenuItem(
                    text = { Text("${minutes} minutes") },
                    onClick = { onSelected(minutes); expanded = false }
                )
            }
            if (selectedMinutes != null) {
                DropdownMenuItem(
                    text = { Text("Turn off timer") },
                    onClick = { onSelected(null); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun NineDotEqualizer(isPlaying: Boolean, progress: Float, tint: Color) {
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            tick++
            delay(140)
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { column ->
            val wave = if (isPlaying) {
                ((sin(progress * 24f + tick * .75f + column * 1.7f) + 1f) / 2f).toFloat()
            } else .18f
            Column(verticalArrangement = Arrangement.spacedBy(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                repeat(3) { row ->
                    val threshold = (row + 1) / 3f
                    val targetAlpha = if (wave >= threshold) .95f else .16f
                    val alpha by animateFloatAsState(targetAlpha, label = "nine-dot")
                    Box(
                        Modifier.offset(y = ((1f - wave) * 4f).dp).size(8.dp).background(tint.copy(alpha = alpha), CircleShape)
                    )
                }
            }
        }
    }
}
