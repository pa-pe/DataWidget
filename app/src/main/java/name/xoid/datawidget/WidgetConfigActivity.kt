package name.xoid.datawidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import name.xoid.datawidget.databinding.LayoutConfigEditBinding

class WidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var binding: LayoutConfigEditBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setResult(RESULT_CANCELED)

        binding = LayoutConfigEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Load existing settings if any
        val currentUrl = WidgetSettings.getUrl(this, appWidgetId) ?: AppConfig.DEFAULT_JSON_URL
        val currentColor = WidgetSettings.getBgColor(this, appWidgetId)
        val currentAlpha = WidgetSettings.getBgAlpha(this, appWidgetId)
        val currentScreenOn = WidgetSettings.getScreenOnOnly(this, appWidgetId)
        val currentProgVis = WidgetSettings.getProgressVisibility(this, appWidgetId)

        val tempConfig = WidgetConfig("temp", currentUrl, String.format("#%06X", (0xFFFFFF and currentColor)), currentAlpha, currentScreenOn, currentProgVis)
        
        val helper = ConfigUiHelper(this, layoutInflater, binding)
        helper.setup(tempConfig)

        binding.btnSave.setOnClickListener {
            val url = binding.editUrl.text.toString()
            val colorStr = String.format("#%06X", (0xFFFFFF and helper.selectedColor))
            val alpha = helper.selectedAlpha
            val screenOnOnly = binding.checkScreenOn.isChecked
            val progVis = if (binding.radioOnTap.isChecked) "on_tap" else "always"

            if (url.isEmpty()) {
                Toast.makeText(this, "URL cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                WidgetSettings.saveUrl(this, appWidgetId, url)
                WidgetSettings.saveBgSettings(this, appWidgetId, helper.selectedColor, alpha, screenOnOnly, progVis)

                // Also add/update in the global config library
                val configs = ConfigManager.getConfigs(this)
                val existing = configs.find { it.url == url }
                if (existing != null) {
                    existing.bgColor = colorStr
                    existing.bgAlpha = alpha
                    existing.updateOnlyScreenOn = screenOnOnly
                    existing.progressVisibility = progVis
                } else {
                    configs.add(WidgetConfig("Widget $appWidgetId", url, colorStr, alpha, screenOnOnly, progVis))
                }
                ConfigManager.saveConfigs(this, configs)

                // Update the widget immediately
                val appWidgetManager = AppWidgetManager.getInstance(this)
                val serviceIntent = Intent(this, UpdateService::class.java).apply {
                    action = UpdateService.ACTION_UPDATE_WIDGETS
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
                }
                startForegroundService(serviceIntent)

                val resultValue = Intent()
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(RESULT_OK, resultValue)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this, "Error saving settings", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
