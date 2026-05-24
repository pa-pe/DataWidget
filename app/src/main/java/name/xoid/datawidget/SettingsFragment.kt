package name.xoid.datawidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import name.xoid.datawidget.databinding.FragmentSettingsBinding
import name.xoid.datawidget.databinding.DialogColorPickerBinding
import kotlin.math.abs

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load Radius
        val currentRadius = AppSettings.getWidgetRadius(requireContext())
        binding.seekRadius.progress = currentRadius
        binding.txtRadiusValue.text = getString(R.string.dp_unit, currentRadius)
        binding.cardPreview.radius = currentRadius * resources.displayMetrics.density

        // Load Padding
        val currentPadding = AppSettings.getWidgetPadding(requireContext())
        binding.seekPadding.progress = currentPadding
        binding.txtPaddingValue.text = getString(R.string.dp_unit, currentPadding)
        val paddingPx = (currentPadding * resources.displayMetrics.density).toInt()
        binding.previewContentContainer.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)

        // Load Font Size
        val currentFontSize = AppSettings.getWidgetFontSize(requireContext())
        binding.seekFontSize.progress = currentFontSize
        binding.txtFontValue.text = getString(R.string.sp_unit, currentFontSize)
        binding.previewText.textSize = currentFontSize.toFloat()

        // Load Panel Color
        val currentPanelColorHex = AppSettings.getPanelBgColor(requireContext())
        val currentPanelColor = ColorUtils.parseColor(currentPanelColorHex)
        binding.txtPanelColorHex.text = currentPanelColorHex
        binding.viewPanelColorPreview.setBackgroundColor(currentPanelColor)

        // Load Panel Alpha
        val currentPanelAlpha = AppSettings.getPanelBgAlpha(requireContext())
        binding.seekPanelAlpha.progress = (currentPanelAlpha * 100).toInt()
        binding.txtPanelAlphaValue.text = getString(R.string.percent_unit, (currentPanelAlpha * 100).toInt())

        updatePreview()

        // Radius Listener
        binding.seekRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val steps = intArrayOf(0, 8, 16, 24, 32)
                val snapped = steps.minByOrNull { abs(it - progress) } ?: progress
                binding.txtRadiusValue.text = getString(R.string.dp_unit, snapped)
                binding.cardPreview.radius = snapped * resources.displayMetrics.density
                
                val marginPx = ((snapped + 2) * resources.displayMetrics.density).toInt()
                val lp = binding.previewProgress.layoutParams as ViewGroup.MarginLayoutParams
                lp.setMargins(marginPx, 0, marginPx, 0)
                binding.previewProgress.layoutParams = lp

                if (fromUser) {
                    if (snapped != progress) {
                        seekBar?.progress = snapped
                    }
                    AppSettings.saveWidgetRadius(requireContext(), snapped)
                    triggerWidgetUpdate()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Padding Listener
        binding.seekPadding.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.txtPaddingValue.text = getString(R.string.dp_unit, progress)
                val px = (progress * resources.displayMetrics.density).toInt()
                binding.previewContentContainer.setPadding(px, px, px, px)
                if (fromUser) {
                    AppSettings.saveWidgetPadding(requireContext(), progress)
                    triggerWidgetUpdate()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Font Size Listener
        binding.seekFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fontSize = progress.coerceAtLeast(6)
                binding.txtFontValue.text = getString(R.string.sp_unit, fontSize)
                binding.previewText.textSize = fontSize.toFloat()
                if (fromUser) {
                    AppSettings.saveWidgetFontSize(requireContext(), fontSize)
                    triggerWidgetUpdate()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Panel Color Picker
        binding.viewPanelColorPreview.setOnClickListener {
            showColorPicker { color ->
                val hex = String.format("#%06X", (0xFFFFFF and color))
                AppSettings.savePanelBgColor(requireContext(), hex)
                binding.txtPanelColorHex.text = hex
                binding.viewPanelColorPreview.setBackgroundColor(color)
                updatePreview()
                triggerWidgetUpdate()
            }
        }

        // Panel Alpha Listener
        binding.seekPanelAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.txtPanelAlphaValue.text = getString(R.string.percent_unit, progress)
                updatePreview()
                if (fromUser) {
                    AppSettings.savePanelBgAlpha(requireContext(), progress / 100f)
                    triggerWidgetUpdate()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updatePreview() {
        // Panel Preview Update
        val panelColorHex = AppSettings.getPanelBgColor(requireContext())
        val panelColor = ColorUtils.parseColor(panelColorHex)
        val panelAlpha = AppSettings.getPanelBgAlpha(requireContext())
        
        // Combine alpha into the color for the preview view
        val alphaInt = (panelAlpha * 255).toInt().coerceIn(0, 255)
        val combinedColor = Color.argb(
            alphaInt,
            Color.red(panelColor),
            Color.green(panelColor),
            Color.blue(panelColor)
        )
        binding.previewPanel.setBackgroundColor(combinedColor)
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
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_color_picker_title)
            .setView(gridBinding.root)
            .create()

        gridBinding.colorGrid.adapter = object : BaseAdapter() {
            override fun getCount(): Int = colors.size
            override fun getItem(position: Int): Any = colors[position]
            override fun getItemId(position: Int): Long = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = convertView ?: LayoutInflater.from(requireContext()).inflate(R.layout.item_color_tile, parent, false)
                val colorView = view.findViewById<View>(R.id.color_tile)
                colorView.setBackgroundColor(colors[position])
                colorView.setOnClickListener {
                    onColorSelected(colors[position])
                    dialog.dismiss()
                }
                return view
            }
        }
        dialog.show()
    }

    private fun triggerWidgetUpdate() {
        val serviceIntent = Intent(requireContext(), UpdateService::class.java).apply {
            action = UpdateService.ACTION_UPDATE_WIDGETS
            val appWidgetManager = AppWidgetManager.getInstance(requireContext())
            val componentName = android.content.ComponentName(requireContext(), DataWidgetProvider::class.java)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetManager.getAppWidgetIds(componentName))
        }
        requireContext().startForegroundService(serviceIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
