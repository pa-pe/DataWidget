package name.xoid.datawidget

import android.graphics.Color
import androidx.core.graphics.toColorInt

object ColorUtils {
    private val webColors = mapOf(
        "green" to "#008000",
        "lime" to "#00FF00",
        "yellowgreen" to "#9ACD32",
        "orange" to "#FFA500",
        "pink" to "#FFC0CB",
        "gold" to "#FFD700",
        "brown" to "#A52A2A",
        "skyblue" to "#87CEEB",
        "violet" to "#EE82EE",
        "indigo" to "#4B0082",
        "chocolate" to "#D2691E",
        "crimson" to "#DC143C",
        "gray" to "#808080",
        "silver" to "#C0C0C0",
        "teal" to "#008080",
        "navy" to "#000080"
    )

    fun parseColor(colorStr: String?): Int {
        if (colorStr.isNullOrEmpty()) return Color.BLACK
        var normalized = colorStr.lowercase().trim()
        
        // Handle short HEX codes like #444 -> #444444
        if (normalized.startsWith("#") && normalized.length == 4) {
            val r = normalized[1]
            val g = normalized[2]
            val b = normalized[3]
            normalized = "#$r$r$g$g$b$b"
        }

        // Check our custom map first
        val hex = webColors[normalized]
        if (hex != null) return hex.toColorInt()

        return try {
            if (normalized.startsWith("#")) {
                normalized.toColorInt()
            } else {
                // Fallback for standard system colors
                Color.parseColor(normalized)
            }
        } catch (e: Exception) {
            Color.BLACK
        }
    }
}
