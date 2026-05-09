package name.xoid.datawidget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PinnedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        Log.d("PinnedReceiver", "Widget pinned successfully! ID: $appWidgetId")

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val pending = PendingPinConfig.config
            if (pending != null) {
                Log.d("PinnedReceiver", "Linking widget $appWidgetId to config ID ${pending.id}")
                
                // Establish the link
                WidgetSettings.saveConfigId(context, appWidgetId, pending.id)
                
                // Fallback: Also save actual values in case config is deleted from library later
                WidgetSettings.saveUrl(context, appWidgetId, pending.url)
                WidgetSettings.saveName(context, appWidgetId, pending.name)
                WidgetSettings.saveSettings(
                    context, appWidgetId, 
                    ColorUtils.parseColor(pending.bgColor), 
                    pending.bgAlpha, 
                    pending.updateOnlyScreenOn, 
                    pending.progressVisibility, 
                    pending.requestType, 
                    pending.baseFontSize
                )

                // Trigger immediate update
                val serviceIntent = Intent(context, UpdateService::class.java).apply {
                    action = UpdateService.ACTION_UPDATE_WIDGETS
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
                }
                context.startForegroundService(serviceIntent)

                // Clear bridge only here if we are NOT using the ConfigActivity
                // But since we want the bridge available for Activity too, we keep it for now.
            }
        }
    }
}
