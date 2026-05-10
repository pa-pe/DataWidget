package name.xoid.datawidget

import android.content.Context
import androidx.core.content.edit

object AppSettings {
    private const val PREFS_NAME = "AppSettings"
    private const val KEY_WIDGET_RADIUS = "widget_radius"
    private const val KEY_WIDGET_PADDING = "widget_padding"

    fun saveWidgetRadius(context: Context, radiusDp: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putInt(KEY_WIDGET_RADIUS, radiusDp)
        }
    }

    fun getWidgetRadius(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_WIDGET_RADIUS, AppConfig.DEFAULT_WIDGET_RADIUS)
    }

    fun saveWidgetPadding(context: Context, paddingDp: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putInt(KEY_WIDGET_PADDING, paddingDp)
        }
    }

    fun getWidgetPadding(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_WIDGET_PADDING, AppConfig.DEFAULT_WIDGET_PADDING)
    }
}
