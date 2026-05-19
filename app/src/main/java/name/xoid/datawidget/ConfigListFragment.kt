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
import android.content.Intent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import name.xoid.datawidget.databinding.LayoutConfigFormBinding
import androidx.core.graphics.toColorInt

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
        configs = ConfigManager.getConfigs(requireContext())
        refreshList()
    }

    fun addConfig(config: WidgetConfig) {
        configs.add(config)
        ConfigManager.saveConfigs(requireContext(), configs)
        refreshList()
    }

    private fun refreshList() {
        val context = requireContext()
        binding.itemsContainer.removeAllViews()
        
        // Count active widgets for each config ID
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, DataWidgetProvider::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        
        val usageCounts = mutableMapOf<String, Int>()
        for (id in allWidgetIds) {
            val configId = WidgetSettings.getConfigId(context, id)
            if (configId != null) {
                usageCounts[configId] = (usageCounts[configId] ?: 0) + 1
            } else {
                // FALLBACK: If old widget without ID, try to match by URL
                val url = WidgetSettings.getUrl(context, id)
                if (url != null) {
                    val matchingConfig = configs.find { it.url == url }
                    if (matchingConfig != null) {
                        usageCounts[matchingConfig.id] = (usageCounts[matchingConfig.id] ?: 0) + 1
                        // Heal the widget by saving the ID link for next time
                        WidgetSettings.saveConfigId(context, id, matchingConfig.id)
                    }
                }
            }
        }
        
        configs.forEach { config ->
            val itemBinding = ItemConfigBinding.inflate(layoutInflater, binding.itemsContainer, false)
            itemBinding.configName.text = config.name
            
            val count = usageCounts[config.id] ?: 0
            if (count > 0) {
                itemBinding.configStatus.text = context.getString(R.string.status_pinned_count, count)
                itemBinding.configStatus.setTextColor("#4CAF50".toColorInt()) // Nice green
            } else {
                itemBinding.configStatus.text = context.getString(R.string.status_not_pinned)
                itemBinding.configStatus.setTextColor(android.graphics.Color.GRAY)
            }
            
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

        dialogBinding.btnEdit.text = getString(R.string.btn_edit)
        dialogBinding.btnEdit.setOnClickListener {
            dialog.dismiss()
            showEditDialog(config)
        }

        dialogBinding.btnTest.text = getString(R.string.btn_test)
        dialogBinding.btnTest.setOnClickListener {
            dialog.dismiss()
            runTest(config)
        }

        dialogBinding.btnPin.text = getString(R.string.btn_pin_to_home)
        dialogBinding.btnPin.setOnClickListener {
            dialog.dismiss()
            pinWidget(config)
        }

        dialogBinding.btnDelete.text = getString(R.string.btn_delete_from_library)
        dialogBinding.btnDelete.setOnClickListener {
            dialog.dismiss()
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setPositiveButton(R.string.btn_delete) { _, _ ->
                    configs.remove(config)
                    ConfigManager.saveConfigs(requireContext(), configs)
                    refreshList()
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        }

        dialog.show()
    }

    private fun showEditDialog(config: WidgetConfig) {
        val editBinding = LayoutConfigFormBinding.inflate(layoutInflater)
        val helper = ConfigUiHelper(requireContext(), layoutInflater, editBinding)
        helper.setup(config)

        // Hide the internal title and save button because we'll use the Dialog's UI
        editBinding.txtTitle.visibility = View.GONE
        editBinding.btnSave.visibility = View.GONE

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.title_edit_config)
            .setView(editBinding.root)
            .setPositiveButton(R.string.btn_save, null) // Set to null to handle manually for validation
            .setNegativeButton(R.string.btn_cancel, null)
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
        
        // Make the dialog wider (95% of screen width)
        val width = (resources.displayMetrics.widthPixels * 0.95).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun runTest(config: WidgetConfig) {
        val progressDialog = AlertDialog.Builder(requireContext())
            .setMessage(R.string.dialog_testing_message)
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
                results.add(getString(R.string.test_conn_success))
                text
            } catch (e: Exception) {
                success = false
                results.add(getString(R.string.test_conn_failed, e.message ?: ""))
                null
            }

            // 2. Parse and validate
            if (content != null) {
                try {
                    val json = JSONObject(content)
                    results.add(getString(R.string.test_json_success))
                    
                    val errors = mutableListOf<String>()
                    
                    if (!json.has("update_interval_sec") && !json.has("next_update_at")) {
                        errors.add(getString(R.string.test_err_params))
                    }
                    
                    val rows = json.optJSONArray("rows")
                    if (rows == null || rows.length() == 0) {
                        errors.add(getString(R.string.test_err_rows))
                    }
                    
                    if (errors.isEmpty()) {
                        results.add(getString(R.string.test_val_success))
                    } else {
                        success = false
                        results.add(getString(R.string.test_val_failed))
                        results.addAll(errors)
                    }
                } catch (e: Exception) {
                    success = false
                    results.add(getString(R.string.test_json_failed, e.message ?: ""))
                }
            }

            activity?.runOnUiThread {
                progressDialog.dismiss()
                AlertDialog.Builder(requireContext())
                    .setTitle(if (success) R.string.test_passed else R.string.test_failed)
                    .setMessage(results.joinToString("\n"))
                    .setPositiveButton(R.string.btn_ok, null)
                    .show()
            }
        }
    }

    private fun pinWidget(config: WidgetConfig) {
        val appWidgetManager = AppWidgetManager.getInstance(requireContext())
        val componentName = ComponentName(requireContext(), DataWidgetProvider::class.java)

        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            // 1. Save config to bridge
            PendingPinConfig.config = config.copy()
            
            // 2. Create callback intent to our PinnedReceiver
            val callbackIntent = Intent(requireContext(), PinnedReceiver::class.java)
            val successCallback = android.app.PendingIntent.getBroadcast(
                requireContext(), 
                0, 
                callbackIntent, 
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
            )
            
            // 3. Request pin with callback
            appWidgetManager.requestPinAppWidget(componentName, null, successCallback)
        } else {
            Toast.makeText(requireContext(), R.string.toast_no_pin_support, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
