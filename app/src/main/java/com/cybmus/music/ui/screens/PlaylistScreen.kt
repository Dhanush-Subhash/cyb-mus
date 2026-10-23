package com.cybmus.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cybmus.music.data.Song
import com.cybmus.music.ui.theme.LocalCybColors

@Composable
fun PlaylistScreen(
    songs: List<Song>,
    playlists: Map<String, List<Long>>,
    editingSong: Song?,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onToggleSong: (String, Long) -> Unit,
    onMoveSong: (String, Int, Int) -> Unit,
    onPlayPlaylist: (String) -> Unit,
    onCloseEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalCybColors.current
    var selectedPlaylist by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    LaunchedEffect(playlists.keys) {
        if (selectedPlaylist !in playlists.keys) selectedPlaylist = playlists.keys.firstOrNull()
    }

    Column(modifier.fillMaxSize().background(colors.background).padding(20.dp)) {
        Text("PLAYLISTS", color = colors.primary, fontSize = 13.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Your collections", color = colors.onBackground, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showCreateDialog = true }) { Icon(Icons.Default.Add, contentDescription = "New playlist", tint = colors.primary) }
        }
        Text("Long-press a library song to add or remove it from a playlist.", color = colors.onSurface.copy(alpha = .65f), fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))

        if (playlists.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.QueueMusic, contentDescription = null, tint = colors.primary.copy(alpha = .7f), modifier = Modifier.size(54.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("No playlists yet", color = colors.onSurface.copy(alpha = .7f))
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { showCreateDialog = true }) { Text("Create playlist") }
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(playlists.keys.toList()) { name ->
                    val selected = name == selectedPlaylist
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (selected) colors.primary.copy(alpha = .15f) else colors.surface)
                            .clickable { selectedPlaylist = name }.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.QueueMusic, contentDescription = null, tint = colors.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, color = colors.onBackground, fontWeight = FontWeight.SemiBold)
                            Text("${playlists[name].orEmpty().size} songs", color = colors.onSurface.copy(alpha = .6f), fontSize = 12.sp)
                        }
                        IconButton(onClick = { onDeletePlaylist(name) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete playlist", tint = colors.onSurface.copy(alpha = .65f))
                        }
                    }
                }
                selectedPlaylist?.let { name ->
                    val playlistSongs = playlists[name].orEmpty().mapNotNull { id -> songs.find { it.id == id } }
                    item {
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("${name.uppercase()} TRACKS", color = colors.primary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onPlayPlaylist(name) }, enabled = playlistSongs.isNotEmpty()) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play playlist", tint = colors.primary)
                            }
                        }
                    }
                    itemsIndexed(playlistSongs, key = { _, song -> song.id }) { index, song ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(song.title, color = colors.onBackground, maxLines = 1)
                                Text(song.artist, color = colors.onSurface.copy(alpha = .6f), fontSize = 12.sp, maxLines = 1)
                            }
                            IconButton(onClick = { if (index > 0) onMoveSong(name, index, index - 1) }, enabled = index > 0) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up", tint = colors.primary)
                            }
                            IconButton(onClick = { if (index < playlistSongs.lastIndex) onMoveSong(name, index, index + 1) }, enabled = index < playlistSongs.lastIndex) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down", tint = colors.primary)
                            }
                            IconButton(onClick = { onToggleSong(name, song.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove song", tint = colors.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New playlist") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true, label = { Text("Playlist name") }) },
            confirmButton = {
                TextButton(onClick = { onCreatePlaylist(newName); newName = ""; showCreateDialog = false }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }

    editingSong?.let { song ->
        AlertDialog(
            onDismissRequest = onCloseEditor,
            title = { Text("Edit playlists") },
            text = {
                Column {
                    Text(song.title, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    if (playlists.isEmpty()) Text("Create a playlist first from the + button.")
                    playlists.forEach { (name, ids) ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = song.id in ids, onCheckedChange = { onToggleSong(name, song.id) })
                            Text(name, modifier = Modifier.clickable { onToggleSong(name, song.id) })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onCloseEditor) { Text("Done") } }
        )
    }
}
