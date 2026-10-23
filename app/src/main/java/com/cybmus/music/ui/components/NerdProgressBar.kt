package com.cybmus.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cybmus.music.ui.theme.LocalCybColors

/**
 * Cool nerd-style progress bar that shows a live #hex progress value.
 */
@Composable
fun NerdProgressBar(
    progress: Float, // 0f..1f
    positionMs: Long,
    durationMs: Long,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalCybColors.current
    val clamped = progress.coerceIn(0f, 1f)

    // Convert progress to a cool hex-like display
    val hexProgress = remember(clamped) {
        val v = (clamped * 255).toInt().coerceIn(0, 255)
        "#%02X".format(v)
    }

    val timeText = remember(positionMs, durationMs) {
        fun fmt(ms: Long): String {
            val s = (ms / 1000).toInt()
            return "%d:%02d".format(s / 60, s % 60)
        }
        "${fmt(positionMs)}  /  ${fmt(durationMs)}"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek(fraction)
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek(fraction)
                    }
                }
        ) {
            val barHeight = 8.dp.toPx()
            val y = (size.height - barHeight) / 2
            val corner = CornerRadius(4.dp.toPx())

            // Track
            drawRoundRect(
                color = colors.surfaceVariant.copy(alpha = 0.6f),
                topLeft = Offset(0f, y),
                size = Size(size.width, barHeight),
                cornerRadius = corner
            )

            // Glow fill
            val fillWidth = size.width * clamped
            if (fillWidth > 0) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            colors.primary.copy(alpha = 0.7f),
                            colors.accent,
                            colors.progressGlow
                        )
                    ),
                    topLeft = Offset(0f, y),
                    size = Size(fillWidth, barHeight),
                    cornerRadius = corner
                )
                // Soft outer glow
                drawRoundRect(
                    color = colors.progressGlow.copy(alpha = 0.25f),
                    topLeft = Offset(0f, y - 3.dp.toPx()),
                    size = Size(fillWidth, barHeight + 6.dp.toPx()),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                    style = Stroke(width = 4.dp.toPx())
                )
            }

            // Thumb
            val thumbX = fillWidth
            drawCircle(
                color = colors.accent,
                radius = 7.dp.toPx(),
                center = Offset(thumbX, size.height / 2)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 3.dp.toPx(),
                center = Offset(thumbX, size.height / 2)
            )
        }

        // Hex progress + time (nerd vibe)
        Text(
            text = "$hexProgress  ·  $timeText",
            color = colors.onSurface.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(top = 32.dp)
        )
    }
}
