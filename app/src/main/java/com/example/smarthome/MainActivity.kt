package com.example.smarthome

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smarthome.ui.theme.SmartHomeTheme
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartHomeTheme {
                MainScreen()
            }
        }
    }
}

// Data Structures
data class DeviceInfo(
    val name: String,
    val type: String
)

data class RoomStatus(
    val name: String,
    val bulbs: Int,
    val power: Int,
    val comment: String,
    val devices: List<DeviceInfo> = emptyList(),
    val color: Color = Color(0xFF6200EE)
)

data class UsageStats(
    val type: String,
    val data: List<Float>? = null,
    val summary: String? = null
)

// Persistence Helper
object RoomPreferences {
    private const val PREFS_NAME = "smart_home_prefs"
    private const val ROOMS_KEY = "custom_rooms_v2"

    fun saveRooms(context: Context, rooms: List<RoomStatus>) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val roomsString = rooms.joinToString(";") { room ->
            val devicesString = room.devices.joinToString(",") { "${it.name}:${it.type}" }
            "${room.name}|$devicesString|${room.color.toArgb()}"
        }
        sharedPrefs.edit().putString(ROOMS_KEY, roomsString).apply()
    }

    fun loadRooms(context: Context): List<RoomStatus> {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val roomsString = sharedPrefs.getString(ROOMS_KEY, "") ?: ""
        if (roomsString.isBlank()) return emptyList()
        
        return roomsString.split(";").mapNotNull { roomData ->
            val parts = roomData.split("|")
            if (parts.size >= 3) {
                val name = parts[0]
                val devices = parts[1].split(",").filter { it.isNotBlank() }.map {
                    val dParts = it.split(":")
                    DeviceInfo(dParts[0], if(dParts.size > 1) dParts[1] else "Bulb")
                }
                val colorInt = parts[2].toIntOrNull() ?: 0xFF6200EE.toInt()
                RoomStatus(name, 0, 0, "Saved room", devices, Color(colorInt))
            } else null
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val customRooms = remember { 
        RoomPreferences.loadRooms(context).toMutableStateList()
    }

    LaunchedEffect(customRooms.size) {
        RoomPreferences.saveRooms(context, customRooms)
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            Navdraw(
                drawerState = drawerState,
                customRooms = customRooms,
                onHomeClick = { scope.launch { drawerState.close() } },
                onSettingsClick = {
                    scope.launch {
                        drawerState.close()
                        navController.navigate("settings")
                    }
                },
                onRoomClick = { roomName ->
                    scope.launch {
                        drawerState.close()
                        navController.navigate("room/$roomName")
                    }
                }
            ) {
                HomeScreen(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onAddRoom = { name, devices, color ->
                        val newRoom = RoomStatus(name, 0, 0, "Newly added room", devices, color)
                        customRooms.add(newRoom)
                        
                        // Initialize room and devices in Firebase
                        val database = FirebaseDatabase.getInstance("https://smarthome-b527c-default-rtdb.asia-southeast1.firebasedatabase.app")
                        val roomRef = database.getReference(name)
                        devices.forEach { device ->
                            roomRef.child(device.name).setValue(0)
                        }
                    }
                )
            }
        }
        composable("settings") {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable("room/{roomName}") { backStackEntry ->
            val roomName = backStackEntry.arguments?.getString("roomName") ?: ""
            val room = customRooms.find { it.name == roomName }
            if (room != null) {
                DynamicRoomScreen(
                    room = room,
                    onBackClick = { navController.popBackStack() },
                    onDeleteRoom = {
                        // Remove room from Firebase
                        val database = FirebaseDatabase.getInstance("https://smarthome-b527c-default-rtdb.asia-southeast1.firebasedatabase.app")
                        database.getReference(room.name).removeValue()
                        
                        customRooms.remove(room)
                        RoomPreferences.saveRooms(context, customRooms)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(onMenuClick: () -> Unit, onAddRoom: (String, List<DeviceInfo>, Color) -> Unit) {
    var showAddRoomDialog by remember { mutableStateOf(false) }

    var roomIndex by remember { mutableIntStateOf(0) }
    var usageIndex by remember { mutableIntStateOf(0) }

    val defaultRooms = listOf(
        RoomStatus("Living Room", 4, 145, "Optimal usage today", emptyList(), Color(0xFF2196F3)),
        RoomStatus("Kitchen", 2, 850, "Heavy appliance active", emptyList(), Color(0xFF4CAF50)),
        RoomStatus("Bedroom", 1, 40, "All systems efficient", emptyList(), Color(0xFF9C27B0)),
        RoomStatus("Garage", 0, 0, "Standby mode", emptyList(), Color(0xFF607D8B))
    )

    val usageModes = listOf(
        UsageStats("REAL-TIME USAGE", data = listOf(0.4f, 0.6f, 0.8f, 0.3f, 0.7f)),
        UsageStats("MONTHLY BILL", summary = "Est: $142.50\nUnits: 342 kWh\nAvg: 11 kWh/day")
    )

    LaunchedEffect(Unit) {
        while(true) {
            delay(4000)
            roomIndex = (roomIndex + 1) % defaultRooms.size
            usageIndex = (usageIndex + 1) % usageModes.size
        }
    }

    if (showAddRoomDialog) {
        AddRoomDialog(
            onDismiss = { showAddRoomDialog = false },
            onAdd = { name, devices, color ->
                onAddRoom(name, devices, color)
                showAddRoomDialog = false
            }
        )
    }

    Scaffold(
        topBar = { Appbar(onMenuClick = onMenuClick) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddRoomDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Room") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Welcome Back,", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(text = "Your Smart Home is Secure", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Overview(currentRoom = defaultRooms[roomIndex], usage = usageModes[usageIndex])
            EnergySaverCard()
            AnomalyCard()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRoomDialog(onDismiss: () -> Unit, onAdd: (String, List<DeviceInfo>, Color) -> Unit) {
    var roomName by remember { mutableStateOf("") }
    val deviceNames = remember { mutableStateListOf("", "", "") }
    val deviceTypes = remember { mutableStateListOf("Bulb", "Bulb", "Bulb") }
    
    val professionalColors = listOf(
        Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFF9C27B0), 
        Color(0xFFF44336), Color(0xFFFF9800), Color(0xFF607D8B)
    )
    var selectedColor by remember { mutableStateOf(professionalColors[0]) }

    val typeOptions = listOf("Bulb", "Fan", "Heater", "Others")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Room", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Room Name (e.g., BedRoom)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                HorizontalDivider()
                Text("Select Room Theme:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    professionalColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                                .clickable { selectedColor = color }
                                .let { 
                                    if (selectedColor == color) it.background(color, RoundedCornerShape(4.dp)).padding(2.dp).background(Color.White, RoundedCornerShape(4.dp)).padding(2.dp).background(color, RoundedCornerShape(4.dp))
                                    else it
                                }
                        )
                    }
                }
                HorizontalDivider()
                Text("Device Config:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                
                deviceNames.forEachIndexed { index, name ->
                    Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)).padding(8.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { deviceNames[index] = it },
                            label = { Text("Device ${index + 1} Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = deviceTypes[index],
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                typeOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            deviceTypes[index] = option
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                IconButton(onClick = { 
                    deviceNames.add("")
                    deviceTypes.add("Bulb")
                }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(Icons.Default.Add, contentDescription = "Add device", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                if (roomName.isNotBlank()) {
                    val finalDevices = deviceNames.mapIndexedNotNull { i, name ->
                        if (name.isNotBlank()) DeviceInfo(name, deviceTypes[i]) else null
                    }
                    onAdd(roomName, finalDevices, selectedColor) 
                }
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicRoomScreen(room: RoomStatus, onBackClick: () -> Unit, onDeleteRoom: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBluetoothConfig by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Room?") },
            text = { Text("Are you sure you want to delete ${room.name}?") },
            confirmButton = {
                TextButton(onClick = onDeleteRoom) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBluetoothConfig) {
        BluetoothProvisioningDialog(room = room, onDismiss = { showBluetoothConfig = false })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(room.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = room.color,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = room.color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, room.color.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(room.name, style = MaterialTheme.typography.headlineSmall, color = room.color, fontWeight = FontWeight.Bold)
                        Text("${room.devices.size} Devices Total", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Button(
                            onClick = { showBluetoothConfig = true },
                            colors = ButtonDefaults.buttonColors(containerColor = room.color)
                        ) {
                            Icon(Icons.Default.Settings, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Configure")
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.clickable { showDeleteConfirm = true },
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Delete Room", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            Text("Control Center", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(room.devices) { device ->
                    DeviceControlCard(roomName = room.name, device = device, roomColor = room.color)
                }
            }
        }
    }
}

@Composable
fun DeviceControlCard(roomName: String, device: DeviceInfo, roomColor: Color) {
    var isActive by remember { mutableStateOf(false) }
    var fanSpeed by remember { mutableFloatStateOf(0f) }
    
    val database = FirebaseDatabase.getInstance("https://smarthome-b527c-default-rtdb.asia-southeast1.firebasedatabase.app")
    val deviceRef = database.getReference(roomName).child(device.name)

    val icon = when (device.type) {
        "Bulb" -> Icons.Default.Lightbulb
        "Fan" -> Icons.Default.Air
        "Heater" -> Icons.Default.Bolt
        "AC" -> Icons.Default.AcUnit
        "TV" -> Icons.Default.Tv
        else -> Icons.Default.Devices
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp), 
                        color = if (isActive || (device.type == "Fan" && fanSpeed > 0)) roomColor.copy(0.15f) else Color.LightGray.copy(0.1f), 
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) { 
                            Icon(icon, null, tint = if (isActive || (device.type == "Fan" && fanSpeed > 0)) roomColor else Color.Gray)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(device.type, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
                
                if (device.type != "Fan") {
                    Switch(
                        checked = isActive,
                        onCheckedChange = { 
                            isActive = it
                            deviceRef.setValue(if(it) 1 else 0)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = roomColor,
                            checkedTrackColor = roomColor.copy(alpha = 0.5f)
                        )
                    )
                }
            }
            
            if (device.type == "Fan") {
                Spacer(Modifier.height(16.dp))
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Speed: ${fanSpeed.roundToInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = roomColor)
                        Text(if(fanSpeed > 0) "Rotating" else "Stopped", style = MaterialTheme.typography.labelSmall, color = if(fanSpeed > 0) roomColor else Color.Gray)
                    }
                    Slider(
                        value = fanSpeed,
                        onValueChange = { 
                            fanSpeed = it
                            deviceRef.setValue(it.roundToInt())
                        },
                        valueRange = 0f..100f,
                        steps = 3,
                        colors = SliderDefaults.colors(
                            thumbColor = roomColor,
                            activeTrackColor = roomColor,
                            inactiveTrackColor = roomColor.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun BluetoothProvisioningDialog(room: RoomStatus, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    val scope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(0) } // 0: Permissions, 1: Scanning, 2: Device Selection, 3: WiFi Config, 4: Success
    val foundDevices = remember { mutableStateListOf<BluetoothDevice>() }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf("") }
    var isOperating by remember { mutableStateOf(false) }

    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) step = 1 else Toast.makeText(context, "Permissions required", Toast.LENGTH_SHORT).show()
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (BluetoothDevice.ACTION_FOUND == intent?.action) {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let { if (!foundDevices.contains(it)) foundDevices.add(it) }
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(step) {
        if (step == 1 && bluetoothAdapter != null) {
            foundDevices.clear()
            bluetoothAdapter.startDiscovery()
            delay(10000)
            bluetoothAdapter.cancelDiscovery()
            if (foundDevices.isNotEmpty()) step = 2 else statusMsg = "No devices found. Try again."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bluetooth, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Bluetooth Provisioning")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                when (step) {
                    0 -> {
                        Text("Need permissions to scan for ESP32.")
                        Button(onClick = {
                            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            } else {
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                            permissionLauncher.launch(permissions)
                        }) { Text("Grant Permissions") }
                    }
                    1 -> {
                        Text("Searching for your Smart Hub...")
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    2 -> {
                        Text("Select your ESP32 device:")
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(foundDevices) { device ->
                                val name = try { device.name } catch(e: SecurityException) { null } ?: "Unknown Device"
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { 
                                        selectedDevice = device
                                        step = 3
                                    }.padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(name, modifier = Modifier.padding(12.dp))
                                }
                            }
                        }
                    }
                    3 -> {
                        Text("Device: ${try { selectedDevice?.name } catch(e: SecurityException) { "Selected" }}")
                        Text("Provisioning Room: ${room.name}", fontWeight = FontWeight.Bold, color = room.color)
                        OutlinedTextField(value = ssid, onValueChange = { ssid = it }, label = { Text("Wi-Fi SSID") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Wi-Fi Password") }, modifier = Modifier.fillMaxWidth())
                        if (isOperating) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text(statusMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    4 -> {
                        Icon(Icons.Default.Bolt, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally))
                        Text("Configuration sent successfully!", textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            if (step == 3) {
                Button(enabled = !isOperating && ssid.isNotBlank(), onClick = {
                    scope.launch {
                        isOperating = true
                        statusMsg = "Connecting to ESP32..."
                        val result = withContext(Dispatchers.IO) {
                            var socket: BluetoothSocket? = null
                            try {
                                socket = selectedDevice?.createRfcommSocketToServiceRecord(SPP_UUID)
                                socket?.connect()
                                val devicesList = room.devices.joinToString(",") { it.name }
                                // Sending Room Name along with devices and WiFi credentials
                                val payload = "ROOM:${room.name};DEVICES:$devicesList;SSID:$ssid;PASS:$password\n"
                                socket?.outputStream?.write(payload.toByteArray())
                                true
                            } catch (e: Exception) {
                                Log.e("BT", "Error during Bluetooth transfer", e)
                                false
                            } finally {
                                try { socket?.close() } catch (e: Exception) {}
                            }
                        }
                        isOperating = false
                        if (result) step = 4 else statusMsg = "Connection failed. Ensure ESP32 is powered and in range."
                    }
                }) { Text("Send Config") }
            } else if (step == 4) {
                Button(onClick = onDismiss) { Text("Finish") }
            }
        },
        dismissButton = {
            if (step < 4) TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Appbar(onMenuClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 4.dp) {
        CenterAlignedTopAppBar(
            title = { Text("SMART HUB", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp)) },
            navigationIcon = { IconButton(onClick = onMenuClick) { Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu") } },
            actions = { IconButton(onClick = { }) { Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Account") } },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
        )
    }
}

@Composable
fun Navdraw(
    drawerState: DrawerState,
    customRooms: List<RoomStatus>,
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRoomClick: (String) -> Unit,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxHeight().width(300.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp).background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer))).padding(24.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text("Smart Hub Pro", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
                LazyColumn {
                    item {
                        NavigationDrawerItem(
                            label = { Text("Dashboard") },
                            selected = true,
                            onClick = onHomeClick,
                            icon = { Icon(Icons.Default.Home, null) },
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    if (customRooms.isNotEmpty()) {
                        item { Text("My Rooms", modifier = Modifier.padding(16.dp, 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                        items(customRooms) { room ->
                            NavigationDrawerItem(
                                label = { Text(room.name) },
                                selected = false,
                                onClick = { onRoomClick(room.name) },
                                icon = { Icon(Icons.Default.Home, null, tint = room.color) },
                                modifier = Modifier.padding(horizontal = 12.dp),
                                colors = NavigationDrawerItemDefaults.colors(unselectedIconColor = room.color)
                            )
                        }
                    }
                    item {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        NavigationDrawerItem(label = { Text("Settings") }, selected = false, onClick = onSettingsClick, icon = { Icon(Icons.Default.Settings, null) }, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }
    ) { content() }
}

@Composable
fun Overview(currentRoom: RoomStatus, usage: UsageStats) {
    Row(modifier = Modifier.fillMaxWidth().height(220.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ElevatedCard(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text(text = "STATUS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Crossfade(targetState = currentRoom, label = "") { room ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusRow(Icons.Default.Home, room.name)
                        StatusRow(Icons.Default.Lightbulb, "${room.bulbs} Bulbs Active")
                        StatusRow(Icons.Default.Bolt, "${room.power} W Dissipation")
                    }
                }
            }
        }
        ElevatedCard(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                Crossfade(targetState = usage.type, label = "") { Text(text = it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(16.dp))
                Crossfade(targetState = usage, label = "") { currentUsage ->
                    if (currentUsage.data != null) {
                        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                            currentUsage.data.forEach { value ->
                                Box(modifier = Modifier.width(12.dp).fillMaxHeight(value).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary))
                            }
                        }
                    } else {
                        Text(text = currentUsage.summary ?: "", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun EnergySaverCard() {
    ElevatedCard(modifier = Modifier.fillMaxWidth().height(80.dp)) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(0.1f), modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Bolt, null, tint = MaterialTheme.colorScheme.primary) } }
                Spacer(Modifier.width(12.dp))
                Column { Text("Energy Saver", fontWeight = FontWeight.SemiBold); Text("Active", style = MaterialTheme.typography.bodySmall) }
            }
            Text("On", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AnomalyCard() {
    ElevatedCard(modifier = Modifier.fillMaxWidth().height(80.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(0.2f))) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.error.copy(0.1f), modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) } }
            Spacer(Modifier.width(12.dp))
            Column { Text("Security", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error); Text("All systems normal", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun StatusRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Preview(showSystemUi = true)
@Composable
fun AppPreview() { SmartHomeTheme { MainScreen() } }
