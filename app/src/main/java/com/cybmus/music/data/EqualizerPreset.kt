package com.cybmus.music.data

/**
 * 6 built-in equalizer presets for a relaxing / cyber feel.
 * Levels are relative band gains in millibels (mB). Android Equalizer uses short arrays.
 */
data class EqualizerPreset(
    val id: Int,
    val name: String,
    val description: String,
    val bandLevels: ShortArray // relative to center, typically -1500..1500
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EqualizerPreset
        return id == other.id
    }

    override fun hashCode(): Int = id
}

object EqualizerPresets {
    // Typical 5-band layout approximation (bass → treble)
    // Values chosen for pleasant, non-harsh listening
    val ALL = listOf(
        EqualizerPreset(
            id = 0,
            name = "Flat",
            description = "Neutral response",
            bandLevels = shortArrayOf(0, 0, 0, 0, 0)
        ),
        EqualizerPreset(
            id = 1,
            name = "Relax",
            description = "Soft bass, gentle highs – perfect for unwinding",
            bandLevels = shortArrayOf(300, 150, 0, -100, -200)
        ),
        EqualizerPreset(
            id = 2,
            name = "Focus",
            description = "Clear mids, reduced distraction",
            bandLevels = shortArrayOf(-100, 100, 250, 100, -50)
        ),
        EqualizerPreset(
            id = 3,
            name = "Night",
            description = "Warm low end, rolled-off treble for late hours",
            bandLevels = shortArrayOf(400, 200, 50, -150, -300)
        ),
        EqualizerPreset(
            id = 4,
            name = "Bass Boost",
            description = "Deep, satisfying low frequencies",
            bandLevels = shortArrayOf(600, 300, 0, -100, -150)
        ),
        EqualizerPreset(
            id = 5,
            name = "Treble",
            description = "Airy highs and sparkle",
            bandLevels = shortArrayOf(-150, -50, 100, 350, 500)
        )
    )
}
