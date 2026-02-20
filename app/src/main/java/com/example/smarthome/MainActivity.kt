package com.example.smarthome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smarthome.ui.theme.SmartHomeTheme
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

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            Navdraw(
                drawerState = drawerState,
                onHomeClick = {
                    scope.launch { drawerState.close() }
                },
                onSettingsClick = {
                    scope.launch {
                        drawerState.close()
                        navController.navigate("settings")
                    }
                }
            ) {
                HomeScreen(onMenuClick = {
                    scope.launch { drawerState.open() }
                })
            }
        }
        composable("settings") {
            SettingsScreen(onBackClick = {
                navController.popBackStack()
            })
        }
    }
}

@Composable
fun HomeScreen(onMenuClick: () -> Unit) {
    Scaffold(
        topBar = {
            Appbar(onMenuClick = onMenuClick)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Row(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                Box(modifier = Modifier.fillMaxSize().background(color = Color.Gray))
            }
            Row(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(20.dp).background(color = Color.Cyan))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Appbar(onMenuClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text("Smart Home") },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            IconButton(onClick = { /* Profile click */ }) {
                Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Account")
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        )
    )
}

@Composable
fun Navdraw(
    drawerState: DrawerState,
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxHeight().width(250.dp),
                drawerContainerColor = MaterialTheme.colorScheme.primaryContainer,
                drawerContentColor = MaterialTheme.colorScheme.primary,
            ) {
                Text(
                    "   Smart Home",
                    modifier = Modifier.padding(vertical = 16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Home") },
                    selected = false,
                    onClick = onHomeClick,
                    icon = { Icon(Icons.Default.Home, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = onSettingsClick,
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                )
            }
        }
    ) {
        content()
    }
}
