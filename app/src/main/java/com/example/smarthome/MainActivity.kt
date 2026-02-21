package com.example.smarthome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smarthome.ui.theme.SmartHomeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

// Dynamic Data Structures
data class RoomStatus(
    val name: String,
    val bulbs: Int,
    val power: Int,
    val comment: String,
    val devices: List<String> = emptyList()
)

data class UsageStats(
    val type: String,
    val data: List<Float>? = null,
    val summary: String? = null
)

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val customRooms = remember { mutableStateListOf<RoomStatus>() }

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
                    onAddRoom = { name, devices ->
                        customRooms.add(RoomStatus(name, 0, 0, "Newly added room", devices))
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
                        customRooms.remove(room)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(onMenuClick: () -> Unit, onAddRoom: (String, List<String>) -> Unit) {
    var showAddRoomDialog by remember { mutableStateOf(false) }

    // Simulation states
    var roomIndex by remember { mutableIntStateOf(0) }
    var usageIndex by remember { mutableIntStateOf(0) }

    val defaultRooms = listOf(
        RoomStatus("Living Room", 4, 145, "Optimal usage today"),
        RoomStatus("Kitchen", 2, 850, "Heavy appliance active"),
        RoomStatus("Bedroom", 1, 40, "All systems efficient"),
        RoomStatus("Garage", 0, 0, "Standby mode")
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
            onAdd = { name, devices ->
                onAddRoom(name, devices)
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

@Composable
fun AddRoomDialog(onDismiss: () -> Unit, onAdd: (String, List<String>) -> Unit) {
    var roomName by remember { mutableStateOf("") }
    var device1 by remember { mutableStateOf("") }
    var device2 by remember { mutableStateOf("") }
    var device3 by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Room", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Room Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                HorizontalDivider()
                Text("Device Names:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                
                OutlinedTextField(
                    value = device1,
                    onValueChange = { device1 = it },
                    label = { Text("Device 1 Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = device2,
                    onValueChange = { device2 = it },
                    label = { Text("Device 2 Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = device3,
                    onValueChange = { device3 = it },
                    label = { Text("Device 3 Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (roomName.isNotBlank()) {
                        val devices = listOf(device1, device2, device3).filter { it.isNotBlank() }
                        onAdd(roomName, devices) 
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicRoomScreen(room: RoomStatus, onBackClick: () -> Unit, onDeleteRoom: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(room.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Connected Devices", style = MaterialTheme.typography.titleLarge)
                Button(onClick = { /* Configure All */ }) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Configure")
                }
            }
            
            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(room.devices) { device ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(device, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Safe Zone - Delete Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Danger Zone", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                        Text("Permanently remove this room", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
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
                        NavigationDrawerItem(label = { Text("Dashboard") }, selected = true, onClick = onHomeClick, icon = { Icon(Icons.Default.Home, null) }, modifier = Modifier.padding(12.dp))
                    }
                    if (customRooms.isNotEmpty()) {
                        item { Text("My Rooms", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                        items(customRooms) { room ->
                            NavigationDrawerItem(label = { Text(room.name) }, selected = false, onClick = { onRoomClick(room.name) }, icon = { Icon(Icons.Default.Home, null) }, modifier = Modifier.padding(horizontal = 12.dp))
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
