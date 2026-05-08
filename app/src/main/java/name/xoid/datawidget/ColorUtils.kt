package name.xoid.datawidget

import android.graphics.Color
import androidx.core.graphics.toColorInt

object ColorUtils {
    private val webColors = mapOf(
        // The standard Android "green" is actually HTML "lime" (#00FF00).
        // Here we map "green" to the darker HTML standard.
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
        val normalized = colorStr.lowercase().trim()
        
        // Check our custom map first
        val hex = webColors[normalized]
        if (hex != null) return hex.toColorInt()

        return try {
            // If it's a hex string (starts with #), use toColorInt
            if (normalized.startsWith("#")) {
                normalized.toColorInt()
            } else {
                // Fallback to system parser for standard names like "red", "blue"
                normalized.toColorInt()
            }
        } catch (e: Exception) {
            Color.BLACK
        }
    }
}
