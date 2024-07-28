package com.example.weather.DataClasses

data class ForecastResponse(
    val list: List<WeatherData>
)

data class WeatherData(
    val dt: Long,
    val main: Main
)


