package com.example.gps.ui.maps


import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gps.RetrofitClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class MapData(
    val lat: Double,
    val lon: Double,
    val fechaHora: String // Agregar el nuevo campo
)

data class LocationResponse(
    val id: Int,
    val codigo_usuario: String,
    val lat: String,
    val lon: String,
    val fecha_hora: String,
    val created_at: String,
    val updated_at: String
)

class MapsViewModel(application: Application) : AndroidViewModel(application) {

    val mapData = mutableStateOf<List<MapData>>(emptyList())
    val currentLocation = mutableStateOf<LatLng?>(null)


    fun fetchMapData(userId: String) {
        viewModelScope.launch {
            try {
                val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                val response = RetrofitClient.apiService.getLastLocation(userId)
                mapData.value = response.map { location ->
                    MapData(
                        lat = location.lat.toDouble(),
                        lon = location.lon.toDouble(),
                        fechaHora = location.fecha_hora
                    )
                }
            } catch (e: HttpException) {
                Log.e("DashboardViewModel", "Error en la solicitud HTTP: ${e.message}", e)
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error general: ${e.message}", e)
            }
        }
    }

    fun updateCurrentLocation(latLng: LatLng?) {
        currentLocation.value = latLng
    }
}