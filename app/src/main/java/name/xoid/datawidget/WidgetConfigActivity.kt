package name.xoid.datawidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import name.xoid.datawidget.databinding.LayoutConfigFormBinding

class WidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var binding: LayoutConfigFormBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setResult(RESULT_CANCELED)

        binding = LayoutConfigFormBinding.inflate(layoutInflater)
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
        var currentUrl = WidgetSettings.getUrl(this, appWidgetId) ?: AppConfig.DEFAULT_JSON_URL
        var currentColor = WidgetSettings.getBgColor(this, appWidgetId)
        var currentAlpha = WidgetSettings.getBgAlpha(this, appWidgetId)
        var currentScreenOn = WidgetSettings.getScreenOnOnly(this, appWidgetId)
        var currentProgVis = WidgetSettings.getProgressVisibility(this, appWidgetId)
        var currentReqType = WidgetSettings.getRequestType(this, appWidgetId)
        var currentFontSize = WidgetSettings.getFontSize(this, appWidgetId)
        var currentName = WidgetSettings.getName(this, appWidgetId) ?: "Widget $appWidgetId"

        // Check if we have data from PendingPinConfig (Bridge)
        val pending = PendingPinConfig.config
        var linkedConfigId = WidgetSettings.getConfigId(this, appWidgetId)

        if (pending != null) {
            currentUrl = pending.url
            currentName = pending.name
            currentColor = ColorUtils.parseColor(pending.bgColor)
            currentAlpha = pending.bgAlpha
            currentScreenOn = pending.updateOnlyScreenOn
            currentProgVis = pending.progressVisibility
            currentReqType = pending.requestType
            currentFontSize = pending.baseFontSize
            linkedConfigId = pending.id // Store the link!
        }

        val tempConfig = WidgetConfig(currentName, currentUrl, String.format("#%06X", (0xFFFFFF and currentColor)), currentAlpha, currentScreenOn, currentProgVis, currentReqType, currentFontSize)
        if (pending != null) tempConfig.id = pending.id

        val helper = ConfigUiHelper(this, layoutInflater, binding)
        helper.setup(tempConfig)

        binding.btnSave.setOnClickListener {
            if (!helper.isValid()) return@setOnClickListener

            val name = binding.editName.text.toString().trim()
            val url = binding.editUrl.text.toString().trim()
            val alpha = helper.selectedAlpha
            val screenOnOnly = binding.checkScreenOn.isChecked
            val progVis = if (binding.radioOnTap.isChecked) "on_tap" else "always"
            val requestType = if (binding.radioPost.isChecked) "POST" else "GET"
            val fontSize = helper.selectedFontSize

            try {
                WidgetSettings.saveUrl(this, appWidgetId, url)
                WidgetSettings.saveName(this, appWidgetId, name)
                WidgetSettings.saveSettings(this, appWidgetId, helper.selectedColor, alpha, screenOnOnly, progVis, requestType, fontSize)
                
                // Establish or break the link based on URL
                val library = ConfigManager.getConfigs(this)
                val matching = library.find { it.url == url }
                if (matching != null) {
                    WidgetSettings.saveConfigId(this, appWidgetId, matching.id)
                } else {
                    val prefs = getSharedPreferences("WidgetSettings", Context.MODE_PRIVATE)
                    prefs.edit().remove("config_id_$appWidgetId").apply()
                }

                // Clear the bridge only on successful save
                PendingPinConfig.config = null

                // Update the widget immediately
                AppWidgetManager.getInstance(this)
                val serviceIntent = Intent(this, UpdateService::class.java).apply {
                    action = UpdateService.ACTION_UPDATE_WIDGETS
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
                }
                startForegroundService(serviceIntent)

                val resultValue = Intent()
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(RESULT_OK, resultValue)
                finish()
            } catch (_: Exception) {
                Toast.makeText(this, "Error saving settings", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
