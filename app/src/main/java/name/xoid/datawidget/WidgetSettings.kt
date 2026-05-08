package name.xoid.datawidget

import android.content.Context
import android.graphics.Color

object WidgetSettings {
    private const val PREFS_NAME = "WidgetSettings"
    private const val KEY_URL_PREFIX = "url_"
    private const val KEY_BG_COLOR_PREFIX = "bg_color_"
    private const val KEY_BG_ALPHA_PREFIX = "bg_alpha_"
    private const val KEY_SCREEN_ON_ONLY_PREFIX = "screen_on_only_"
    private const val KEY_PROGRESS_VISIBILITY_PREFIX = "progress_visibility_"

    fun saveUrl(context: Context, appWidgetId: Int, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_URL_PREFIX + appWidgetId, url).apply()
    }

    fun getUrl(context: Context, appWidgetId: Int): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_URL_PREFIX + appWidgetId, null)
    }

    fun saveBgSettings(context: Context, appWidgetId: Int, color: Int, alpha: Float, screenOnOnly: Boolean, progressVisibility: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_BG_COLOR_PREFIX + appWidgetId, color)
            .putFloat(KEY_BG_ALPHA_PREFIX + appWidgetId, alpha)
            .putBoolean(KEY_SCREEN_ON_ONLY_PREFIX + appWidgetId, screenOnOnly)
            .putString(KEY_PROGRESS_VISIBILITY_PREFIX + appWidgetId, progressVisibility)
            .apply()
    }

    fun getBgColor(context: Context, appWidgetId: Int): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_BG_COLOR_PREFIX + appWidgetId, Color.WHITE)
    }

    fun getBgAlpha(context: Context, appWidgetId: Int): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_BG_ALPHA_PREFIX + appWidgetId, 1.0f)
    }

    fun getScreenOnOnly(context: Context, appWidgetId: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SCREEN_ON_ONLY_PREFIX + appWidgetId, true)
    }

    fun getProgressVisibility(context: Context, appWidgetId: Int): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PROGRESS_VISIBILITY_PREFIX + appWidgetId, "always") ?: "always"
    }

    fun deleteSettings(context: Context, appWidgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_URL_PREFIX + appWidgetId)
            .remove(KEY_BG_COLOR_PREFIX + appWidgetId)
            .remove(KEY_BG_ALPHA_PREFIX + appWidgetId)
            .remove(KEY_SCREEN_ON_ONLY_PREFIX + appWidgetId)
            .remove(KEY_PROGRESS_VISIBILITY_PREFIX + appWidgetId)
            .apply()
    }

    fun deleteUrl(context: Context, appWidgetId: Int) {
        deleteSettings(context, appWidgetId)
    }
}
