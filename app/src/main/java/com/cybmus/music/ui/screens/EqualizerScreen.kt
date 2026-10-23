package com.cybmus.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cybmus.music.data.EqualizerPreset
import com.cybmus.music.data.EqualizerPresets
import com.cybmus.music.ui.theme.LocalCybColors
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun EqualizerScreen(
    selectedPreset: EqualizerPreset,
    equalizerEnabled: Boolean,
    customBandLevels: List<Int>,
    customPresets: List<EqualizerPreset>,
    onPresetSelected: (EqualizerPreset) -> Unit,
    onBandChanged: (Int, Int) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onRemoveCustomPreset: (EqualizerPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalCybColors.current
    val labels = listOf("60 Hz", "230 Hz", "910 Hz", "4 kHz", "14 kHz")
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }
    val allPresets = EqualizerPresets.ALL + customPresets
    val isEdited = customBandLevels != selectedPreset.bandLevels.map { it.toInt() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("EQUALIZER", color = colors.primary, fontSize = 13.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        Text("Sound shape", color = colors.onBackground, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.surface).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Equalizer, contentDescription = null, tint = colors.primary)
                Spacer(Modifier.width(12.dp))
                Text("EQ Active", color = colors.onBackground, fontWeight = FontWeight.Medium)
            }
            Switch(checked = equalizerEnabled, onCheckedChange = onEnabledChange)
        }

        Spacer(Modifier.height(20.dp))
        Text("CUSTOM EQ", color = colors.onSurface.copy(alpha = .6f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(Modifier.padding(16.dp)) {
                customBandLevels.take(5).forEachIndexed { index, level ->
                    Text(labels[index], color = colors.onSurface.copy(alpha = .7f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = level.toFloat(),
                            onValueChange = { onBandChanged(index, it.toInt()) },
                            valueRange = -1500f..1500f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${level / 100} dB", color = colors.onSurface.copy(alpha = .7f), fontSize = 11.sp, modifier = Modifier.width(48.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("PRESETS", color = colors.onSurface.copy(alpha = .6f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(8.dp))
        allPresets.chunked(3).forEach { rowPresets ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowPresets.forEach { preset ->
                    Box(Modifier.weight(1f)) {
                        PresetButton(preset, preset.id == selectedPreset.id, onPresetSelected, Modifier.fillMaxWidth())
                        if (preset.id < 0) {
                            IconButton(
                                onClick = { onRemoveCustomPreset(preset) },
                                modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${preset.name}", tint = colors.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                repeat(3 - rowPresets.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (isEdited) {
            Button(
                onClick = { presetName = ""; showSaveDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) { Text("Save current EQ as preset", color = colors.onPrimary) }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save EQ preset") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            onSaveCustomPreset(presetName)
                            showSaveDialog = false
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PresetButton(
    preset: EqualizerPreset,
    selected: Boolean,
    onClick: (EqualizerPreset) -> Unit,
    modifier: Modifier
) {
    val colors = LocalCybColors.current
    TextButton(
        onClick = { onClick(preset) },
        modifier = modifier.border(1.dp, if (selected) colors.primary else colors.surfaceVariant, RoundedCornerShape(10.dp)),
        colors = ButtonDefaults.textButtonColors(contentColor = if (selected) colors.primary else colors.onSurface)
    ) { Text(preset.name, maxLines = 1, fontSize = 11.sp) }
}
