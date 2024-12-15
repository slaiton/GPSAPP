package com.example.gps


import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
data class LocationData1(
    val codigo_usuario: String?,
    val lat: Double,
    val lon: Double
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

object RetrofitClient {

    interface SendLocation {
        @POST("ubicaciones")
        fun sendLocation(@Body locationData: LocationData1): Call<Void>

        @GET("ubicaciones")
        suspend fun getLocation(
            @Query("codigo") codigo: String,
            @Query("fecha") fecha: String
        ): List<LocationResponse>


        @GET("ubicaciones/latest")
        suspend fun getLastLocation(
            @Query("codigo") codigo: String,
        ): List<LocationResponse>
    }

    private const val BASE_URL = "https://api.3slogistica.com/api/"

    val apiService: SendLocation by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SendLocation::class.java)
    }
}