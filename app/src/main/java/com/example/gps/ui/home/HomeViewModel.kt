package com.example.gps.ui.home

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.location.LocationManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.gps.LocationService

class HomeViewModel : ViewModel() {

    private val _isSendingLocation = MutableLiveData(false)
    val isSendingLocation: LiveData<Boolean> get() = _isSendingLocation

    fun toggleLocationUpdates(sharedPreferences: SharedPreferences, activity: Activity, enable: Boolean) {

        val editor = sharedPreferences.edit()
        editor.putBoolean("status", enable)
        editor.apply()

        if (enable) {
            LocationService.startService(activity)
        } else {
            LocationService.stopService(activity)
        }
    }

    // Comprueba si el GPS está habilitado
    fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun startLocationUpdates(activity: Activity) {
        // Lógica para iniciar el envío de ubicación
    }

    private fun stopLocationUpdates(activity: Activity) {
        // Lógica para detener el envío de ubicación
    }
}