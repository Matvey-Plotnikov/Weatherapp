package com.example.weatherapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val apiKey = "77550d1824e1bf34a0a8616d77ae8944" // вставь свой ключ

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnSearch = findViewById<Button>(R.id.btnSearch)
        val btnLocation = findViewById<Button>(R.id.btnLocation)
        val historyList = findViewById<ListView>(R.id.historyList)

        loadHistory(historyList)

        btnSearch.setOnClickListener {
            val city = findViewById<EditText>(R.id.editCity).text.toString().trim()

            if (city.isNotEmpty()) {
                getWeatherByCity(city)
                getForecastByCity(city)
                saveCity(city)
                loadHistory(historyList)
            } else {
                Toast.makeText(this, "Введите город", Toast.LENGTH_SHORT).show()
            }
        }

        btnLocation.setOnClickListener {
            requestLocation()
        }

        historyList.setOnItemClickListener { _, _, position, _ ->
            val city = getHistory()[position]
            getWeatherByCity(city)
            getForecastByCity(city)
        }
    }

    // =========================
    // ТЕКУЩАЯ ПОГОДА
    // =========================

    private fun getWeatherByCity(city: String) {
        thread {
            try {
                val url =
                    "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$apiKey&units=metric&lang=ru"

                val result = fetchData(url)

                runOnUiThread { updateCurrentWeatherUI(result) }

            } catch (e: Exception) {
                showError()
            }
        }
    }

    private fun getWeatherByCoordinates(lat: Double, lon: Double) {
        thread {
            try {
                val url =
                    "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric&lang=ru"

                val result = fetchData(url)

                runOnUiThread { updateCurrentWeatherUI(result) }

            } catch (e: Exception) {
                showError()
            }
        }
    }

    // =========================
    // ПРОГНОЗ НА 3 ДНЯ
    // =========================

    private fun getForecastByCity(city: String) {
        thread {
            try {
                val url =
                    "https://api.openweathermap.org/data/2.5/forecast?q=$city&appid=$apiKey&units=metric&lang=ru"

                val result = fetchData(url)

                runOnUiThread { updateForecastUI(result) }

            } catch (e: Exception) {
                showError()
            }
        }
    }

    private fun getForecastByCoordinates(lat: Double, lon: Double) {
        thread {
            try {
                val url =
                    "https://api.openweathermap.org/data/2.5/forecast?lat=$lat&lon=$lon&appid=$apiKey&units=metric&lang=ru"

                val result = fetchData(url)

                runOnUiThread { updateForecastUI(result) }

            } catch (e: Exception) {
                showError()
            }
        }
    }

    // =========================
    // СЕТЕВОЙ ЗАПРОС
    // =========================

    private fun fetchData(urlString: String): JSONObject? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val response = connection.inputStream.bufferedReader().readText()
            JSONObject(response)

        } catch (e: Exception) {
            null
        }
    }

    // =========================
    // ОБНОВЛЕНИЕ UI
    // =========================

    private fun updateCurrentWeatherUI(json: JSONObject?) {
        if (json == null) return

        val city = json.getString("name")
        val main = json.getJSONObject("main")
        val wind = json.getJSONObject("wind")
        val weather = json.getJSONArray("weather").getJSONObject(0)

        findViewById<TextView>(R.id.tvCity).text = city
        findViewById<TextView>(R.id.tvTemp).text =
            "${main.getDouble("temp")}°C"
        findViewById<TextView>(R.id.tvDesc).text =
            weather.getString("description")
        findViewById<TextView>(R.id.tvHumidity).text =
            "Влажность: ${main.getInt("humidity")}%"
        findViewById<TextView>(R.id.tvWind).text =
            "Ветер: ${wind.getDouble("speed")} м/с"
    }

    private fun updateForecastUI(json: JSONObject?) {
        if (json == null) return

        val list = json.getJSONArray("list")

        val day1 = list.getJSONObject(0)
        val day2 = list.getJSONObject(8)
        val day3 = list.getJSONObject(16)

        val forecastText = StringBuilder()
        val days = listOf(day1, day2, day3)

        for (day in days) {
            val date = day.getString("dt_txt")
            val temp = day.getJSONObject("main").getDouble("temp")
            val desc = day.getJSONArray("weather")
                .getJSONObject(0)
                .getString("description")

            forecastText.append("$date\n$temp°C, $desc\n\n")
        }

        findViewById<TextView>(R.id.tvForecast).text =
            forecastText.toString()
    }

    private fun showError() {
        runOnUiThread {
            Toast.makeText(this, "Ошибка запроса", Toast.LENGTH_SHORT).show()
        }
    }

    // =========================
    // ГЕОЛОКАЦИЯ
    // =========================

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }

        val manager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        location?.let {
            getWeatherByCoordinates(it.latitude, it.longitude)
            getForecastByCoordinates(it.latitude, it.longitude)
        }
    }

    // =========================
    // ИСТОРИЯ
    // =========================

    private fun saveCity(city: String) {
        val prefs = getSharedPreferences("history", MODE_PRIVATE)
        val list = getHistory().toMutableList()

        if (list.contains(city)) list.remove(city)

        list.add(0, city)

        if (list.size > 5) list.removeAt(list.size - 1)

        prefs.edit().putString("cities", list.joinToString(",")).apply()
    }

    private fun getHistory(): List<String> {
        val prefs = getSharedPreferences("history", MODE_PRIVATE)
        val data = prefs.getString("cities", "") ?: ""

        return if (data.isEmpty()) emptyList()
        else data.split(",")
    }

    private fun loadHistory(listView: ListView) {
        val adapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, getHistory())

        listView.adapter = adapter
    }
}