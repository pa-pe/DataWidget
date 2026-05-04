package name.xoid.datawidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import name.xoid.datawidget.databinding.ActivityWidgetConfigBinding

class WidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var binding: ActivityWidgetConfigBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setResult(RESULT_CANCELED)

        binding = ActivityWidgetConfigBinding.inflate(layoutInflater)
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

        binding.editUrl.setText(currentUrl)
        binding.editBgColor.setText(String.format("#%06X", (0xFFFFFF and currentColor)))
        binding.editBgAlpha.setText(currentAlpha.toString())

        binding.btnSave.setOnClickListener {
            val url = binding.editUrl.text.toString()
            val colorStr = binding.editBgColor.text.toString()
            val alphaStr = binding.editBgAlpha.text.toString()

            if (url.isEmpty()) {
                Toast.makeText(this, "URL cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val color = Color.parseColor(colorStr)
                val alpha = alphaStr.toFloat().coerceIn(0f, 1f)

                WidgetSettings.saveUrl(this, appWidgetId, url)
                WidgetSettings.saveBgSettings(this, appWidgetId, color, alpha)

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
                Toast.makeText(this, "Invalid color or alpha", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
