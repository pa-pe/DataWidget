package name.xoid.datawidget

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.content.Intent
import name.xoid.datawidget.databinding.ActivityMainBinding
import name.xoid.datawidget.databinding.LayoutConfigEditBinding

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

        binding.fab.setOnClickListener { view ->
            val editBinding = LayoutConfigEditBinding.inflate(layoutInflater)
            val helper = ConfigUiHelper(this, layoutInflater, editBinding)
            
            // Default values for a new config
            val newConfig = WidgetConfig("New Config", "")
            helper.setup(newConfig)
            
            editBinding.txtTitle.text = "Add New Configuration"
            editBinding.btnSave.visibility = android.view.View.GONE

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("New Configuration")
                .setView(editBinding.root)
                .setPositiveButton("Add") { _, _ ->
                    val name = editBinding.editName.text.toString()
                    val url = editBinding.editUrl.text.toString()
                    if (url.isNotEmpty()) {
                        val finalName = if (name.isEmpty()) "Unnamed" else name
                        val config = WidgetConfig(
                            finalName, 
                            url, 
                            String.format("#%06X", (0xFFFFFF and helper.selectedColor)),
                            helper.selectedAlpha,
                            editBinding.checkScreenOn.isChecked,
                            if (editBinding.radioOnTap.isChecked) "on_tap" else "always",
                            if (editBinding.radioPost.isChecked) "POST" else "GET",
                            helper.selectedFontSize
                        )
                        
                        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
                        val currentFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
                        if (currentFragment is ConfigListFragment) {
                            currentFragment.addConfig(config)
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_about -> {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.action_ConfigListFragment_to_AboutFragment)
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