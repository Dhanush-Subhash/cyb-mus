package com.cybmus.music.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cybmus.music.ui.theme.LocalCybColors

@Composable
fun SettingsScreen(
    wallpaperUri: Uri?,
    onWallpaperSelected: (Uri?) -> Unit,
    musicFolderUris: List<Uri>,
    onMusicFolderSelected: (Uri) -> Unit,
    onMusicFolderRemoved: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalCybColors.current
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        onWallpaperSelected(uri)
    }
    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? -> uri?.let(onMusicFolderSelected) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(20.dp)
    ) {
        Text(
            text = "SETTINGS",
            color = colors.primary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        Text(
            text = "Atmosphere",
            color = colors.onBackground,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(28.dp))

        // Wallpaper section
        Text(
            text = "WALLPAPER & THEME",
            color = colors.onSurface.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (wallpaperUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(wallpaperUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Wallpaper preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.Wallpaper,
                            contentDescription = null,
                            tint = colors.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    CherryBlossomBranch(accent = colors.primary)
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "The player picks colors from your wallpaper and applies them across the whole app – progress glow, accents, surfaces… everything.",
                    color = colors.onSurface.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { launcher.launch(arrayOf("image/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Choose", color = colors.onPrimary)
                    }
                    if (wallpaperUri != null) {
                        OutlinedButton(
                            onClick = { onWallpaperSelected(null) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurface),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "MUSIC FOLDERS",
            color = colors.onSurface.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Choose where cyb-mus should look for downloaded audio. Only these folders and their subfolders are included in scans.",
                    color = colors.onSurface.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(12.dp))
                musicFolderUris.forEach { uri ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = colors.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = uri.toString().substringAfterLast('/').ifBlank { "Selected folder" },
                            color = colors.onSurface,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onMusicFolderRemoved(uri) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove folder", tint = colors.onSurface.copy(alpha = 0.7f))
                        }
                    }
                }
                Button(
                    onClick = { folderLauncher.launch(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add music folder", color = colors.onPrimary)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "ABOUT",
            color = colors.onSurface.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "cyb-mus  ·  v1.0.0\nA calm cyber music player.\nBuilt for focus & late-night sessions.",
            color = colors.onSurface.copy(alpha = 0.7f),
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun CherryBlossomBranch(accent: androidx.compose.ui.graphics.Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val branch = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * .08f, size.height * .94f)
            cubicTo(size.width * .34f, size.height * .72f, size.width * .48f, size.height * .42f, size.width * .88f, size.height * .18f)
        }
        drawPath(branch, accent.copy(alpha = .7f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f))
        val twigs = listOf(
            .30f to .70f, .43f to .52f, .57f to .40f, .70f to .28f, .80f to .22f
        )
        twigs.forEach { (x, y) ->
            drawLine(accent.copy(alpha = .6f), Offset(size.width * x, size.height * y), Offset(size.width * (x + .06f), size.height * (y - .16f)), strokeWidth = 3f)
            drawCircle(accent.copy(alpha = .78f), radius = 7f, center = Offset(size.width * (x + .06f), size.height * (y - .16f)))
            drawCircle(Color.White.copy(alpha = .65f), radius = 2f, center = Offset(size.width * (x + .06f), size.height * (y - .16f)))
        }
    }
}
