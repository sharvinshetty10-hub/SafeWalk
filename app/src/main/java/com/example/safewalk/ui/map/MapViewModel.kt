package com.example.safewalk.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.data.local.LocationHelper
import com.example.safewalk.data.repository.PlacesRepository
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationHelper: LocationHelper,
    private val placesRepository: PlacesRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation

    private val _policeStations = MutableStateFlow<List<Pair<String, LatLng>>>(emptyList())
    val policeStations: StateFlow<List<Pair<String, LatLng>>> = _policeStations

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Live Alert Tracking
    private val _trackedLocation = MutableStateFlow<LatLng?>(null)
    val trackedLocation: StateFlow<LatLng?> = _trackedLocation

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking

    private var alertListener: ListenerRegistration? = null

    fun fetchUserLocation() {
        _isLoading.value = true
        viewModelScope.launch {
            val location = locationHelper.getCurrentLocation()
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                _userLocation.value = userLatLng
                searchNearbyPolice(userLatLng)
            } else {
                _isLoading.value = false
            }
        }
    }

    private fun searchNearbyPolice(latLng: LatLng) {
        viewModelScope.launch {
            val stations = placesRepository.findNearbyPoliceStations(latLng.latitude, latLng.longitude)
            _policeStations.value = stations
            _isLoading.value = false
        }
    }

    fun startTrackingAlert(friendUid: String, alertId: String) {
        alertListener?.remove()
        _isTracking.value = true
        alertListener = firestore.collection("users").document(friendUid)
            .collection("alerts").document(alertId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    _isTracking.value = false
                    _trackedLocation.value = null
                    return@addSnapshotListener
                }
                val lat = snapshot.getDouble("lat")
                val lng = snapshot.getDouble("lng")
                if (lat != null && lng != null) {
                    _trackedLocation.value = LatLng(lat, lng)
                }
            }
    }

    fun stopTrackingAlert() {
        alertListener?.remove()
        alertListener = null
        _trackedLocation.value = null
        _isTracking.value = false
    }

    override fun onCleared() {
        alertListener?.remove()
        super.onCleared()
    }
}
