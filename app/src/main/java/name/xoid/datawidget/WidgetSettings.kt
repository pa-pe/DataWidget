package name.xoid.datawidget

import android.content.Context

object WidgetSettings {
    private const val PREFS_NAME = "WidgetSettings"
    private const val KEY_URL_PREFIX = "url_"

    fun saveUrl(context: Context, appWidgetId: Int, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_URL_PREFIX + appWidgetId, url).apply()
    }

    fun getUrl(context: Context, appWidgetId: Int): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_URL_PREFIX + appWidgetId, "https://api.example.com/data/$appWidgetId")
    }

    fun deleteUrl(context: Context, appWidgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_URL_PREFIX + appWidgetId).apply()
    }
}
