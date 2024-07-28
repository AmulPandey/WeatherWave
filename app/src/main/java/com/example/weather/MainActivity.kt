package com.example.weather

import android.annotation.SuppressLint
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.SearchView
import androidx.core.app.ActivityCompat
import com.example.weather.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import com.example.weather.DataClasses.ForecastResponse
import com.example.weather.DataClasses.WeatherData
import com.example.weather.DataClasses.weatherApp
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.gms.tasks.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



class MainActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        lineChart = findViewById(R.id.lineChart)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fetchWeatherData("cityname")
        searchCity()
        fetchLocation()
    }



    private fun fetchLocation() {
        val task: Task<Location> = fusedLocationProviderClient.lastLocation

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 101)
            return
        }

        task.addOnSuccessListener {
            if (it != null) {
                val latitude = it.latitude
                val longitude = it.longitude
                val city = getCity(latitude, longitude)
                fetchWeatherData(city)
            }
        }
    }

    private fun searchCity() {
        val searchView = binding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchView.setQuery(query, false)
                    fetchWeatherData(query)
                    searchView.clearFocus()
                    searchView.onActionViewCollapsed()
                } else {
                    fetchLocation()
                }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun fetchWeatherData(cityname: String) {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .build().create(ApiInterface::class.java)

        // Get current weather data
        val currentWeatherResponse = retrofit.getweatherdata(cityname, "114541bee5935fe60123f08b59232cbb", "metric")
        currentWeatherResponse.enqueue(object : Callback<weatherApp> {
            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call<weatherApp>, response: Response<weatherApp>) {
                val responseBody = response.body()
                if (response.isSuccessful && responseBody != null) {
                    val tempreture = responseBody.main.temp.toString()
                    val weather = responseBody.weather.firstOrNull()?.main ?: "unknown"
                    val minTemp = responseBody.main.temp_min.toString()
                    val maxTemp = responseBody.main.temp_max.toString()
                    val wind = responseBody.wind.speed.toString()
                    val cloud = responseBody.clouds.all.toString()
                    val humidity = responseBody.main.humidity.toString()

                    binding.currentTemp.text = "$tempreture°c"
                    binding.weather.text = weather
                    binding.min.text = "$minTemp"
                    binding.max.text = "$maxTemp"
                    binding.wind1.text = "$wind m/s"
                    binding.cloud1.text = "$cloud"
                    binding.humidity1.text = "$humidity"
                    binding.city.text = cityname

                    changeAnimation(weather)
                }
            }

            override fun onFailure(call: Call<weatherApp>, t: Throwable) {
                Log.d("RetrofitError", "API call failed: ${t.message}")
            }
        })

        // Get forecast data
        val forecastResponse = retrofit.getForecastData(cityname, "114541bee5935fe60123f08b59232cbb", "metric")
        forecastResponse.enqueue(object : Callback<ForecastResponse> {
            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call<ForecastResponse>, response: Response<ForecastResponse>) {
                val responseBody = response.body()
                if (response.isSuccessful && responseBody != null) {
                    val forecastList = responseBody.list
                    showWeatherChart(forecastList)
                }
            }

            override fun onFailure(call: Call<ForecastResponse>, t: Throwable) {
                Log.d("RetrofitError", "API call failed: ${t.message}")
            }
        })
    }

    private fun changeAnimation(weather: String) {
        when (weather) {
            "Clear Sky", "Clear" -> binding.lottieAnimationView.setAnimation(R.raw.clear)
            "Sunny" -> binding.lottieAnimationView.setAnimation(R.raw.sunny)
            "Thunderstorm" -> binding.lottieAnimationView.setAnimation(R.raw.thunderstorm)
            "Partial Clouds", "Clouds", "Mist", "Foggy", "Haze" -> binding.lottieAnimationView.setAnimation(R.raw.cloudy)
            "Light Rain", "Rain", "Drizzle", "Moderate Rain", "Showers", "Heavy Rain" -> binding.lottieAnimationView.setAnimation(R.raw.rainy)
            "Light Snow", "Moderate Snow", "Heavy Snow", "Blizzard" -> binding.lottieAnimationView.setAnimation(R.raw.snow)
            else -> binding.lottieAnimationView.setAnimation(R.raw.def)
        }
        binding.lottieAnimationView.playAnimation()
    }

    private fun getCity(latitude: Double, longitude: Double): String {
        var city = ""
        val gcd = Geocoder(this, Locale.getDefault())
        val addresses = gcd.getFromLocation(latitude, longitude, 1)
        if (addresses != null && addresses.isNotEmpty()) {
            city = addresses[0].locality ?: ""
        }
        binding.city.text = city
        return city
    }


    private fun showWeatherChart(forecastList: List<WeatherData>) {
        val entries = ArrayList<Entry>()
        var sum = 0.0
        val xAxisLabels = ArrayList<String>()

        for ((index, forecast) in forecastList.withIndex()) {
            val temp = forecast.main.temp
            val date = Date(forecast.dt * 1000L) // Convert seconds to milliseconds
            entries.add(Entry(index.toFloat(), temp.toFloat()))
            sum += temp

            // Format date for X-axis label
            val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
            xAxisLabels.add(sdf.format(date))
        }

        val avgTemp = sum / forecastList.size
        val dataSet = LineDataSet(entries, "Temperature°c")
        dataSet.color = Color.RED
        dataSet.setCircleColor(Color.RED)
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setDrawValues(false)

        // Set the text color for the X and Y axis labels
        val xAxis: XAxis = lineChart.xAxis
        xAxis.textColor = Color.WHITE
        xAxis.valueFormatter = IndexAxisValueFormatter(xAxisLabels) // Set custom labels
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f // Rotate labels for better readability

        val leftAxis: YAxis = lineChart.axisLeft
        leftAxis.textColor = Color.WHITE

        val rightAxis: YAxis = lineChart.axisRight
        rightAxis.textColor = Color.WHITE

        // Set the text color for the legend
        val legend: Legend = lineChart.legend
        legend.textColor = Color.WHITE

        // Create a LimitLine for the average value
        val avgLine = LimitLine(avgTemp.toFloat(), "Avg: %.0f".format(avgTemp))
        avgLine.lineWidth = 2f
        avgLine.lineColor = Color.YELLOW
        avgLine.textColor = Color.WHITE
        avgLine.textSize = 12f

        leftAxis.addLimitLine(avgLine)

        val lineData = LineData(dataSet)
        binding.lineChart.data = lineData
        binding.lineChart.invalidate() // Refresh chart
    }

}
