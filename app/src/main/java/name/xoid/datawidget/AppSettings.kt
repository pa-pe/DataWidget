package name.xoid.datawidget

import android.content.Context
import androidx.core.content.edit

object AppSettings {
    private const val PREFS_NAME = "AppSettings"
    private const val KEY_WIDGET_RADIUS = "widget_radius"
    private const val KEY_WIDGET_PADDING = "widget_padding"
    private const val KEY_PANEL_BG_COLOR = "panel_bg_color"
    private const val KEY_PANEL_BG_ALPHA = "panel_bg_alpha"

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

    fun savePanelBgColor(context: Context, colorHex: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_PANEL_BG_COLOR, colorHex)
        }
    }

    fun getPanelBgColor(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PANEL_BG_COLOR, AppConfig.DEFAULT_PANEL_BG_COLOR) ?: AppConfig.DEFAULT_PANEL_BG_COLOR
    }

    fun savePanelBgAlpha(context: Context, alpha: Float) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putFloat(KEY_PANEL_BG_ALPHA, alpha)
        }
    }

    fun getPanelBgAlpha(context: Context): Float {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_PANEL_BG_ALPHA, AppConfig.DEFAULT_PANEL_BG_ALPHA)
    }
}
