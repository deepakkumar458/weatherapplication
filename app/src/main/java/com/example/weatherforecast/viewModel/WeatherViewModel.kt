package com.example.weatherforecast.viewModel

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherforecast.data.WeatherCurrentResponse
import com.example.weatherforecast.repository.WeatherRepository
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {
    private val _currentWeather = MutableLiveData<WeatherCurrentResponse>()
    val currentWeather: LiveData<WeatherCurrentResponse> get() = _currentWeather

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun fetchCurrentWeatherData(apiKey: String, location: String, aqi: String = "no") {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val weatherResponse = repository.getCurrentWeather(apiKey, location, aqi)
                _currentWeather.value = weatherResponse
                _isLoading.value = false
                Log.e(TAG, "onResponse: " + weatherResponse.current.cloud)
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = e.message
            }
        }
    }
}
