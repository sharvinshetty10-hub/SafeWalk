package com.example.safewalk.ui.map

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateBack: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userLocation by viewModel.userLocation.collectAsState()
    val policeStations by viewModel.policeStations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val trackedLocation by viewModel.trackedLocation.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()

    // Input fields for tracking alert
    var friendUidInput by remember { mutableStateOf("") }
    var alertIdInput by remember { mutableStateOf("") }
    var showTrackingDialog by remember { mutableStateOf(false) }

    // Aesthetic dark theme colors
    val darkBg = Color(0xFF121212)
    val cardBg = Color(0xFF1E1E1E)
    val primaryColor = Color(0xFFBB86FC)
    val accentGreen = Color(0xFF03DAC6)
    val accentRed = Color(0xFFCF6679)

    val cameraPositionState = rememberCameraPositionState()

    // Center camera on user location when loaded
    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(it, 15f)
                )
            )
        }
    }

    // Center camera on friend location when tracking alert updates coordinates
    LaunchedEffect(trackedLocation) {
        trackedLocation?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(it, 16f)
                )
            )
        }
    }

    // Fetch initial user location
    LaunchedEffect(Unit) {
        viewModel.fetchUserLocation()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safety Map", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (isTracking) {
                        IconButton(onClick = { viewModel.stopTrackingAlert() }) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Tracking",
                                tint = accentRed
                            )
                        }
                    } else {
                        IconButton(onClick = { showTrackingDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = "Track Friend",
                                tint = primaryColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkBg,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = darkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Google Map View
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
                properties = MapProperties(isMyLocationEnabled = false)
            ) {
                // Plot user location marker
                userLocation?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "You",
                        snippet = "Your current location",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }

                // Plot police station markers
                policeStations.forEach { station ->
                    Marker(
                        state = MarkerState(position = station.second),
                        title = station.first,
                        snippet = "Tap card at bottom or bubble to navigate",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
                        onInfoWindowClick = {
                            val gmmIntentUri = Uri.parse("google.navigation:q=${station.second.latitude},${station.second.longitude}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            if (mapIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(mapIntent)
                            } else {
                                Toast.makeText(context, "Google Maps app not found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // Plot tracked friend marker
                trackedLocation?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "Emergency Contact (Live)",
                        snippet = "Friend needs help!",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                    )
                }
            }

            // Map Overlays
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Horizontal list of police stations
                if (policeStations.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(policeStations) { station ->
                            PoliceStationCard(
                                name = station.first,
                                location = station.second,
                                cardBg = cardBg,
                                accentGreen = accentGreen,
                                onNavigateClick = {
                                    val gmmIntentUri = Uri.parse("google.navigation:q=${station.second.latitude},${station.second.longitude}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                    context.startActivity(mapIntent)
                                }
                            )
                        }
                    }
                }

                // Floating Action Indicators (GPS Status, Tracking Active Indicator)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tracking Alert Status Card
                    AnimatedVisibility(visible = isTracking) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(accentGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tracking friend alert...",
                                    fontSize = 12.sp,
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Re-center button
                    FloatingActionButton(
                        onClick = { viewModel.fetchUserLocation() },
                        containerColor = cardBg,
                        contentColor = primaryColor,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "My Location"
                        )
                    }
                }
            }

            // Spinner Overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = primaryColor)
                }
            }
        }

        // Track Friend Alert Modal Dialog
        if (showTrackingDialog) {
            AlertDialog(
                onDismissRequest = { showTrackingDialog = false },
                title = {
                    Text(
                        text = "Track Emergency Alert",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Enter your friend's Firebase UID and their active Alert ID to listen to their coordinates live on the map.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = friendUidInput,
                            onValueChange = { friendUidInput = it },
                            label = { Text("Friend's Firebase UID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = alertIdInput,
                            onValueChange = { alertIdInput = it },
                            label = { Text("Active Alert ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (friendUidInput.isNotBlank() && alertIdInput.isNotBlank()) {
                                viewModel.startTrackingAlert(friendUidInput, alertIdInput)
                                showTrackingDialog = false
                            } else {
                                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Start Tracking")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTrackingDialog = false }) {
                        Text("Cancel", color = Color.LightGray)
                    }
                },
                containerColor = cardBg
            )
        }
    }
}

@Composable
fun PoliceStationCard(
    name: String,
    location: LatLng,
    cardBg: Color,
    accentGreen: Color,
    onNavigateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = accentGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "Radius: Within 3km",
                color = Color.Gray,
                fontSize = 11.sp
            )
            Button(
                onClick = onNavigateClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D2D2D)),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Navigate",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Navigate", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}
