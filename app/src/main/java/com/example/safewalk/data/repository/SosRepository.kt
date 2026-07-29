package com.example.safewalk.data.repository

import com.example.safewalk.data.local.LocationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SosRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val locationHelper: LocationHelper
) {
    suspend fun triggerSos(source: String = "manual"): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val loc = locationHelper.getCurrentLocation() ?: return false

        val alert = hashMapOf(
            "lat" to loc.latitude,
            "lng" to loc.longitude,
            "timestamp" to FieldValue.serverTimestamp(),
            "status" to "active",
            "triggeredBy" to source
        )
        return try {
            firestore.collection("users").document(uid)
                .collection("alerts").add(alert).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
