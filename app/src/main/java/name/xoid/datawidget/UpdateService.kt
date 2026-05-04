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
import android.util.Log

class UpdateService : Service() {

    private var timer: Timer? = null
    private var widgetIds = mutableSetOf<Int>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        startTimer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE_WIDGETS -> {
                val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ids?.let { widgetIds.addAll(it.toTypedArray()) }
            }
            ACTION_REMOVE_WIDGETS -> {
                val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ids?.let { it.forEach { id -> widgetIds.remove(id) } }
                if (widgetIds.isEmpty()) stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startTimer() {
        timer?.cancel()
        timer = Timer()
        // Use schedule instead of scheduleAtFixedRate for better reliability on Android
        timer?.schedule(object : TimerTask() {
            override fun run() {
                updateAllWidgets()
            }
        }, 0, 1000) // Update every 1 second
    }

    private fun updateAllWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        for (id in widgetIds) {
            updateWidget(this, appWidgetManager, id)
        }
    }

    private fun updateWidget(context: android.content.Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val root = RemoteViews(context.packageName, R.layout.widget_main)
        root.removeAllViews(R.id.widget_container)

        // Mocking JSON data for different widgets
        val rows = if (appWidgetId % 2 == 0) {
            // Widget A: One row with 3 columns
            listOf(listOf("4", "4", "4"))
        } else {
            // Widget B: Two rows
            listOf(listOf("6", "6"), listOf("12"))
        }

        val currentTime = System.currentTimeMillis() / 1000
        val remaining = 60 - (currentTime % 60) // Simple countdown to the next minute

        for (rowCols in rows) {
            val rowView = RemoteViews(context.packageName, R.layout.widget_row)
            for (colWeight in rowCols) {
                val cellLayout = when (colWeight) {
                    "6" -> R.layout.widget_col_6
                    "4" -> R.layout.widget_col_4
                    else -> R.layout.widget_col_12
                }
                val cellView = RemoteViews(context.packageName, cellLayout)
                
                // If it's the last column of the first row, show the countdown
                if (colWeight == rowCols.last() && rowCols == rows.first()) {
                    cellView.setTextViewText(R.id.item_text, "T-minus: $remaining")
                    cellView.setTextColor(R.id.item_text, android.graphics.Color.RED)
                } else {
                    cellView.setTextViewText(R.id.item_text, "ID:$appWidgetId col-$colWeight")
                }
                
                rowView.addView(R.id.row_container, cellView)
            }
            root.addView(R.id.widget_container, rowView)
        }

        appWidgetManager.updateAppWidget(appWidgetId, root)
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
    }

    companion object {
        const val ACTION_UPDATE_WIDGETS = "name.xoid.datawidget.ACTION_UPDATE_WIDGETS"
        const val ACTION_REMOVE_WIDGETS = "name.xoid.datawidget.ACTION_REMOVE_WIDGETS"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "update_service_channel"
    }
}
