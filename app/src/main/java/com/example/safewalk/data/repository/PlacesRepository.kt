package com.example.safewalk.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject

class PlacesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient()

    private val apiKey: String by lazy {
        try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            appInfo.metaData.getString("com.google.android.geo.API_KEY") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun findNearbyPoliceStations(lat: Double, lng: Double): List<Pair<String, LatLng>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext emptyList()

        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=$lat,$lng" +
                "&radius=3000" +
                "&type=police" +
                "&key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val responseBody = response.body?.string() ?: return@withContext emptyList()
                
                val jsonObject = JSONObject(responseBody)
                val resultsArray = jsonObject.getJSONArray("results")
                val stationsList = mutableListOf<Pair<String, LatLng>>()
                
                for (i in 0 until resultsArray.length()) {
                    val result = resultsArray.getJSONObject(i)
                    val name = result.getString("name")
                    val geometry = result.getJSONObject("geometry")
                    val location = geometry.getJSONObject("location")
                    val stationLat = location.getDouble("lat")
                    val stationLng = location.getDouble("lng")
                    
                    stationsList.add(Pair(name, LatLng(stationLat, stationLng)))
                }
                stationsList
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
