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

    fun addConfig(name: String, url: String) {
        configs.add(WidgetConfig(name, url))
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

        dialog.show()
    }

    private fun showEditDialog(config: WidgetConfig) {
        val inputName = android.widget.EditText(context).apply { 
            hint = "Name" 
            setText(config.name)
        }
        val inputUrl = android.widget.EditText(context).apply { 
            hint = "URL" 
            setText(config.url)
        }
        val inputBgColor = android.widget.EditText(context).apply { 
            hint = "BG Color (HEX)" 
            setText(config.bgColor)
        }
        val inputBgAlpha = android.widget.EditText(context).apply { 
            hint = "Transparency (0.0 - 1.0)" 
            setText(config.bgAlpha.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val inputScreenOn = android.widget.CheckBox(context).apply {
            text = "Update only when screen is on"
            isChecked = config.updateOnlyScreenOn
        }
        val inputProgressVis = android.widget.RadioGroup(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val rbAlways = android.widget.RadioButton(context).apply { 
                id = View.generateViewId()
                text = "Always Show Progress" 
            }
            val rbOnTap = android.widget.RadioButton(context).apply { 
                id = View.generateViewId()
                text = "Show Progress on Tap" 
            }
            addView(rbAlways)
            addView(rbOnTap)
            if (config.progressVisibility == "on_tap") rbOnTap.isChecked = true else rbAlways.isChecked = true
        }

        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(inputName)
            addView(inputUrl)
            addView(inputBgColor)
            addView(inputBgAlpha)
            addView(inputScreenOn)
            addView(android.widget.TextView(context).apply { text = "Progress Bar Visibility"; setPadding(0, 10, 0, 0) })
            addView(inputProgressVis)
            setPadding(50, 40, 50, 10)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Configuration")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                try {
                    config.name = inputName.text.toString()
                    config.url = inputUrl.text.toString()
                    config.bgColor = inputBgColor.text.toString()
                    config.bgAlpha = inputBgAlpha.text.toString().toFloat().coerceIn(0f, 1f)
                    config.updateOnlyScreenOn = inputScreenOn.isChecked
                    
                    val selectedId = inputProgressVis.checkedRadioButtonId
                    config.progressVisibility = if (selectedId != -1) {
                        val rb = inputProgressVis.findViewById<android.widget.RadioButton>(selectedId)
                        if (rb.text.toString().contains("Tap")) "on_tap" else "always"
                    } else "always"
                    
                    ConfigManager.saveConfigs(requireContext(), configs)
                    refreshList()
                } catch (e: Exception) {
                    Toast.makeText(context, "Invalid data", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
                        errors.add("- Missing update parameters (update_interval_sec or next_update_at)")
                    }
                    
                    val rows = json.optJSONArray("rows")
                    if (rows == null || rows.length() == 0) {
                        errors.add("- No rows found")
                    } else {
                        var hasCol = false
                        for (i in 0 until rows.length()) {
                            val cols = rows.getJSONObject(i).optJSONArray("cols")
                            if (cols != null && cols.length() > 0) {
                                hasCol = true
                                break
                            }
                        }
                        if (!hasCol) errors.add("- No columns found in any row")
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
