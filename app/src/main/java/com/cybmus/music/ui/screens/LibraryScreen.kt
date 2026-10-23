package com.cybmus.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cybmus.music.data.Song
import com.cybmus.music.ui.theme.LocalCybColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    songs: List<Song>,
    isLoading: Boolean,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song, Int) -> Unit,
    onSongLongPress: (Song) -> Unit,
    onPlayPause: () -> Unit,
    onOpenPlayer: () -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalCybColors.current
    var searchQuery by remember { mutableStateOf("") }
    val visibleSongs = remember(songs, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) songs else songs.filter { song ->
            song.title.contains(query, true) || song.artist.contains(query, true) || song.fileType.contains(query, true)
        }
    }
    Column(
        modifier = modifier.fillMaxSize().background(colors.background).padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("LIBRARY", color = colors.primary, fontSize = 13.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                Text("cyb-mus", color = colors.onBackground, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onScan, colors = IconButtonDefaults.iconButtonColors(contentColor = colors.accent)) {
                Icon(Icons.Default.Refresh, contentDescription = "Scan")
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search songs, artists, or file types") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(10.dp))

        if (isLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else if (songs.isEmpty()) {
            EmptyLibrary(onScan, Modifier.weight(1f))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 165.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(visibleSongs, key = { _, song -> song.id }) { index, song ->
                    SongCard(song, song.id == currentSong?.id, onSongLongPress) { onSongClick(song, songs.indexOfFirst { it.id == song.id }) }
                }
            }
        }

        if (currentSong != null) {
            MiniPlayer(currentSong, isPlaying, onPlayPause, onOpenPlayer)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EmptyLibrary(onScan: () -> Unit, modifier: Modifier) {
    val colors = LocalCybColors.current
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.width(200.dp).height(50.dp).clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceVariant.copy(alpha = .55f)).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.primary, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(10.dp))
                Text("CYB-MUS", color = colors.primary.copy(alpha = .9f), fontSize = 18.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text("No songs found", color = colors.onSurface.copy(alpha = .7f))
            Text("Grant permission & scan", color = colors.onSurface.copy(alpha = .5f), fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onScan, colors = ButtonDefaults.buttonColors(containerColor = colors.primary)) {
                Text("Scan Music", color = colors.onPrimary)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SongCard(song: Song, isCurrent: Boolean, onLongPress: (Song) -> Unit, onClick: () -> Unit) {
    val colors = LocalCybColors.current
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = { onLongPress(song) }),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) colors.primary.copy(alpha = .16f) else colors.surface
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                song.title,
                color = if (isCurrent) colors.primary else colors.onBackground,
                fontSize = 15.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(song.artist, color = colors.onSurface.copy(alpha = .7f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SongMeta("TIME", song.durationFormatted)
                SongMeta("SIZE", song.sizeFormatted)
                SongMeta("TYPE", song.fileTypeDisplay)
            }
        }
    }
}

@Composable
private fun SongMeta(label: String, value: String) {
    val colors = LocalCybColors.current
    Column {
        Text(label, color = colors.primary.copy(alpha = .75f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(2.dp))
        Text(value, color = colors.onSurface.copy(alpha = .8f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun MiniPlayer(song: Song, isPlaying: Boolean, onPlayPause: () -> Unit, onOpenPlayer: () -> Unit) {
    val colors = LocalCybColors.current
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surfaceVariant.copy(alpha = .8f))
            .clickable(onClick = onOpenPlayer).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = colors.onBackground, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, color = colors.onSurface.copy(alpha = .65f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onPlayPause, modifier = Modifier.size(38.dp)) {
            Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = if (isPlaying) "Pause" else "Play", tint = colors.primary)
        }
    }
}
