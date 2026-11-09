package name.xoid.datawidget

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import name.xoid.datawidget.databinding.DialogColorPickerBinding
import name.xoid.datawidget.databinding.FragmentSettingsBinding
import kotlin.math.abs

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            updateNotificationPermissionUI()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply bottom system bar insets so content is not covered by navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)
            insets
        }

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

        // Grant Notifications Permission Button
        binding.btnGrantNotifPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val isGranted = requireContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                if (!isGranted) {
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                        // User might have denied previously or permanently denied; attempt to launch prompt or open settings
                        try {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } catch (_: Exception) {
                            openAppSettings()
                        }
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateNotificationPermissionUI()
    }

    private fun updateNotificationPermissionUI() {
        if (_binding == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = requireContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            binding.containerPermissions.visibility = View.VISIBLE
            binding.dividerPermissions.visibility = View.VISIBLE
            if (isGranted) {
                binding.txtNotifPermStatus.text = getString(R.string.notif_perm_granted)
                binding.btnGrantNotifPermission.visibility = View.GONE
            } else {
                binding.txtNotifPermStatus.text = getString(R.string.notif_perm_denied)
                binding.btnGrantNotifPermission.visibility = View.VISIBLE
            }
        } else {
            binding.containerPermissions.visibility = View.GONE
            binding.dividerPermissions.visibility = View.GONE
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
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
            "#1A237E".toColorInt(), "#008000".toColorInt(),
            "#9ACD32".toColorInt(), "#FFA500".toColorInt(),
            "#FFC0CB".toColorInt(), "#87CEEB".toColorInt()
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
                view.setOnClickListener {
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
