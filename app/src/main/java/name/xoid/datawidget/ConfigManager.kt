package name.xoid.datawidget

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages the "Master Copy" of all configurations.
 * These templates are displayed in the main app list.
 */
object ConfigManager {
    private const val PREFS_NAME = "Configs"
    private const val KEY_CONFIG_LIST = "config_list"

    fun saveConfigs(context: Context, configs: List<WidgetConfig>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        configs.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("name", it.name)
            obj.put("url", it.url)
            obj.put("bg_color", it.bgColor)
            obj.put("bg_alpha", it.bgAlpha.toDouble())
            obj.put("screen_on_only", it.updateOnlyScreenOn)
            obj.put("progress_visibility", it.progressVisibility)
            obj.put("request_type", it.requestType)
            obj.put("font_size", it.baseFontSize)
            array.put(obj)
        }
        prefs.edit { putString(KEY_CONFIG_LIST, array.toString()) }
    }

    fun getConfigs(context: Context): MutableList<WidgetConfig> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CONFIG_LIST, null)
        
        if (json == null) {
            // First run: Initialize with examples and SAVE them immediately
            val initialList = ExampleProvider.EXAMPLES.map { it.copy() }.toMutableList()
            saveConfigs(context, initialList)
            return initialList
        }

        val list = mutableListOf<WidgetConfig>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(WidgetConfig(
                    obj.getString("name"),
                    obj.getString("url"),
                    obj.optString("bg_color", "#FFFFFF"),
                    obj.optDouble("bg_alpha", AppConfig.DEFAULT_BG_ALPHA.toDouble()).toFloat(),
                    obj.optBoolean("screen_on_only", true),
                    obj.optString("progress_visibility", "always"),
                    obj.optString("request_type", "GET"),
                    obj.optInt("font_size", 12),
                    obj.optString("id", obj.optString("id", System.currentTimeMillis().toString() + i))
                ))
            }
        } catch (_: Exception) {
            // If something is broken, return examples as safety net
            return ExampleProvider.EXAMPLES.map { it.copy() }.toMutableList()
        }
        return list
    }
}
