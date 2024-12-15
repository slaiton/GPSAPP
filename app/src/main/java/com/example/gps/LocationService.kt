package com.example.gps

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.os.*
import java.util.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Build
import android.os.IBinder

public class LocationService : Service() {
    private lateinit var notificationManager: NotificationManager
    private val fusedLocationProviderClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private var locationCallback: LocationCallback? = null
    private var sendLocationUpdates = false // Controla el envío de la ubicación
    private val locationHandler = Handler(Looper.getMainLooper())
    private lateinit var sharedPreferences: SharedPreferences
    private val locationUpdateRunnable = object : Runnable {
        override fun run() {
            if (sendLocationUpdates) {
                requestCurrentLocation()
                locationHandler.postDelayed(this, 5000) // Repite cada 5 segundos
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("LocationPreferences", Context.MODE_PRIVATE)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        locationHandler.removeCallbacks(locationUpdateRunnable)
        fusedLocationProviderClient.removeLocationUpdates(locationCallback as LocationCallback)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!checkLocationPermissions() || !isGpsEnabled()) {
            Log.e("APPGPS", "Permisos o GPS no habilitados. Deteniendo servicio.")
            stopSelf()
            return START_NOT_STICKY
        }

        startLocationUpdates()
        makeForeground()
        return START_STICKY
    }

    private fun checkLocationPermissions(): Boolean {
        val fineLocationPermission = android.Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocationPermission = android.Manifest.permission.ACCESS_COARSE_LOCATION
        return checkSelfPermission(fineLocationPermission) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(coarseLocationPermission) == PackageManager.PERMISSION_GRANTED
    }

    private fun isGpsEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    @SuppressLint("ForegroundServiceType")
    private fun makeForeground() {
        try {
            createServiceNotificationChannel()

            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("APP GPS")
                .setContentText("Esta compartiendo su ubicacion..")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Usa el tipo de servicio de ubicación
                startForeground(ONGOING_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(ONGOING_NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("NotificationError", "Error al cargar el ícono: ${e.message}")
        }

    }


    private fun createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ubicacion GPS",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val ONGOING_NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "1001"

        fun startService(context: Context) {
            if(isGpsEnabled(context))
            {
                val intent = Intent(context, LocationService::class.java)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    context.startService(intent)
                } else {
                    context.startForegroundService(intent)
                }
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationService::class.java)
            context.stopService(intent)
        }
    }


    private fun startLocationUpdates() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    Log.d("APPGPS", "Ubicación actualizada: Latitud = ${location.latitude}, Longitud = ${location.longitude}")
                    sendLocationToApi(location.latitude, location.longitude)
                }
            }
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000 // Intervalo de actualización en milisegundos
        ).apply {
            setMinUpdateIntervalMillis(2000) // Intervalo mínimo entre actualizaciones
        }.build()

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback as LocationCallback,
            Looper.getMainLooper()
        )

        locationHandler.post(locationUpdateRunnable)
    }

    private fun requestCurrentLocation() {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null && sendLocationUpdates) {
                Log.d("APPGPS", "Ubicación obtenida: Latitud = ${location.latitude}, Longitud = ${location.longitude}")
                sendLocationToApi(location.latitude, location.longitude)
            }
        }
    }

    fun setSendLocationUpdates(enabled: Boolean) {
        sendLocationUpdates = enabled
    }



    fun sendLocationToApi(lat: Double, lon: Double) {
        try {
            val codigoUsuario = sharedPreferences.getString("savedCode", "12345");
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val fechaHora = sdf.format(Date())

            val locationData = LocationData1(
                codigo_usuario = codigoUsuario,
                lat = lat,
                lon = lon
            )

            Log.d("APPAPI", "Iniciando el envío de la ubicación al API")

            RetrofitClient.apiService.sendLocation(locationData).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Log.d("APPAPI", "Ubicación enviada con éxito al API")
                    } else {
                        Log.e("APPAPI", "Error al enviar ubicación: Código de respuesta ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e("APPAPI", "Fallo al enviar ubicación: ${t.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("ERRAPP", "Error en sendLocationToApi: ${e.message}")
        }
    }
}