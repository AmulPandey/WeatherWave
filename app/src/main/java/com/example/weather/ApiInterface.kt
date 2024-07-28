package com.example.weather

import com.example.weather.DataClasses.ForecastResponse
import com.example.weather.DataClasses.weatherApp
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {
    @GET("weather")
    fun getweatherdata(
        @Query("q") city:String,
        @Query("appid") appid:String,
        @Query("units") units:String
    ) : Call<weatherApp>

    @GET("forecast")
    fun getForecastData(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String): Call<ForecastResponse>


}


