package name.xoid.datawidget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.IBinder
import android.widget.RemoteViews
import java.util.Timer
import java.util.TimerTask
import android.graphics.Color
import org.json.JSONObject
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread
import android.content.pm.ServiceInfo
import android.os.Build
import android.view.View
import android.widget.Toast
import android.view.Gravity
import android.os.PowerManager
import android.content.Context

class UpdateService : Service() {

    private var timer: Timer? = null
    private var widgetIds = mutableSetOf<Int>()
    private val cachedData = mutableMapOf<Int, String>()
    private val nextFetchTime = mutableMapOf<Int, Long>()
    private val lastFetchTime = mutableMapOf<Int, Long>()
    private val fetchError = mutableMapOf<Int, Boolean>()
    private val controlsVisible = mutableMapOf<Int, Boolean>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        // Recover active widget IDs
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = android.content.ComponentName(this, DataWidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)
        widgetIds.addAll(ids.toTypedArray())

        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        startTimer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("UpdateService", "onStartCommand action: ${intent?.action}")
        val appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        when (intent?.action) {
            ACTION_UPDATE_WIDGETS -> {
                val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                Log.d("UpdateService", "Received IDs: ${ids?.joinToString()}")
                ids?.let {
                    widgetIds.addAll(it.toTypedArray())
                    it.forEach { id ->
                        fetchData(id)
                    }
                    updateAllWidgets()
                }
            }
            ACTION_TOGGLE_CONTROLS -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    controlsVisible[appWidgetId] = !(controlsVisible[appWidgetId] ?: false)
                    updateWidget(this, AppWidgetManager.getInstance(this), appWidgetId)
                }
            }
            ACTION_FORCE_REFRESH -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    fetchData(appWidgetId)
                    Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show()
                }
            }
            ACTION_REMOVE_WIDGETS -> {
                val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ids?.let {
                    it.forEach { id ->
                        widgetIds.remove(id)
                        cachedData.remove(id)
                    }
                }
                if (widgetIds.isEmpty()) stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startTimer() {
        timer?.cancel()
        timer = Timer()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        timer?.schedule(object : TimerTask() {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                val isScreenOn = powerManager.isInteractive

                widgetIds.forEach { id ->
                    val onlyScreenOn = WidgetSettings.getScreenOnOnly(this@UpdateService, id)

                    // Skip fetching if screen is off and setting is enabled
                    if (onlyScreenOn && !isScreenOn) {
                        return@forEach
                    }

                    val nextFetch = nextFetchTime[id] ?: 0L
                    if (currentTime >= nextFetch) {
                        // Avoid immediate re-trigger by setting a temporary future time
                        nextFetchTime[id] = currentTime + 10000
                        fetchData(id)
                    }
                }
                updateAllWidgets()
            }
        }, 0, AppConfig.UI_UPDATE_INTERVAL_MS)
    }

    private fun fetchData(appWidgetId: Int) {
        val urlString = WidgetSettings.getUrl(this, appWidgetId) ?: AppConfig.DEFAULT_JSON_URL
        val requestType = WidgetSettings.getRequestType(this, appWidgetId)
        
        thread {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = requestType
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                if (requestType == "POST") {
                    connection.doOutput = true
                    // You could add POST data here in the future
                }

                val content = connection.inputStream.bufferedReader().use { it.readText() }

                cachedData[appWidgetId] = content
                fetchError[appWidgetId] = false
                val now = System.currentTimeMillis()
                lastFetchTime[appWidgetId] = now

                // Parse intervals from JSON
                val json = JSONObject(content)
                val intervalSec = json.optLong("update_interval_sec", AppConfig.DEFAULT_FETCH_INTERVAL_SEC)
                val nextAt = json.optLong("next_update_at", -1L) // timestamp in seconds

                nextFetchTime[appWidgetId] = if (nextAt > 0) {
                    nextAt * 1000
                } else {
                    now + (intervalSec * 1000)
                }

                Log.d("UpdateService", "Fetched data for $appWidgetId. Next fetch at ${nextFetchTime[appWidgetId]}")
                updateAllWidgets() // Update UI immediately after fetch
            } catch (e: Exception) {
                Log.e("UpdateService", "Error fetching data", e)
                fetchError[appWidgetId] = true
                // Retry in a bit on error
                nextFetchTime[appWidgetId] = System.currentTimeMillis() + 10000
                updateAllWidgets() // Show error state immediately
            }
        }
    }

    private fun updateAllWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = powerManager.isInteractive

        for (id in widgetIds) {
            val onlyScreenOn = WidgetSettings.getScreenOnOnly(this, id)
            if (onlyScreenOn && !isScreenOn) {
                // Skip update to save battery
                continue
            }
            updateWidget(this, appWidgetManager, id)
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        Log.d("UpdateService", "updateWidget called for $appWidgetId")
        val root = RemoteViews(context.packageName, R.layout.widget_main)

        try {
            val jsonString = cachedData[appWidgetId]
                ?: throw Exception("Waiting for data from GitHub...")
            val jsonObject = JSONObject(jsonString)

            // Background color and transparency from WidgetSettings (User customization)
            val userBgColor = WidgetSettings.getBgColor(context, appWidgetId)
            val userBgAlpha = WidgetSettings.getBgAlpha(context, appWidgetId)

            // Fallback to JSON if user hasn't set anything? 
            // For now, let's assume WidgetSettings always has a default (White, 1.0)

            val finalColor = Color.argb(
                (userBgAlpha * 255).toInt().coerceIn(0, 255),
                Color.red(userBgColor),
                Color.green(userBgColor),
                Color.blue(userBgColor)
            )
            root.setInt(R.id.widget_root, "setBackgroundColor", finalColor)

            // Update fetch progress bar visibility and value
            val progVis = WidgetSettings.getProgressVisibility(context, appWidgetId)
            val isControlsVisible = controlsVisible[appWidgetId] ?: false
            val shouldShowProgress = if (progVis == "on_tap") isControlsVisible else true

            root.setViewVisibility(R.id.fetch_progress, if (shouldShowProgress) View.VISIBLE else View.GONE)

            val hasError = fetchError[appWidgetId] ?: false
            if (hasError) {
                root.setProgressBar(R.id.fetch_progress, 1000, 1000, false)
            } else {
                val next = nextFetchTime[appWidgetId] ?: 0L
                val last = lastFetchTime[appWidgetId] ?: 0L
                if (next > last) {
                    val total = next - last
                    val elapsed = System.currentTimeMillis() - last
                    val progress = (elapsed.toDouble() / total * 1000).toInt().coerceIn(0, 1000)
                    root.setProgressBar(R.id.fetch_progress, 1000, progress, false)
                } else {
                    root.setProgressBar(R.id.fetch_progress, 1000, 0, false)
                }
            }

            val rowsArray = jsonObject.getJSONArray("rows")
            val baseFontSize = WidgetSettings.getFontSize(context, appWidgetId)

            val currentTimeSeconds = System.currentTimeMillis() / 1000

            // Re-apply control setup (I moved it during the previous edit, putting it back)
            setupControls(context, root, appWidgetId)

            root.removeAllViews(R.id.widget_container)

            for (i in 0 until rowsArray.length()) {
                val rowJson = rowsArray.getJSONObject(i)
                val colsArray = rowJson.getJSONArray("cols")
                val rowView = RemoteViews(context.packageName, R.layout.widget_row)

                for (j in 0 until colsArray.length()) {
                    val colJson = colsArray.getJSONObject(j)
                    val type = colJson.optString("type")

                    if (type == "v-separator") {
                        val sepView = RemoteViews(context.packageName, R.layout.widget_v_separator)
                        val sepColor = colJson.optString("color", "#CCCCCC")
                        sepView.setInt(R.id.separator_line, "setBackgroundColor", ColorUtils.parseColor(sepColor))
                        rowView.addView(R.id.row_container, sepView)
                        continue
                    }

                    val weight = colJson.optString("weight", "12")
                    val layoutName = "widget_col_$weight"
                    val layoutId = context.resources.getIdentifier(layoutName, "layout", context.packageName)

                    val cellView = if (layoutId != 0) {
                        RemoteViews(context.packageName, layoutId)
                    } else {
                        RemoteViews(context.packageName, R.layout.widget_col_12)
                    }

                    // Apply font size
                    cellView.setTextViewTextSize(R.id.item_text, android.util.TypedValue.COMPLEX_UNIT_SP, baseFontSize.toFloat())

                    if (colJson.optString("type") == "countdown") {
                        val target = colJson.getLong("target_timestamp")
                        val diff = target - currentTimeSeconds
                        val text = if (diff > 0) {
                            val days = diff / 86400
                            val hours = (diff % 86400) / 3600
                            val minutes = (diff % 3600) / 60
                            val seconds = diff % 60
                            if (days != 0L){
                                String.format(Locale.US, "%dd %02d:%02d:%02d", days, hours, minutes, seconds)
                            } else if (hours != 0L) {
                                String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                            } else {
                                String.format(Locale.US, "%02d:%02d", minutes, seconds)
                            }
                        } else {
                            "Happy New Year!"
                        }
                        cellView.setTextViewText(R.id.item_text, text)
                    } else {
                        cellView.setTextViewText(R.id.item_text, colJson.optString("text", ""))
                    }

                    val colorStr = colJson.optString("color", "#000000")
                    cellView.setTextColor(R.id.item_text, ColorUtils.parseColor(colorStr))

                    val align = colJson.optString("align", "left")
                    val gravity = when (align.lowercase()) {
                        "right" -> Gravity.END or Gravity.CENTER_VERTICAL
                        "center" -> Gravity.CENTER
                        else -> Gravity.START or Gravity.CENTER_VERTICAL
                    }
                    cellView.setInt(R.id.item_text, "setGravity", gravity)

                    rowView.addView(R.id.row_container, cellView)
                }
                root.addView(R.id.widget_container, rowView)
            }
        } catch (e: Exception) {
            Log.e("UpdateService", "Error parsing JSON", e)
            // Show error on widget
            val errorView = RemoteViews(context.packageName, R.layout.widget_col_12)
            errorView.setTextViewText(R.id.item_text, "Error: ${e.message}")
            root.addView(R.id.widget_container, errorView)
        }

        appWidgetManager.updateAppWidget(appWidgetId, root)
    }

    private fun setupControls(context: android.content.Context, root: RemoteViews, appWidgetId: Int) {
        // Setup toggle click on root
        val toggleIntent = Intent(context, UpdateService::class.java).apply {
            action = ACTION_TOGGLE_CONTROLS
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val togglePendingIntent = android.app.PendingIntent.getService(
            context, appWidgetId, toggleIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        root.setOnClickPendingIntent(R.id.widget_root, togglePendingIntent)

        // Setup refresh click
        val refreshIntent = Intent(context, UpdateService::class.java).apply {
            action = ACTION_FORCE_REFRESH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val refreshPendingIntent = android.app.PendingIntent.getService(
            context, appWidgetId + 10000, refreshIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        root.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)

        // Setup setup click
        val setupIntent = Intent(context, WidgetConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val setupPendingIntent = android.app.PendingIntent.getActivity(
            context, appWidgetId + 20000, setupIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        root.setOnClickPendingIntent(R.id.btn_setup, setupPendingIntent)

        // Control visibility
        val isVisible = controlsVisible[appWidgetId] ?: false
        root.setViewVisibility(R.id.widget_controls, if (isVisible) View.VISIBLE else View.GONE)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Widget Update Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("DataWidget is running")
            .setContentText("Updating widgets...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use default icon
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        updateAllWidgetsWithStatus("Update service stopped.")
    }

    private fun updateAllWidgetsWithStatus(status: String) {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        for (id in widgetIds) {
            val root = RemoteViews(packageName, R.layout.widget_main)
            root.removeAllViews(R.id.widget_container)
            val statusView = RemoteViews(packageName, R.layout.widget_col_12)
            statusView.setTextViewText(R.id.item_text, status)
            root.addView(R.id.widget_container, statusView)
            appWidgetManager.updateAppWidget(id, root)
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGETS = "name.xoid.datawidget.ACTION_UPDATE_WIDGETS"
        const val ACTION_REMOVE_WIDGETS = "name.xoid.datawidget.ACTION_REMOVE_WIDGETS"
        const val ACTION_TOGGLE_CONTROLS = "name.xoid.datawidget.ACTION_TOGGLE_CONTROLS"
        const val ACTION_FORCE_REFRESH = "name.xoid.datawidget.ACTION_FORCE_REFRESH"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "update_service_channel"
    }
}
