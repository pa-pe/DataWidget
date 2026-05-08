package name.xoid.datawidget

import android.graphics.Color

data class WidgetConfig(
    var name: String,
    var url: String,
    var bgColor: String = "#FFFFFF",
    var bgAlpha: Float = 1.0f
)
