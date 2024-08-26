package com.example.weatherforecast.repository

import com.example.weatherforecast.api.WeatherApiService

class WeatherRepository(private val weatherApiService: WeatherApiService) {

    suspend fun getCurrentWeather(apiKey: String, location: String,  aqi: String) =
        weatherApiService.getCurrentWeather(apiKey, location, aqi)
}
