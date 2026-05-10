package name.xoid.datawidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import name.xoid.datawidget.databinding.FragmentSettingsBinding

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

        val currentRadius = AppSettings.getWidgetRadius(requireContext())
        binding.seekRadius.progress = currentRadius
        binding.txtRadiusValue.text = "${currentRadius}dp"
        binding.cardPreview.radius = currentRadius * resources.displayMetrics.density

        binding.seekRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Magnetic snapping to our predefined steps: 0, 8, 16, 24, 32
                val steps = intArrayOf(0, 8, 16, 24, 32)
                val snapped = steps.minByOrNull { Math.abs(it - progress) } ?: progress
                
                binding.txtRadiusValue.text = "${snapped}dp"
                binding.cardPreview.radius = snapped * resources.displayMetrics.density
                
                if (fromUser) {
                    if (snapped != progress) {
                        seekBar?.progress = snapped
                    }
                    AppSettings.saveWidgetRadius(requireContext(), snapped)
                    // Trigger refresh for all widgets to apply the new radius
                    val serviceIntent = Intent(requireContext(), UpdateService::class.java).apply {
                        action = UpdateService.ACTION_UPDATE_WIDGETS
                        val appWidgetManager = AppWidgetManager.getInstance(requireContext())
                        val componentName = android.content.ComponentName(requireContext(), DataWidgetProvider::class.java)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetManager.getAppWidgetIds(componentName))
                    }
                    requireContext().startForegroundService(serviceIntent)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
