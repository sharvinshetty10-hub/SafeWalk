package com.example.safewalk.ui.home

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.safewalk.FakeCallActivity
import com.example.safewalk.service.ShakeMonitorService
import com.example.safewalk.ui.sos.SosState
import com.example.safewalk.ui.sos.SosViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToContacts: () -> Unit,
    onNavigateToMap: () -> Unit,
    onSignOut: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val email = auth.currentUser?.email ?: "User"
    val displayName = email.substringBefore("@")

    val context = LocalContext.current
    val sosViewModel: SosViewModel = hiltViewModel()
    val sosState by sosViewModel.state.collectAsState()

    // Elegant Dark Theme Colors
    val darkBg = Color(0xFF121212)
    val cardBg = Color(0xFF1E1E1E)
    val primaryColor = Color(0xFFBB86FC)
    val accentGreen = Color(0xFF03DAC6)

    val permissions = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    var hasLocationPermission by remember {
        mutableStateOf(
            permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    // Shared preferences to persist the toggle state
    val sharedPrefs = remember { context.getSharedPreferences("safewalk_prefs", Context.MODE_PRIVATE) }
    var isShakeEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("shake_enabled", false)) }

    val toggleShakeService = { enabled: Boolean ->
        isShakeEnabled = enabled
        sharedPrefs.edit().putBoolean("shake_enabled", enabled).apply()
        val intent = Intent(context, ShakeMonitorService::class.java)
        if (enabled) {
            val hasLoc = permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            if (hasLoc) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                Toast.makeText(context, "Location permission required to start protection", Toast.LENGTH_SHORT).show()
                isShakeEnabled = false
                sharedPrefs.edit().putBoolean("shake_enabled", false).apply()
            }
        } else {
            context.stopService(intent)
        }
    }

    // Sync service state on launch safely
    LaunchedEffect(Unit) {
        val enabled = sharedPrefs.getBoolean("shake_enabled", false)
        val hasLoc = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (enabled && hasLoc) {
            val intent = Intent(context, ShakeMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else if (enabled && !hasLoc) {
            // Reset disabled service due to missing permissions
            sharedPrefs.edit().putBoolean("shake_enabled", false).apply()
            isShakeEnabled = false
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasLocationPermission = results.values.all { it }
        if (hasLocationPermission) {
            sosViewModel.triggerSos("manual")
        } else {
            Toast.makeText(context, "Location permission is required for SOS alerts", Toast.LENGTH_LONG).show()
        }
    }

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.POST_NOTIFICATIONS
    } else {
        null
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            toggleShakeService(true)
        } else {
            Toast.makeText(context, "Notification permission is required for background protection", Toast.LENGTH_LONG).show()
        }
    }

    // Auto reset the SOS button to Idle after showing "Sent" or "Failed" for a few seconds
    LaunchedEffect(sosState) {
        if (sosState is SosState.Sent || sosState is SosState.Failed) {
            delay(3500)
            sosViewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SafeWalk",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = {
                        // Make sure we stop shake monitor service when signing out
                        toggleShakeService(false)
                        auth.signOut()
                        onSignOut()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkBg
                )
            )
        },
        containerColor = darkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Welcome Area
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text(
                    text = "Welcome,",
                    color = Color.LightGray,
                    fontSize = 16.sp
                )
                Text(
                    text = displayName,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Quick Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield Icon",
                        tint = accentGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Status: Protected",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Emergency alert systems are fully active.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Main Pulsing SOS Trigger Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(170.dp)
                    .padding(vertical = 8.dp)
            ) {
                val pulsingColor = when (sosState) {
                    SosState.Sending -> Color(0xFFE57373).copy(alpha = 0.2f)
                    SosState.Sent -> Color(0xFF81C784).copy(alpha = 0.2f)
                    SosState.Failed -> Color(0xFFFFB74D).copy(alpha = 0.2f)
                    else -> Color(0xFFCF6679).copy(alpha = 0.15f)
                }

                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(CircleShape)
                        .background(pulsingColor)
                )

                SosButton(
                    state = sosState,
                    onClick = {
                        if (hasLocationPermission) {
                            sosViewModel.triggerSos("manual")
                        } else {
                            locationLauncher.launch(permissions)
                        }
                    }
                )
            }

            // Shake-to-Trigger Toggle Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Shake icon",
                            tint = primaryColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Shake-to-Trigger",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Trigger SOS automatically on shake",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Switch(
                        checked = isShakeEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                val hasLoc = permissions.all {
                                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                                }
                                if (!hasLoc) {
                                    Toast.makeText(context, "Please grant Location permission first using the SOS button", Toast.LENGTH_LONG).show()
                                } else {
                                    if (notificationPermission != null &&
                                        ContextCompat.checkSelfPermission(context, notificationPermission) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationLauncher.launch(notificationPermission)
                                    } else {
                                        toggleShakeService(true)
                                    }
                                }
                            } else {
                                toggleShakeService(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = primaryColor,
                            checkedTrackColor = primaryColor.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Fake Call Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Fake call icon",
                            tint = accentGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Fake Call Escape",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Trigger a mock call to exit safely",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val intent = Intent(context, FakeCallActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2D2D2D),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Trigger", fontSize = 13.sp)
                    }
                }
            }

            // Map Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateToMap() },
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Map icon",
                        tint = accentGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live Safety Map",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Find nearby police stations & track alerts",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "→",
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            // Contacts Navigation Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateToContacts() },
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Contacts Icon",
                            tint = primaryColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Emergency Contacts",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add and manage trusted contacts who will receive emergency alerts when you trigger SOS.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Manage Contacts →",
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SafeWalk protects you wherever you go.",
                color = Color.DarkGray,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SosButton(
    state: SosState,
    onClick: () -> Unit
) {
    val buttonColors = when (state) {
        SosState.Sending -> ButtonDefaults.buttonColors(
            containerColor = Color(0xFFD32F2F),
            contentColor = Color.White
        )
        SosState.Sent -> ButtonDefaults.buttonColors(
            containerColor = Color(0xFF388E3C),
            contentColor = Color.White
        )
        SosState.Failed -> ButtonDefaults.buttonColors(
            containerColor = Color(0xFFF57C00),
            contentColor = Color.White
        )
        else -> ButtonDefaults.buttonColors(
            containerColor = Color(0xFFB00020),
            contentColor = Color.White
        )
    }

    Button(
        onClick = onClick,
        colors = buttonColors,
        shape = CircleShape,
        modifier = Modifier
            .size(140.dp)
            .shadow(16.dp, CircleShape),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "SOS Warning symbol",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when (state) {
                    SosState.Sending -> "Sending..."
                    SosState.Sent -> "Sent ✓"
                    SosState.Failed -> "Queued ⚠"
                    else -> "TAP SOS"
                },
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
