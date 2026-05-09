package name.xoid.datawidget

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import name.xoid.datawidget.databinding.FragmentConfigListBinding
import name.xoid.datawidget.databinding.ItemConfigBinding
import name.xoid.datawidget.databinding.DialogConfigActionsBinding
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import org.json.JSONObject
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import name.xoid.datawidget.databinding.LayoutConfigFormBinding

class ConfigListFragment : Fragment() {

    private var _binding: FragmentConfigListBinding? = null
    private val binding get() = _binding!!

    private var configs = mutableListOf<WidgetConfig>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        ConfigManager.syncWithActiveWidgets(requireContext())
        configs = ConfigManager.getConfigs(requireContext())
        refreshList()
    }

    fun addConfig(config: WidgetConfig) {
        configs.add(config)
        ConfigManager.saveConfigs(requireContext(), configs)
        refreshList()
    }

    private fun refreshList() {
        binding.itemsContainer.removeAllViews()
        
        configs.forEach { config ->
            val itemBinding = ItemConfigBinding.inflate(layoutInflater, binding.itemsContainer, false)
            itemBinding.configName.text = config.name
            itemBinding.configUrl.text = config.url
            
            itemBinding.root.setOnClickListener {
                showActionsDialog(config)
            }
            
            binding.itemsContainer.addView(itemBinding.root)
        }
    }

    private fun showActionsDialog(config: WidgetConfig) {
        val dialogBinding = DialogConfigActionsBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(config.name)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnEdit.setOnClickListener {
            dialog.dismiss()
            showEditDialog(config)
        }

        dialogBinding.btnTest.setOnClickListener {
            dialog.dismiss()
            runTest(config)
        }

        dialogBinding.btnPin.setOnClickListener {
            dialog.dismiss()
            pinWidget(config)
        }

        dialogBinding.btnDelete.setOnClickListener {
            dialog.dismiss()
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Configuration")
                .setMessage("Are you sure you want to remove this from your library?")
                .setPositiveButton("Delete") { _, _ ->
                    configs.remove(config)
                    ConfigManager.saveConfigs(requireContext(), configs)
                    refreshList()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialog.show()
    }

    private fun showEditDialog(config: WidgetConfig) {
        val editBinding = LayoutConfigFormBinding.inflate(layoutInflater)
        val helper = ConfigUiHelper(requireContext(), layoutInflater, editBinding)
        helper.setup(config)

        editBinding.txtTitle.text = "Edit Configuration"
        editBinding.btnSave.visibility = View.GONE

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Configuration")
            .setView(editBinding.root)
            .setPositiveButton("Save", null) // Set to null to handle manually for validation
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                if (helper.isValid()) {
                    config.name = editBinding.editName.text.toString().trim()
                    config.url = editBinding.editUrl.text.toString().trim()
                    config.bgColor = String.format("#%06X", (0xFFFFFF and helper.selectedColor))
                    config.bgAlpha = helper.selectedAlpha
                    config.updateOnlyScreenOn = editBinding.checkScreenOn.isChecked
                    
                    val progVis = if (editBinding.radioOnTap.isChecked) "on_tap" else "always"
                    config.progressVisibility = progVis
                    
                    config.requestType = if (editBinding.radioPost.isChecked) "POST" else "GET"
                    config.baseFontSize = helper.selectedFontSize
                    
                    ConfigManager.saveConfigs(requireContext(), configs)
                    refreshList()
                    dialog.dismiss()
                }
            }
        }
        
        dialog.show()
    }

    private fun runTest(config: WidgetConfig) {
        val progressDialog = AlertDialog.Builder(requireContext())
            .setMessage("Testing connection...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        thread {
            val results = mutableListOf<String>()
            var success = true
            
            // 1. Fetch content
            val content = try {
                val url = URL(config.url)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = config.requestType
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                results.add("✅ Connection: Success")
                text
            } catch (e: Exception) {
                success = false
                results.add("❌ Connection: Failed (${e.message})")
                null
            }

            // 2. Parse and validate
            if (content != null) {
                try {
                    val json = JSONObject(content)
                    results.add("✅ Parse JSON: Success")
                    
                    val errors = mutableListOf<String>()
                    
                    if (!json.has("update_interval_sec") && !json.has("next_update_at")) {
                        errors.add("- Missing update parameters")
                    }
                    
                    val rows = json.optJSONArray("rows")
                    if (rows == null || rows.length() == 0) {
                        errors.add("- No rows found")
                    }
                    
                    if (errors.isEmpty()) {
                        results.add("✅ Validation: Success")
                    } else {
                        success = false
                        results.add("❌ Validation: Failed")
                        results.addAll(errors)
                    }
                } catch (e: Exception) {
                    success = false
                    results.add("❌ Parse JSON: Failed (${e.message})")
                }
            }

            activity?.runOnUiThread {
                progressDialog.dismiss()
                AlertDialog.Builder(requireContext())
                    .setTitle(if (success) "Test Passed" else "Test Failed")
                    .setMessage(results.joinToString("\n"))
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun pinWidget(config: WidgetConfig) {
        val appWidgetManager = AppWidgetManager.getInstance(requireContext())
        val componentName = ComponentName(requireContext(), DataWidgetProvider::class.java)

        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            appWidgetManager.requestPinAppWidget(componentName, null, null)
        } else {
            Toast.makeText(requireContext(), "Your launcher does not support direct pinning", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
