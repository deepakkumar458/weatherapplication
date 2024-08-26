package com.example.weatherforecast.activity

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.weatherforecast.api.RetrofitClient
import com.example.weatherforecast.api.WeatherApiService
import com.example.weatherforecast.databinding.ActivityMainBinding
import com.example.weatherforecast.repository.WeatherRepository
import com.example.weatherforecast.utils.Constants
import com.example.weatherforecast.viewModel.WeatherViewModel
import com.example.weatherforecast.viewModelFactory.WeatherViewModelFactory
import com.google.android.gms.location.LocationServices
import com.squareup.picasso.Picasso
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityMainBinding
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000
    private lateinit var addressFromLatLong: String
    private lateinit var mViewModel: WeatherViewModel
    private lateinit var mQuery : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.searchEt.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Handle the search action (e.g., search for the query in a list or database)
                if (query != null) {
                    // Perform search operation
                    mQuery = query
                    mViewModel.fetchCurrentWeatherData(Constants.API_KEY, mQuery, "no")
                }
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(mBinding.searchEt.windowToken, 0)

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Handle the text change
                return false
            }
        })

        mBinding.refreshTv.setOnClickListener {
            if (mQuery!=null || mQuery!=""){
                mViewModel.fetchCurrentWeatherData(Constants.API_KEY, mQuery, "no")
            }else{
                Toast.makeText(this, "No location found to search ...", Toast.LENGTH_SHORT).show()
            }

        }

        // Initialize ViewModel
        val mApi = RetrofitClient.retrofit.create(WeatherApiService::class.java)
        val mRepo = WeatherRepository(mApi)
        mViewModel = ViewModelProvider(this, WeatherViewModelFactory(mRepo)).get(WeatherViewModel::class.java)

        // Observe LiveData
        mViewModel.isLoading.observe(this, { isLoading ->
            mBinding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

        })

        mViewModel.currentWeather.observe(this, Observer { weatherResponse ->
            // Update UI with weatherResponse
            // For example:
            if (weatherResponse != null) {

                mBinding.temperatureTv.text = weatherResponse.current.temp_c.toString()
                mBinding.celciusTv.visibility  = View.VISIBLE
                mBinding.cTv.visibility  = View.VISIBLE

                mBinding.conditionDataTv.text = weatherResponse.current.condition.text
                val originalUrl = weatherResponse.current.condition.icon
                val correctedUrl = "https:${originalUrl.replace("//", "")}"
                Picasso.get()
                    .load(correctedUrl)
                    .into(mBinding.conditionIv)

                mBinding.humidityDataTV.text = weatherResponse.current.humidity.toString()
                mBinding.pressureDataTV.text = weatherResponse.current.pressure_mb.toString()
                mBinding.windDataTV.text = weatherResponse.current.wind_kph.toString()
                mBinding.addressTv.text = weatherResponse.location.name+","+weatherResponse.location.region+","+weatherResponse.location.country
            }
        })

        mViewModel.error.observe(this, Observer { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
           // mBinding.textViewError.text = errorMessage
        })
    }


    override fun onResume() {
        super.onResume()
        checkLocationPermission()
    }
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            getCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                showAlertDailog()
            }
        }
    }

    private fun showAlertDailog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Denied")
            .setMessage("Location permission is required for this app to function properly. Please grant permission in your settings or search manually any location")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
                // Optionally, guide the user to app settings
            }
            .setNegativeButton("Cancel"){dailog , _ ->
                dailog.dismiss()
            }
            .create()
            .show()
    }

    private fun getCurrentLocation() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val latitude = it.latitude
                val longitude = it.longitude
                Log.e(TAG, "getCurrentLocation: $latitude,$longitude")
                addressFromLatLong = getAddressFromLatLng(this, latitude, longitude)
                if (addressFromLatLong.isNotEmpty()) {
                    mBinding.addressTv.text = addressFromLatLong
                }
                mQuery = "$latitude,$longitude"
                // Fetch weather forecast based on lat/long
                mViewModel.fetchCurrentWeatherData(Constants.API_KEY, mQuery, "no")

            }
        }
    }

    private fun getAddressFromLatLng(context: Context, latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            Log.d("Geocoder", "Requesting address for latitude: $latitude, longitude: $longitude")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val locality = address.locality // City or town name
                val adminArea = address.adminArea

                // Combine components as needed
                val addressString = buildString {
                    if (!locality.isNullOrEmpty()) append("$locality, ")
                    if (!adminArea.isNullOrEmpty()) append("$adminArea, ")
                }.trimEnd { it == ',' || it == ' ' }

                Log.d("Geocoder", "Formatted Address: $addressString")
                addressString
            } else {
                Log.d("Geocoder", "No addresses found")
                "Address not found"
            }
        } catch (e: Exception) {
            Log.e("Geocoder", "Error getting address", e)
            "Unable to get address"
        }
    }
}
