package name.xoid.datawidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import android.view.View
import android.view.ViewGroup
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
        var selectedColor = WidgetSettings.getBgColor(this, appWidgetId)
        var selectedAlpha = WidgetSettings.getBgAlpha(this, appWidgetId)
        val currentScreenOn = WidgetSettings.getScreenOnOnly(this, appWidgetId)
        val currentProgVis = WidgetSettings.getProgressVisibility(this, appWidgetId)

        binding.editUrl.setText(currentUrl)
        updateColorPreview(selectedColor)
        
        binding.seekAlpha.progress = (selectedAlpha * 100).toInt()
        binding.txtAlphaPercent.text = "${binding.seekAlpha.progress}%"

        binding.checkScreenOn.isChecked = currentScreenOn
        if (currentProgVis == "on_tap") {
            binding.radioOnTap.isChecked = true
        } else {
            binding.radioAlways.isChecked = true
        }

        binding.seekAlpha.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                selectedAlpha = progress / 100f
                binding.txtAlphaPercent.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        binding.btnPickColor.setOnClickListener {
            showColorPicker { color ->
                selectedColor = color
                updateColorPreview(color)
            }
        }

        binding.btnSave.setOnClickListener {
            val url = binding.editUrl.text.toString()
            val colorStr = String.format("#%06X", (0xFFFFFF and selectedColor))
            val screenOnOnly = binding.checkScreenOn.isChecked
            val progVis = if (binding.radioOnTap.isChecked) "on_tap" else "always"

            if (url.isEmpty()) {
                Toast.makeText(this, "URL cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                WidgetSettings.saveUrl(this, appWidgetId, url)
                WidgetSettings.saveBgSettings(this, appWidgetId, selectedColor, selectedAlpha, screenOnOnly, progVis)

                // Also add/update in the global config library
                val configs = ConfigManager.getConfigs(this)
                val existing = configs.find { it.url == url }
                if (existing != null) {
                    existing.bgColor = colorStr
                    existing.bgAlpha = selectedAlpha
                    existing.updateOnlyScreenOn = screenOnOnly
                    existing.progressVisibility = progVis
                } else {
                    configs.add(WidgetConfig("Widget $appWidgetId", url, colorStr, selectedAlpha, screenOnOnly, progVis))
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

    private fun updateColorPreview(color: Int) {
        binding.viewColorPreview.setBackgroundColor(color)
        binding.txtColorHex.text = String.format("#%06X", (0xFFFFFF and color))
    }

    private fun showColorPicker(onColorSelected: (Int) -> Unit) {
        val colors = intArrayOf(
            Color.WHITE, Color.BLACK, Color.LTGRAY, Color.DKGRAY,
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
            Color.CYAN, Color.MAGENTA, Color.parseColor("#1A237E"), Color.parseColor("#008000"),
            Color.parseColor("#9ACD32"), Color.parseColor("#FFA500"), Color.parseColor("#FFC0CB"), Color.parseColor("#87CEEB")
        )

        val gridView = layoutInflater.inflate(R.layout.dialog_color_picker, null) as android.widget.GridView
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Color")
            .setView(gridView)
            .create()

        gridView.adapter = object : android.widget.BaseAdapter() {
            override fun getCount(): Int = colors.size
            override fun getItem(position: Int): Any = colors[position]
            override fun getItemId(position: Int): Long = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = convertView ?: View(this@WidgetConfigActivity)
                view.layoutParams = android.widget.AbsListView.LayoutParams(120, 120)
                view.setBackgroundColor(colors[position])
                view.setOnClickListener {
                    onColorSelected(colors[position])
                    dialog.dismiss()
                }
                return view
            }
        }
        dialog.show()
    }
}
