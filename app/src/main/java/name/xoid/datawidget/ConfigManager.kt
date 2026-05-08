package name.xoid.datawidget

import android.content.Context
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import org.json.JSONArray
import org.json.JSONObject

object ConfigManager {
    private const val PREFS_NAME = "Configs"
    private const val KEY_CONFIG_LIST = "config_list"

    fun syncWithActiveWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, DataWidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)
        
        val currentConfigs = getConfigs(context)
        var changed = false

        for (id in ids) {
            val url = WidgetSettings.getUrl(context, id)
            if (url != null && currentConfigs.none { it.url == url }) {
                currentConfigs.add(WidgetConfig("Widget $id", url))
                changed = true
            }
        }

        if (changed) {
            saveConfigs(context, currentConfigs)
        }
    }

    fun saveConfigs(context: Context, configs: List<WidgetConfig>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        configs.forEach {
            val obj = JSONObject()
            obj.put("name", it.name)
            obj.put("url", it.url)
            array.put(obj)
        }
        prefs.edit().putString(KEY_CONFIG_LIST, array.toString()).apply()
    }

    fun getConfigs(context: Context): MutableList<WidgetConfig> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CONFIG_LIST, null)
        
        if (json == null) {
            // Default list if nothing saved
            return mutableListOf(
                WidgetConfig("Countdown 2030 (GitHub)", AppConfig.DEFAULT_JSON_URL)
            )
        }

        val list = mutableListOf<WidgetConfig>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(WidgetConfig(obj.getString("name"), obj.getString("url")))
            }
        } catch (e: Exception) {
            list.add(WidgetConfig("Countdown 2030 (GitHub)", AppConfig.DEFAULT_JSON_URL))
        }
        return list
    }
}
