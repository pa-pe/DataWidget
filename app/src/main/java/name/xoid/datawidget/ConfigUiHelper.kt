package name.xoid.datawidget

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.SeekBar
import androidx.core.widget.doAfterTextChanged
import name.xoid.datawidget.databinding.LayoutConfigEditBinding
import name.xoid.datawidget.databinding.DialogColorPickerBinding

class ConfigUiHelper(
    private val context: Context,
    private val layoutInflater: LayoutInflater,
    private val binding: LayoutConfigEditBinding
) {
    var selectedColor: Int = Color.WHITE
    var selectedAlpha: Float = 1.0f
    var selectedFontSize: Int = 12

    fun setup(config: WidgetConfig) {
        binding.editName.setText(config.name)
        binding.editUrl.setText(config.url)
        selectedColor = ColorUtils.parseColor(config.bgColor)
        selectedAlpha = config.bgAlpha
        selectedFontSize = config.baseFontSize
        
        binding.editBgColor.setText(config.bgColor)
        updateColorPreview(selectedColor)
        
        binding.seekAlpha.progress = (selectedAlpha * 100).toInt()
        binding.txtAlphaPercent.text = "${binding.seekAlpha.progress}%"

        binding.seekFontSize.progress = selectedFontSize
        binding.txtFontSize.text = "${selectedFontSize}sp"

        if (config.requestType == "POST") {
            binding.radioPost.isChecked = true
        } else {
            binding.radioGet.isChecked = true
        }

        binding.checkScreenOn.isChecked = config.updateOnlyScreenOn
        if (config.progressVisibility == "on_tap") {
            binding.radioOnTap.isChecked = true
        } else {
            binding.radioAlways.isChecked = true
        }

        binding.btnExamples.setOnClickListener {
            showExamplesPicker()
        }

        binding.seekAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedAlpha = progress / 100f
                binding.txtAlphaPercent.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.seekFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val valToSet = progress.coerceAtLeast(6)
                selectedFontSize = valToSet
                binding.txtFontSize.text = "${valToSet}sp"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.viewColorPreview.setOnClickListener {
            showColorPicker { color ->
                selectedColor = color
                val hex = String.format("#%06X", (0xFFFFFF and color))
                binding.editBgColor.setText(hex)
                updateColorPreview(color)
            }
        }

        binding.editBgColor.doAfterTextChanged { s ->
            val colorStr = s.toString()
            if (colorStr.isNotEmpty()) {
                val color = ColorUtils.parseColor(colorStr)
                // Only update preview if it looks like a valid color or we are just typing
                selectedColor = color
                binding.viewColorPreview.setBackgroundColor(color)
            }
        }
    }

    private fun updateColorPreview(color: Int) {
        binding.viewColorPreview.setBackgroundColor(color)
    }

    private fun showExamplesPicker() {
        val examples = ExampleProvider.EXAMPLES
        val names = examples.map { it.name }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Select Example Template")
            .setItems(names) { _, which ->
                val selected = examples[which]
                binding.editName.setText(selected.name)
                binding.editUrl.setText(selected.url)
                
                // Reset visual settings to template defaults (which are now default WidgetConfig values)
                binding.editBgColor.setText(selected.bgColor)
                selectedColor = ColorUtils.parseColor(selected.bgColor)
                updateColorPreview(selectedColor)
                
                selectedAlpha = selected.bgAlpha
                binding.seekAlpha.progress = (selectedAlpha * 100).toInt()
                binding.txtAlphaPercent.text = "${binding.seekAlpha.progress}%"
                
                selectedFontSize = selected.baseFontSize
                binding.seekFontSize.progress = selectedFontSize
                binding.txtFontSize.text = "${selectedFontSize}sp"
                
                if (selected.requestType == "POST") binding.radioPost.isChecked = true else binding.radioGet.isChecked = true
                binding.checkScreenOn.isChecked = selected.updateOnlyScreenOn
                if (selected.progressVisibility == "on_tap") binding.radioOnTap.isChecked = true else binding.radioAlways.isChecked = true
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showColorPicker(onColorSelected: (Int) -> Unit) {
        val colors = intArrayOf(
            Color.WHITE, Color.BLACK, Color.LTGRAY, Color.DKGRAY,
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
            Color.CYAN, Color.MAGENTA, 
            Color.parseColor("#1A237E"), Color.parseColor("#008000"),
            Color.parseColor("#9ACD32"), Color.parseColor("#FFA500"), 
            Color.parseColor("#FFC0CB"), Color.parseColor("#87CEEB")
        )

        val gridBinding = DialogColorPickerBinding.inflate(layoutInflater)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Select Color")
            .setView(gridBinding.root)
            .create()

        gridBinding.colorGrid.adapter = object : BaseAdapter() {
            override fun getCount(): Int = colors.size
            override fun getItem(position: Int): Any = colors[position]
            override fun getItemId(position: Int): Long = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = convertView ?: View(context)
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
