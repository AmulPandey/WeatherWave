package com.example.weather
import android.annotation.SuppressLint
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.weather.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.view.WindowManager.InvalidDisplayException
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LastLocationRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.tasks.Task
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy{
        ActivityMainBinding.inflate(layoutInflater)
    }
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fatchweatherdata("cityname")

        searchcity()
        fatchLoaction()

    }

    private fun fatchLoaction() {

        val task : Task<Location> = fusedLocationProviderClient.lastLocation

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ){
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),101)
            return
        }

        task.addOnSuccessListener {
            if(it != null){
                //Toast.makeText(applicationContext,"${it.latitude} ${it.longitude}",Toast.LENGTH_SHORT).show()
                val latitude = it.latitude
                val longitude = it.longitude
               // fetchWeatherForLocation(latitude, longitude)
               var city =  getCity(latitude, longitude)
                if(city == "Kora Jahanabad")
                    city = "Jahanabad"
                fatchweatherdata(city)

            }
        }


    }


    private fun searchcity() {
        val searchView = binding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchView.setQuery(query, false)
                    fatchweatherdata(query)
                    searchView.clearFocus()
                    searchView.onActionViewCollapsed()
                } else {
                    fatchLoaction()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }


    private fun fatchweatherdata(cityname: String) {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .build().create(ApiInterface :: class.java)

        val response = retrofit.getweatherdata(cityname, "114541bee5935fe60123f08b59232cbb", "metric")

        response.enqueue(object : Callback<weatherApp> {
            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call<weatherApp>, response: Response<weatherApp>) {
                val responseBody = response.body()
                if (response.isSuccessful && responseBody != null) {
                    val tempreture = responseBody.main.temp.toString()
                    val weather = responseBody.weather.firstOrNull()?.main ?: "unknown"
                    val mintemp = responseBody.main.temp_min.toString()
                    val maxtemp = responseBody.main.temp_max.toString()
                    val wind = responseBody.wind.speed.toString()
                    val cloud = responseBody.clouds.all.toString()
                    val humi = responseBody.main.humidity.toString()

                   // Log.d("TAG1", "onResponse: $mintemp")
                   // Log.d("TAG2", "onResponse: $maxtemp")

                    binding.avgtemp.text = "$tempreture°c"
                    binding.weather.text = weather
                    binding.min.text = "$mintemp"
                    binding.max.text = "$maxtemp"
                    binding.wind1.text = "$wind m/s"
                    binding.cloud1.text = "$cloud"
                    binding.humidity1.text = "$humi"
                    binding.city.text = cityname

                   changeAnimation(weather)
                }
            }

            override fun onFailure(call: Call<weatherApp>, t: Throwable) {
                Log.d("RetrofitError", "API call failed: ${t.message}")
            }
        })
    }

    private fun changeAnimation(weather:String) {
        when(weather){
            "Clear Sky","Clear"->{
                //binding.root.setBackgroundResource(R.drawable.name of image)
                binding.lottieAnimationView.setAnimation(R.raw.clear)

            }
            "Sunny"->{
                //binding.root.setBackgroundResource(R.drawable.name of image)
                binding.lottieAnimationView.setAnimation(R.raw.sunny)

            }

             "Thunderstorm"->{
                //binding.root.setBackgroundResource(R.drawable.name of image)
                binding.lottieAnimationView.setAnimation(R.raw.thunderstorm)

            }

            "Partial Clouds","Clouds","Mist","Foggy","Haze"->{
                //binding.root.setBackgroundResource(R.drawable.name of image)
                binding.lottieAnimationView.setAnimation(R.raw.cloudy)

            }

            "Light Rain","Rain","Drizzle","Moderate Rain","Showers","Heavy Rain"->{
                //binding.root.setBackgroundResource(R.drawable.name of image)
                binding.lottieAnimationView.setAnimation(R.raw.rainy)

            }
            "Light Snow","Moderate Snow","Heavy Snow","Blizzard"->{
                //binding.root.setBackgroundResource(R.drawable.name of image)
                binding.lottieAnimationView.setAnimation(R.raw.snow)

            }
            else->{
                //binding.root.setBackgroundResource(R.drawable.name of image)
                binding.lottieAnimationView.setAnimation(R.raw.def)

            }
        }
        binding.lottieAnimationView.playAnimation()

    }


    private fun getCity(latitude: Double, longitude: Double): String {
        var city = ""
        val gcd = Geocoder(this, Locale.getDefault())
        val addressess = gcd.getFromLocation(latitude, longitude, 1)
        if (addressess != null) {
            if (addressess.size > 0) {
                //Toast.makeText(this, addressess[0].locality+"nimbu", Toast.LENGTH_SHORT).show()
                city = addressess[0].locality
            }
        }
        binding.city.text = city
        return city
    }


}
