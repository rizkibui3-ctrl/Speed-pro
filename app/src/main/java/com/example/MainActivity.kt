package com.example

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.OrientationMode
import com.example.data.model.SpeedProfile
import com.example.data.model.SpeedUnit
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.JukenEcuScreen
import com.example.ui.screens.PerformanceScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TripsScreen
import com.example.ui.theme.CockpitBackground
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.SpeedProTheme
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.SpeedProViewModel

private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_JUKEN = "juken"
private const val ROUTE_PERFORMANCE = "performance"
private const val ROUTE_TRIPS = "trips"
private const val ROUTE_SETTINGS = "settings"

private data class NavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val NAV_TABS = listOf(
    NavTab(
        route = ROUTE_DASHBOARD,
        labelRes = R.string.nav_dashboard,
        selectedIcon = Icons.Rounded.Speed,
        unselectedIcon = Icons.Outlined.Speed,
        testTag = "nav_tab_dashboard",
    ),
    NavTab(
        route = ROUTE_JUKEN,
        labelRes = R.string.nav_juken,
        selectedIcon = Icons.Rounded.Memory,
        unselectedIcon = Icons.Outlined.Memory,
        testTag = "nav_tab_juken",
    ),
    NavTab(
        route = ROUTE_PERFORMANCE,
        labelRes = R.string.nav_performance,
        selectedIcon = Icons.Rounded.Timer,
        unselectedIcon = Icons.Outlined.Timer,
        testTag = "nav_tab_performance",
    ),
    NavTab(
        route = ROUTE_TRIPS,
        labelRes = R.string.nav_trips,
        selectedIcon = Icons.Rounded.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_tab_trips",
    ),
    NavTab(
        route = ROUTE_SETTINGS,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_tab_settings",
    ),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            SpeedProApp()
        }
    }
}

@Composable
fun SpeedProApp(
    viewModel: SpeedProViewModel = viewModel(),
) {
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val liveTrip by viewModel.liveTrip.collectAsStateWithLifecycle()
    val accelState by viewModel.acceleration.collectAsStateWithLifecycle()
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    val accelerationRuns by viewModel.accelerationRuns.collectAsStateWithLifecycle()
    val jukenState by viewModel.jukenConnectionState.collectAsStateWithLifecycle()
    val jukenStatusMsg by viewModel.jukenStatusMessage.collectAsStateWithLifecycle()
    val jukenConnectedDevice by viewModel.jukenConnectedDevice.collectAsStateWithLifecycle()
    val jukenDevices by viewModel.jukenDiscoveredDevices.collectAsStateWithLifecycle()
    val jukenTelemetry by viewModel.jukenTelemetry.collectAsStateWithLifecycle()
    val ecuHistory by viewModel.ecuHistory.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.startTelemetry()
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.refreshPairedJukenDevices()
        if (viewModel.hasBluetoothPermissions() && viewModel.isBluetoothEnabled()) {
            viewModel.startJukenScan()
        }
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        viewModel.refreshPairedJukenDevices()
    }

    val requestLocationPermissions: () -> Unit = {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val requestBluetoothPermissions: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            bluetoothPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    val requestEnableBluetooth: () -> Unit = {
        runCatching {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    LaunchedEffect(Unit) {
        requestLocationPermissions()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.startTelemetry()
                Lifecycle.Event.ON_PAUSE -> viewModel.stopTelemetry()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Keep screen awake when enabled
    DisposableEffect(settings.keepScreenOn, context) {
        val window = (context as? Activity)?.window
        if (settings.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Apply user-selected orientation mode (Landscape / Portrait / Auto)
    LaunchedEffect(settings.orientationMode, context) {
        val activity = context as? Activity ?: return@LaunchedEffect
        activity.requestedOrientation = when (settings.orientationMode) {
            OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            OrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    SpeedProTheme(accent = settings.cockpitAccent) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        val navigateToTab: (String) -> Unit = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 840.dp || (maxWidth > maxHeight && maxWidth >= 560.dp)

            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = CockpitSurface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        NAV_TABS.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Scaffold(
                    containerColor = CockpitBackground,
                    contentWindowInsets = if (useNavRail) {
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    } else {
                        WindowInsets.safeDrawing
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (!useNavRail) {
                            NavigationBar(
                                containerColor = CockpitSurface,
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                NAV_TABS.forEach { tab ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTab(tab.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = stringResource(tab.labelRes)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(tab.labelRes),
                                                maxLines = 1
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            unselectedIconColor = TextMuted,
                                            unselectedTextColor = TextMuted
                                        ),
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_DASHBOARD
                        ) {
                            composable(ROUTE_DASHBOARD) {
                                DashboardScreen(
                                    telemetry = telemetry,
                                    settings = settings,
                                    liveTrip = liveTrip,
                                    jukenState = jukenState,
                                    jukenTelemetry = jukenTelemetry,
                                    onRequestLocationPermission = requestLocationPermissions,
                                    onCycleOrientation = viewModel::cycleOrientationMode,
                                    onOpenJukenTab = { navigateToTab(ROUTE_JUKEN) },
                                    onCycleUnit = {
                                        val units = SpeedUnit.entries
                                        val next = units[(settings.speedUnit.ordinal + 1) % units.size]
                                        viewModel.setSpeedUnit(next)
                                    },
                                    onCycleProfile = {
                                        val profiles = SpeedProfile.entries
                                        val next = profiles[(settings.speedProfile.ordinal + 1) % profiles.size]
                                        viewModel.setSpeedProfile(next)
                                    },
                                    onToggleSpeedWarning = {
                                        viewModel.setSpeedWarningEnabled(!settings.speedWarningEnabled)
                                    },
                                    onToggleTripChannel = viewModel::toggleTripChannel,
                                    onToggleTripTracking = viewModel::toggleTripTracking,
                                    onSaveTrip = { viewModel.saveCurrentTrip() },
                                    onResetTrip = viewModel::resetActiveTrip
                                )
                            }

                            composable(ROUTE_JUKEN) {
                                JukenEcuScreen(
                                    connectionState = jukenState,
                                    statusMessage = jukenStatusMsg,
                                    connectedDevice = jukenConnectedDevice,
                                    discoveredDevices = jukenDevices,
                                    ecuTelemetry = jukenTelemetry,
                                    ecuHistory = ecuHistory,
                                    savedTuning = settings.jukenTuning,
                                    lastDeviceName = settings.lastJukenDeviceName,
                                    lastDeviceAddress = settings.lastJukenDeviceAddress,
                                    onRequestBluetoothPermissions = requestBluetoothPermissions,
                                    onEnableBluetooth = requestEnableBluetooth,
                                    onStartScan = viewModel::startJukenScan,
                                    onConnectDevice = viewModel::connectJukenDevice,
                                    onDisconnect = viewModel::disconnectJukenDevice,
                                    onApplyLiveTuning = viewModel::sendJukenTuningToEcu
                                )
                            }

                            composable(ROUTE_PERFORMANCE) {
                                PerformanceScreen(
                                    telemetry = telemetry,
                                    accelState = accelState,
                                    speedUnit = settings.speedUnit,
                                    savedRuns = accelerationRuns,
                                    onArmTimer = viewModel::armAccelerationTimer,
                                    onResetTimer = viewModel::cancelOrResetAccelerationTimer,
                                    onFinishAndSave = viewModel::finishAndSaveAccelerationRun,
                                    onDeleteRun = viewModel::deleteAccelerationRun,
                                    onClearRuns = viewModel::clearAccelerationRuns
                                )
                            }

                            composable(ROUTE_TRIPS) {
                                TripsScreen(
                                    trips = trips,
                                    speedUnit = settings.speedUnit,
                                    onDeleteTrip = viewModel::deleteTrip,
                                    onClearTrips = viewModel::clearAllTrips
                                )
                            }

                            composable(ROUTE_SETTINGS) {
                                SettingsScreen(
                                    settings = settings,
                                    odometerMeters = liveTrip.odometerMeters,
                                    onSetUnit = viewModel::setSpeedUnit,
                                    onSetProfile = viewModel::setSpeedProfile,
                                    onSetAccent = viewModel::setCockpitAccent,
                                    onSetOrientationMode = viewModel::setOrientationMode,
                                    onSetWarningEnabled = viewModel::setSpeedWarningEnabled,
                                    onSetWarningLimitKmh = viewModel::setSpeedWarningLimitKmh,
                                    onSetWarningSound = viewModel::setSpeedWarningSoundEnabled,
                                    onSetWarningVibrate = viewModel::setSpeedWarningVibrateEnabled,
                                    onSetKeepScreenOn = viewModel::setKeepScreenOn,
                                    onSetShowCompassAlt = viewModel::setShowCompassAndAltitude,
                                    onSetOdometerKm = viewModel::setOdometerKm
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
