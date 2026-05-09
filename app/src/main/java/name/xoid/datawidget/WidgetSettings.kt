package name.xoid.datawidget

import android.content.Context
import android.graphics.Color
import androidx.core.content.edit

/**
 * ARCHITECTURE NOTE:
 * This application uses a dual-storage system:
 * 
 * 1. LIBRARY (ConfigManager): Stores "Master Templates". These are the cards you see in the main app.
 *    Any change here propagates to all widgets linked to this template ID.
 * 
 * 2. INSTANCES (WidgetSettings): Stores data for a specific widget "square" on the home screen.
 *    - Each widget has a unique 'appWidgetId' assigned by Android.
 *    - Each widget stores a 'config_id' which points to a Master Template in the Library.
 *    - If a widget is deleted from the screen, only its INSTANCE data is wiped.
 *    - If a widget is customized manually (URL change), the link to the Library is broken.
 */
object WidgetSettings {
    private const val PREFS_NAME = "WidgetSettings"
    private const val KEY_URL_PREFIX = "url_"
    private const val KEY_CONFIG_ID_PREFIX = "config_id_"
    private const val KEY_NAME_PREFIX = "name_"
    private const val KEY_BG_COLOR_PREFIX = "bg_color_"
    private const val KEY_BG_ALPHA_PREFIX = "bg_alpha_"
    private const val KEY_SCREEN_ON_ONLY_PREFIX = "screen_on_only_"
    private const val KEY_PROGRESS_VISIBILITY_PREFIX = "progress_visibility_"
    private const val KEY_REQUEST_TYPE_PREFIX = "request_type_"
    private const val KEY_FONT_SIZE_PREFIX = "font_size_"

    fun saveUrl(context: Context, appWidgetId: Int, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_URL_PREFIX + appWidgetId, url) }
    }

    fun getUrl(context: Context, appWidgetId: Int): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_URL_PREFIX + appWidgetId, null)
    }

    fun saveName(context: Context, appWidgetId: Int, name: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_NAME_PREFIX + appWidgetId, name) }
    }

    fun getName(context: Context, appWidgetId: Int): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NAME_PREFIX + appWidgetId, null)
    }

    fun saveConfigId(context: Context, appWidgetId: Int, configId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_CONFIG_ID_PREFIX + appWidgetId, configId) }
    }

    fun getConfigId(context: Context, appWidgetId: Int): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CONFIG_ID_PREFIX + appWidgetId, null)
    }

    fun saveSettings(
        context: Context, 
        appWidgetId: Int, 
        color: Int, 
        alpha: Float, 
        screenOnOnly: Boolean, 
        progressVisibility: String,
        requestType: String,
        fontSize: Int
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putInt(KEY_BG_COLOR_PREFIX + appWidgetId, color)
            putFloat(KEY_BG_ALPHA_PREFIX + appWidgetId, alpha)
            putBoolean(KEY_SCREEN_ON_ONLY_PREFIX + appWidgetId, screenOnOnly)
            putString(KEY_PROGRESS_VISIBILITY_PREFIX + appWidgetId, progressVisibility)
            putString(KEY_REQUEST_TYPE_PREFIX + appWidgetId, requestType)
            putInt(KEY_FONT_SIZE_PREFIX + appWidgetId, fontSize)
        }
    }

    fun getBgColor(context: Context, appWidgetId: Int): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_BG_COLOR_PREFIX + appWidgetId, Color.WHITE)
    }

    fun getBgAlpha(context: Context, appWidgetId: Int): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_BG_ALPHA_PREFIX + appWidgetId, AppConfig.DEFAULT_BG_ALPHA)
    }

    fun getScreenOnOnly(context: Context, appWidgetId: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SCREEN_ON_ONLY_PREFIX + appWidgetId, true)
    }

    fun getProgressVisibility(context: Context, appWidgetId: Int): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PROGRESS_VISIBILITY_PREFIX + appWidgetId, "always") ?: "always"
    }

    fun getRequestType(context: Context, appWidgetId: Int): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_REQUEST_TYPE_PREFIX + appWidgetId, "GET") ?: "GET"
    }

    fun getFontSize(context: Context, appWidgetId: Int): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_FONT_SIZE_PREFIX + appWidgetId, 12)
    }

    fun deleteSettings(context: Context, appWidgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            remove(KEY_URL_PREFIX + appWidgetId)
            remove(KEY_NAME_PREFIX + appWidgetId)
            remove(KEY_CONFIG_ID_PREFIX + appWidgetId)
            remove(KEY_BG_COLOR_PREFIX + appWidgetId)
            remove(KEY_BG_ALPHA_PREFIX + appWidgetId)
            remove(KEY_SCREEN_ON_ONLY_PREFIX + appWidgetId)
            remove(KEY_PROGRESS_VISIBILITY_PREFIX + appWidgetId)
            remove(KEY_REQUEST_TYPE_PREFIX + appWidgetId)
            remove(KEY_FONT_SIZE_PREFIX + appWidgetId)
        }
    }
}
