package name.xoid.datawidget

import android.content.Context
import androidx.core.content.edit

object AppSettings {
    private const val PREFS_NAME = "AppSettings"
    private const val KEY_WIDGET_RADIUS = "widget_radius"

    fun saveWidgetRadius(context: Context, radiusDp: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putInt(KEY_WIDGET_RADIUS, radiusDp)
        }
    }

    fun getWidgetRadius(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_WIDGET_RADIUS, 16) // Default 16dp
    }
}
