package name.xoid.datawidget

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.content.Intent
import name.xoid.datawidget.databinding.ActivityMainBinding
import name.xoid.datawidget.databinding.LayoutConfigFormBinding
import androidx.appcompat.app.AlertDialog
import android.view.ViewGroup

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Ensure service is running
        val serviceIntent = Intent(this, UpdateService::class.java)
        startForegroundService(serviceIntent)

        binding.fab.setOnClickListener {
            val editBinding = LayoutConfigFormBinding.inflate(layoutInflater)
            val helper = ConfigUiHelper(this, layoutInflater, editBinding)
            
            // Default values for a new config
            val newConfig = WidgetConfig(getString(R.string.error_unnamed), "")
            helper.setup(newConfig)
            
            editBinding.txtTitle.visibility = android.view.View.GONE
            editBinding.btnSave.visibility = android.view.View.GONE

            val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.title_add_config)
                .setView(editBinding.root)
                .setPositiveButton(R.string.btn_add, null)
                .setNegativeButton(R.string.btn_cancel, null)
                .create()

            dialog.setOnShowListener {
                val addButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                addButton.setOnClickListener {
                    if (helper.isValid()) {
                        val name = editBinding.editName.text.toString().trim()
                        val url = editBinding.editUrl.text.toString().trim()
                        
                        val config = WidgetConfig(
                            name.ifEmpty { getString(R.string.error_unnamed) }, 
                            url, 
                            String.format("#%06X", (0xFFFFFF and helper.selectedColor)),
                            helper.selectedAlpha,
                            editBinding.checkScreenOn.isChecked,
                            if (editBinding.radioOnTap.isChecked) "on_tap" else "always",
                            if (editBinding.radioPost.isChecked) "POST" else "GET",
                            helper.selectedFontSize
                        )
                        
                        val hostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
                        val currentFragment = hostFragment.childFragmentManager.fragments.firstOrNull()
                        if (currentFragment is ConfigListFragment) {
                            currentFragment.addConfig(config)
                        }
                        dialog.dismiss()
                    }
                }
            }
            
            dialog.show()
            
            // Make the dialog wider (95% of screen width)
            val width = (resources.displayMetrics.widthPixels * 0.95).toInt()
            dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.SettingsFragment)
                true
            }
            R.id.action_about -> {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.AboutFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}
