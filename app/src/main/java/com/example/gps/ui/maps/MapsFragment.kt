package com.example.gps.ui.maps

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Shader
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.os.Handler
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
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
import com.bumptech.glide.Glide
import com.example.gps.LocationResponse
import com.example.gps.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.gps.R
import com.example.gps.ui.dashboard.DashboardViewModel
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MapStyleOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MapsFragment : Fragment(R.layout.fragment_dashboard), OnMapReadyCallback {

    private val viewModel: MapsViewModel by viewModels()
    private var googleMap: GoogleMap? = null
    private lateinit var sharedPreferences: SharedPreferences
    private val handler = Handler()
    private val refreshInterval = 5000L

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPreferences =
            requireContext().getSharedPreferences("LocationPreferences", Context.MODE_PRIVATE)

        val storedCode = sharedPreferences.getString("savedCode", null)
        val userId = storedCode ?: "N/A"


        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        lifecycleScope.launch {
            viewModel.fetchMapData(userId)
            updateMap()
        }


        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                viewModel.updateCurrentLocation(LatLng(location.latitude, location.longitude))
            } else {
                Toast.makeText(
                    context,
                    "No se pudo obtener la ubicación actual",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Error al obtener la ubicación", Toast.LENGTH_SHORT).show()
        }

        //viewModel.fetchMapData(userId)

        lifecycleScope.launch {
            while (true) {
                if (googleMap != null) {
                    viewModel.fetchMapData(userId)
                    updateMap()
                }
                delay(2000)
            }
        }

    }


    private fun startRefreshingMapData() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                val storedCode = sharedPreferences.getString("savedCode", null)
                val userId = storedCode ?: "N/A"
                viewModel.fetchMapData(userId)
                // Programar la siguiente ejecución
                handler.postDelayed(this, refreshInterval)
            }
        }, refreshInterval)
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

/*
    private fun createCustomMarker(context: Context, title: String): BitmapDescriptor? {
        val markerView = LayoutInflater.from(context).inflate(R.layout.custom_marker, null)

        // Establecer los datos en el marcador

        sharedPreferences =
            requireContext().getSharedPreferences("LocationPreferences", Context.MODE_PRIVATE)


        val markerImage = markerView.findViewById<ImageView>(R.id.marker_image)
        markerImage.setImageResource(R.drawable.marker_map)

        // Renderizar la vista en un bitmap
        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)
        val bitmap = Bitmap.createBitmap(
            markerView.measuredWidth,
            markerView.measuredHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        markerView.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }




    suspend fun createCustomMarker(context: Context, imageUrl: String): BitmapDescriptor? {
        return withContext(Dispatchers.Default) {
            val drawable = ContextCompat.getDrawable(context, R.drawable.marker_map)
            val size = 80

            if (drawable != null) {
                val sizePx = (size * context.resources.displayMetrics.density).toInt()

                val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)

                val canvas = Canvas(bitmap)

                drawable.setBounds(0, 0, sizePx, sizePx)

                drawable.draw(canvas)

                addProfileImage(context, canvas, sizePx, imageUrl)

                return@withContext BitmapDescriptorFactory.fromBitmap(bitmap)
            } else {
                null
            }
        }
    }

 */


    suspend fun createCustomMarker(
        context: Context,
        imageUrl: String,
        drawableSize: Int,
        borderWidth: Float = 5f
    ): BitmapDescriptor? {
        return withContext(Dispatchers.IO) {
            try {
                // Descargar la imagen desde la URL como un Bitmap
                val sourceBitmap = Glide.with(context)
                    .asBitmap()
                    .load(imageUrl)
                    .submit()
                    .get()

                // Redimensionar la imagen al tamaño del drawable
                val resizedBitmap = Bitmap.createScaledBitmap(
                    sourceBitmap,
                    drawableSize,
                    drawableSize,
                    true
                )

                // Crear un Bitmap circular
                val circularBitmap = Bitmap.createBitmap(
                    drawableSize,
                    drawableSize,
                    Bitmap.Config.ARGB_8888
                )

                // Crear un Canvas para dibujar la imagen circular
                val canvas = Canvas(circularBitmap)

                // Configurar el Paint para la imagen circular (con shader)
                val paintImage = Paint().apply {
                    isAntiAlias = true // Suavizar bordes
                    shader = BitmapShader(
                        resizedBitmap,
                        Shader.TileMode.CLAMP,
                        Shader.TileMode.CLAMP
                    )
                }

                val radius = drawableSize / 2f
                canvas.drawCircle(radius, radius, radius, paintImage)

                // Configurar el Paint para el borde
                val paintBorder = Paint().apply {
                    isAntiAlias = true
                    color = Color.RED
                    style = Paint.Style.STROKE
                    strokeWidth = borderWidth
                }

                canvas.drawCircle(radius, radius, radius - borderWidth / 2, paintBorder)

                BitmapDescriptorFactory.fromBitmap(circularBitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }


    private suspend fun updateMap() {
        googleMap?.let { map ->
            withContext(Dispatchers.Main) {
            googleMap?.clear()
            lifecycleScope.launch {
                viewModel.mapData.value.forEach { location ->
                    val latLng = LatLng(location.lat, location.lon)
                    val savedPhotoUri = sharedPreferences.getString("photoUri", null)
                    val drawableSize = 130
                    val markerIcon = createCustomMarker(
                        requireContext(),
                        savedPhotoUri.toString(),
                        drawableSize
                    )
                    googleMap?.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title("Última vez: ${location.fechaHora}")
                            .icon(markerIcon)
                    )
                }

            }
            }
        } ?: run {
            Log.w("MapsFragment", "googleMap no está inicializado en updateMap()")
        }
    }


    override fun onMapReady(map: GoogleMap) {
        sharedPreferences = requireContext().getSharedPreferences("LocationPreferences", Context.MODE_PRIVATE)
        googleMap = map
        val style = loadMapStyle(requireContext())
        if (style != null) {
            googleMap?.setMapStyle(style)
        }
        lifecycleScope.launch {
            updateMap()
            delay(300)
            viewModel.currentLocation.value?.let {
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 18f))
            } ?: run {
                viewModel.mapData.value.firstOrNull()?.let { location ->
                    googleMap?.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(location.lat, location.lon), 10f)
                    )
                }
            }
        }
    }

    private suspend fun addProfileImage(context: Context, canvas: Canvas, sizePx: Int, imageUrl: String) {
        val profileSize = (sizePx * 0.5).toInt()

        val bitmap = withContext(Dispatchers.IO) {
            Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .submit()
                .get()
        }

        val circularBitmap = createCircularBitmap(bitmap, profileSize)

        val left = (sizePx - profileSize) / 2
        val top = (sizePx - profileSize) / 2
        canvas.drawBitmap(circularBitmap, left.toFloat(), top.toFloat(), null)
    }

    private fun createCircularBitmap(source: Bitmap, size: Int): Bitmap {
        val scaledBitmap = Bitmap.createScaledBitmap(source, size, size, false)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            shader = BitmapShader(scaledBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }

        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        return output
    }

    fun resizeBitmap(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

}



