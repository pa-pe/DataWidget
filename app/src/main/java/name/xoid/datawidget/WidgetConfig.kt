package name.xoid.datawidget

data class WidgetConfig(
    var name: String,
    var url: String,
    var bgColor: String = "#FFFFFF",
    var bgAlpha: Float = AppConfig.DEFAULT_BG_ALPHA,
    var updateOnlyScreenOn: Boolean = true,
    var progressVisibility: String = "always",
    var requestType: String = "GET",
    var baseFontSize: Int = 12,
    var id: String = System.currentTimeMillis().toString() + (Math.random() * 1000).toInt()
)
