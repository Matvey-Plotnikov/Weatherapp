package com.example.weatherapp

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class WeatherWidget : AppWidgetProvider() {

    private val apiKey = "77550d1824e1bf34a0a8616d77ae8944"

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        for (widgetId in appWidgetIds) {

            val views = RemoteViews(context.packageName, R.layout.widget_weather)

            val intent = Intent(context, WeatherWidget::class.java)
            intent.action = "UPDATE_WEATHER"

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.widgetUpdate, pendingIntent)

            updateWeather(context, views, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == "UPDATE_WEATHER") {

            val manager = AppWidgetManager.getInstance(context)
            val component = android.content.ComponentName(context, WeatherWidget::class.java)
            val ids = manager.getAppWidgetIds(component)

            for (id in ids) {
                val views = RemoteViews(context.packageName, R.layout.widget_weather)
                updateWeather(context, views, manager, id)
            }
        }
    }

    private fun updateWeather(
        context: Context,
        views: RemoteViews,
        manager: AppWidgetManager,
        widgetId: Int
    ) {

        thread {

            try {

                val locationManager =
                    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    views.setTextViewText(R.id.widgetCity, "Нет доступа к GPS")
                    manager.updateAppWidget(widgetId, views)
                    return@thread
                }

                val location =
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

                if (location == null) {
                    views.setTextViewText(R.id.widgetCity, "Локация недоступна")
                    manager.updateAppWidget(widgetId, views)
                    return@thread
                }

                val lat = location.latitude
                val lon = location.longitude

                val url =
                    "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric&lang=ru"

                val connection = URL(url).openConnection() as HttpURLConnection
                val response = connection.inputStream.bufferedReader().readText()

                val json = JSONObject(response)

                val city = json.getString("name")
                val temp = json.getJSONObject("main").getDouble("temp")

                views.setTextViewText(R.id.widgetCity, city)
                views.setTextViewText(R.id.widgetTemp, "${temp.toInt()}°C")

                manager.updateAppWidget(widgetId, views)

            } catch (e: Exception) {

                views.setTextViewText(R.id.widgetTemp, "Ошибка")
                manager.updateAppWidget(widgetId, views)
            }

        }
    }
}