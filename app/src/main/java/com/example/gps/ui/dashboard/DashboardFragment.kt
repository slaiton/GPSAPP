package com.example.gps.ui.dashboard

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.gps.databinding.FragmentDashboardBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.example.gps.LocationResponse
import com.example.gps.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.gps.R
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MapStyleOptions
import kotlinx.coroutines.launch

class DashboardFragment : Fragment(R.layout.fragment_dashboard), OnMapReadyCallback {

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var googleMap: GoogleMap
    private lateinit var sharedPreferences: SharedPreferences

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPreferences = requireContext().getSharedPreferences("LocationPreferences", Context.MODE_PRIVATE)

        val storedCode = sharedPreferences.getString("savedCode", null)
        val userId = storedCode ?: "N/A"


        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        viewModel.fetchMapData(userId)

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                viewModel.updateCurrentLocation(LatLng(location.latitude, location.longitude))
            } else {
                Toast.makeText(context, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Error al obtener la ubicación", Toast.LENGTH_SHORT).show()
        }

    }

    fun loadMapStyle(context: Context): MapStyleOptions? {
        return try {
            // Cargar el archivo JSON de estilo
            val mapStyle = context.resources.openRawResource(R.raw.map_style)
            val style = String(mapStyle.readBytes()) // Convertir el archivo JSON a una cadena
            MapStyleOptions(style) // Retornar el estilo como MapStyleOptions
        } catch (e: Exception) {
            Log.e("GoogleMapComposable", "Error al cargar el estilo del mapa: ${e.message}", e)
            null
        }
    }



    private fun createCustomMarker(context: Context, title: String): BitmapDescriptor? {
        val markerView = LayoutInflater.from(context).inflate(R.layout.custom_marker, null)

        // Establecer los datos en el marcador

        val markerImage = markerView.findViewById<ImageView>(R.id.marker_image)
        markerImage.setImageResource(R.drawable.marker) // Imagen personalizada

        // Renderizar la vista en un bitmap
        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)
        val bitmap = Bitmap.createBitmap(markerView.measuredWidth, markerView.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        markerView.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }



    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        Log.d("Maps",viewModel.mapData.value.toString())
        Log.e("GoogleMapComposable", "Error al interactuar con GoogleMap:")
        val style = loadMapStyle(requireContext())
        if (style != null) {
            googleMap.setMapStyle(style)
        }
        lifecycleScope.launch {
            viewModel.mapData.value.forEach { location ->
                val latLng = LatLng(location.lat, location.lon)
                val markerIcon = createCustomMarker(requireContext()   , "Ubicación (${location.lat}, ${location.lon})")

                googleMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("Última vez: ${location.fechaHora}")
                        .icon(markerIcon)
                )
            }

            viewModel.currentLocation.value?.let {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 20f))
            } ?: run {
                viewModel.mapData.value.firstOrNull()?.let { location ->
                    googleMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(location.lat, location.lon), 10f)
                    )
                }
            }
        }
    }



}