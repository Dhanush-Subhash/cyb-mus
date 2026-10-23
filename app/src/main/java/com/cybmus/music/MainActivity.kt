package com.cybmus.music

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cybmus.music.ui.MusicViewModel
import com.cybmus.music.ui.screens.EqualizerScreen
import com.cybmus.music.ui.screens.LibraryScreen
import com.cybmus.music.ui.screens.PlayerScreen
import com.cybmus.music.ui.screens.PlaylistScreen
import com.cybmus.music.ui.screens.SettingsScreen
import com.cybmus.music.ui.theme.LocalCybColors
import com.cybmus.music.ui.theme.cybColorScheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Library : Screen("library", "Library", Icons.Default.LibraryMusic)
    data object Player : Screen("player", "Player", Icons.Default.PlayCircle)
    data object Playlists : Screen("playlists", "Lists", Icons.Default.QueueMusic)
    data object Equalizer : Screen("equalizer", "EQ", Icons.Default.Equalizer)
    data object Settings : Screen("settings", "Theme", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MusicViewModel = viewModel()
            val cybColors = viewModel.cybColors
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            val permissionState = rememberPermissionState(permission)

            LaunchedEffect(permissionState.status.isGranted) {
                if (permissionState.status.isGranted) {
                    viewModel.loadLibrary()
                }
            }

            // Keep the visual system consistent while making dense layouts fit older phones.
            // Large modern displays stay at the designed size; compact displays scale down
            // dimensions and text together so controls do not collide or get clipped.
            val configuration = LocalConfiguration.current
            val baseDensity = LocalDensity.current
            val uiScale = minOf(
                configuration.screenWidthDp / 390f,
                configuration.screenHeightDp / 760f
            ).coerceIn(0.78f, 1f)
            val responsiveDensity = Density(
                density = baseDensity.density * uiScale,
                fontScale = baseDensity.fontScale * uiScale
            )

            CompositionLocalProvider(
                LocalDensity provides responsiveDensity,
                LocalCybColors provides cybColors
            ) {
                MaterialTheme(colorScheme = cybColorScheme(cybColors)) {
                    Scaffold(
                        containerColor = cybColors.background,
                        bottomBar = {
                            NavigationBar(
                                containerColor = cybColors.surface.copy(alpha = 0.95f),
                                contentColor = cybColors.onSurface
                            ) {
                                listOf(
                                    Screen.Library,
                                    Screen.Player,
                                    Screen.Playlists,
                                    Screen.Equalizer,
                                    Screen.Settings
                                ).forEach { screen ->
                                    NavigationBarItem(
                                        selected = currentRoute == screen.route,
                                        onClick = {
                                            if (screen == Screen.Library) {
                                                navController.navigate(Screen.Library.route) {
                                                    popUpTo(Screen.Library.route) { inclusive = false }
                                                    launchSingleTop = true
                                                    restoreState = false
                                                }
                                            } else {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = {
                                            Icon(screen.icon, contentDescription = screen.label)
                                        },
                                        label = { Text(screen.label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = cybColors.primary,
                                            selectedTextColor = cybColors.primary,
                                            indicatorColor = cybColors.primary.copy(alpha = 0.15f),
                                            unselectedIconColor = cybColors.onSurface.copy(alpha = 0.6f),
                                            unselectedTextColor = cybColors.onSurface.copy(alpha = 0.6f)
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Library.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Library.route) {
                                if (!permissionState.status.isGranted) {
                                    PermissionRationale(
                                        shouldShowRationale = permissionState.status.shouldShowRationale,
                                        onRequest = { permissionState.launchPermissionRequest() }
                                    )
                                } else {
                                    LibraryScreen(
                                        songs = viewModel.songs,
                                        isLoading = viewModel.isLoading,
                                        currentSong = viewModel.currentSong,
                                        isPlaying = viewModel.isPlaying,
                                        onSongClick = { song, index ->
                                            viewModel.playSong(song, index)
                                            navController.navigate(Screen.Player.route)
                                        },
                                        onSongLongPress = { song ->
                                            viewModel.openPlaylistEditor(song)
                                            navController.navigate(Screen.Playlists.route)
                                        },
                                        onPlayPause = { viewModel.togglePlayPause() },
                                        onOpenPlayer = { navController.navigate(Screen.Player.route) },
                                        onScan = { viewModel.loadLibrary() }
                                    )
                                }
                            }
                            composable(Screen.Player.route) {
                                PlayerScreen(
                                    currentSong = viewModel.currentSong,
                                    isPlaying = viewModel.isPlaying,
                                    progress = viewModel.progress,
                                    positionMs = viewModel.positionMs,
                                    durationMs = viewModel.durationMs,
                                    wallpaperUri = viewModel.wallpaperUri,
                                    shuffleEnabled = viewModel.shuffleEnabled,
                                    repeatEnabled = viewModel.repeatEnabled,
                                    sleepTimerMinutes = viewModel.sleepTimerMinutes,
                                    onPlayPause = { viewModel.togglePlayPause() },
                                    onNext = { viewModel.playNext() },
                                    onPrevious = { viewModel.playPrevious() },
                                    onShuffle = { viewModel.toggleShuffle() },
                                    onRepeat = { viewModel.toggleRepeat() },
                                    onSleepTimerSelected = { viewModel.setSleepTimer(it) },
                                    onSeek = { viewModel.seekTo(it) }
                                )
                            }
                            composable(Screen.Playlists.route) {
                                PlaylistScreen(
                                    songs = viewModel.songs,
                                    playlists = viewModel.playlists,
                                    editingSong = viewModel.editingSong,
                                    onCreatePlaylist = { viewModel.createPlaylist(it) },
                                    onDeletePlaylist = { viewModel.deletePlaylist(it) },
                                    onToggleSong = { name, songId -> viewModel.toggleSongInPlaylist(name, songId) },
                                    onMoveSong = { name, from, to -> viewModel.movePlaylistSong(name, from, to) },
                                    onPlayPlaylist = { viewModel.playPlaylist(it); navController.navigate(Screen.Player.route) },
                                    onCloseEditor = { viewModel.closePlaylistEditor() }
                                )
                            }
                            composable(Screen.Equalizer.route) {
                                EqualizerScreen(
                                    selectedPreset = viewModel.selectedPreset,
                                    equalizerEnabled = viewModel.equalizerEnabled,
                                    customBandLevels = viewModel.customBandLevels,
                                    customPresets = viewModel.customPresets,
                                    onPresetSelected = { viewModel.applyPreset(it) },
                                    onBandChanged = { index, level -> viewModel.setCustomBandLevel(index, level) },
                                    onEnabledChange = { viewModel.setEqualizerEnabled(it) },
                                    onSaveCustomPreset = { viewModel.saveCustomPreset(it) },
                                    onRemoveCustomPreset = { viewModel.removeCustomPreset(it) }
                                )
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    wallpaperUri = viewModel.wallpaperUri,
                                    onWallpaperSelected = { viewModel.setWallpaper(it) },
                                    musicFolderUris = viewModel.musicFolderUris,
                                    onMusicFolderSelected = { viewModel.addMusicFolder(it) },
                                    onMusicFolderRemoved = { viewModel.removeMusicFolder(it) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRationale(
    shouldShowRationale: Boolean,
    onRequest: () -> Unit
) {
    val colors = LocalCybColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Storage access needed",
                color = colors.onBackground,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.padding(8.dp))
            Text(
                text = if (shouldShowRationale)
                    "cyb-mus needs permission to read your music files so it can build the library."
                else
                    "Please grant audio/media permission to continue.",
                color = colors.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.padding(16.dp))
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text("Grant Permission", color = colors.onPrimary)
            }
        }
    }
}
