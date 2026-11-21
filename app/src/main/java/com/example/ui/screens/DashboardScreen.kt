package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GpsOff
import androidx.compose.material.icons.rounded.LocationSearching
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.SpeedProSettings
import com.example.data.model.GpsSignalStatus
import com.example.data.model.GpsTelemetry
import com.example.data.model.JukenConnectionState
import com.example.data.model.JukenEcuTelemetry
import com.example.data.model.LiveTripState
import com.example.data.model.SpeedProfile
import com.example.data.model.SpeedUnit
import com.example.ui.components.CockpitHeaderBar
import com.example.ui.components.CompassAndAltitudeStrip
import com.example.ui.components.JukenLiveStrip
import com.example.ui.components.SpeedometerGauge
import com.example.ui.components.TripComputerPanel
import com.example.ui.components.convertWarningKmhToUnit
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.GpsSearchingAmber
import com.example.ui.theme.SpeedDangerRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    telemetry: GpsTelemetry,
    settings: SpeedProSettings,
    liveTrip: LiveTripState,
    jukenState: JukenConnectionState,
    jukenTelemetry: JukenEcuTelemetry,
    onRequestLocationPermission: () -> Unit,
    onCycleOrientation: () -> Unit,
    onOpenJukenTab: () -> Unit,
    onCycleUnit: () -> Unit,
    onCycleProfile: () -> Unit,
    onToggleSpeedWarning: () -> Unit,
    onToggleTripChannel: () -> Unit,
    onToggleTripTracking: () -> Unit,
    onSaveTrip: () -> Unit,
    onResetTrip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val speedDisplay = settings.speedUnit.convertSpeed(telemetry.speedMps)
    val currentKmh = telemetry.speedMps * 3.6f
    val isOverspeed = settings.speedWarningEnabled && currentKmh > settings.speedWarningLimitKmh
    val warningInUnit = convertWarningKmhToUnit(
        settings.speedWarningLimitKmh,
        settings.speedUnit.speedMultiplierFromMps
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        val isWideLayout = maxWidth >= 560.dp || maxWidth > maxHeight

        if (isWideLayout) {
            // Adaptive Landscape Motor Matic Cockpit Layout
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CockpitHeaderBar(
                    telemetry = telemetry,
                    orientationMode = settings.orientationMode,
                    jukenState = jukenState,
                    onCycleOrientation = onCycleOrientation,
                    onOpenJukenTab = onOpenJukenTab
                )

                PermissionOrGpsBanner(
                    signalStatus = telemetry.signalStatus,
                    onRequestPermission = onRequestLocationPermission,
                    onOpenLocationSettings = {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.92f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        SpeedometerGauge(
                            speedValue = speedDisplay,
                            speedUnit = settings.speedUnit,
                            speedProfile = settings.speedProfile,
                            warningLimitInUnit = warningInUnit,
                            warningEnabled = settings.speedWarningEnabled,
                            isOverspeed = isOverspeed,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.18f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickCockpitChipsRow(
                            speedUnit = settings.speedUnit,
                            speedProfile = settings.speedProfile,
                            warningEnabled = settings.speedWarningEnabled,
                            warningLimitInUnit = warningInUnit,
                            onCycleUnit = onCycleUnit,
                            onCycleProfile = onCycleProfile,
                            onToggleWarning = onToggleSpeedWarning
                        )
                        JukenLiveStrip(
                            jukenState = jukenState,
                            ecuTelemetry = jukenTelemetry,
                            revLimiterRpm = settings.jukenTuning.revLimiterRpm,
                            onOpenJukenClick = onOpenJukenTab
                        )
                        TripComputerPanel(
                            liveTrip = liveTrip,
                            speedUnit = settings.speedUnit,
                            onToggleChannel = onToggleTripChannel,
                            onToggleTracking = onToggleTripTracking,
                            onSaveTrip = onSaveTrip,
                            onResetTrip = onResetTrip
                        )
                        if (settings.showCompassAndAltitude) {
                            CompassAndAltitudeStrip(
                                telemetry = telemetry,
                                speedUnit = settings.speedUnit
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait Motor Matic Cockpit Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CockpitHeaderBar(
                    telemetry = telemetry,
                    orientationMode = settings.orientationMode,
                    jukenState = jukenState,
                    onCycleOrientation = onCycleOrientation,
                    onOpenJukenTab = onOpenJukenTab
                )

                PermissionOrGpsBanner(
                    signalStatus = telemetry.signalStatus,
                    onRequestPermission = onRequestLocationPermission,
                    onOpenLocationSettings = {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                )

                SpeedometerGauge(
                    speedValue = speedDisplay,
                    speedUnit = settings.speedUnit,
                    speedProfile = settings.speedProfile,
                    warningLimitInUnit = warningInUnit,
                    warningEnabled = settings.speedWarningEnabled,
                    isOverspeed = isOverspeed,
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .widthIn(max = 320.dp)
                )

                QuickCockpitChipsRow(
                    speedUnit = settings.speedUnit,
                    speedProfile = settings.speedProfile,
                    warningEnabled = settings.speedWarningEnabled,
                    warningLimitInUnit = warningInUnit,
                    onCycleUnit = onCycleUnit,
                    onCycleProfile = onCycleProfile,
                    onToggleWarning = onToggleSpeedWarning
                )

                JukenLiveStrip(
                    jukenState = jukenState,
                    ecuTelemetry = jukenTelemetry,
                    revLimiterRpm = settings.jukenTuning.revLimiterRpm,
                    onOpenJukenClick = onOpenJukenTab
                )

                TripComputerPanel(
                    liveTrip = liveTrip,
                    speedUnit = settings.speedUnit,
                    onToggleChannel = onToggleTripChannel,
                    onToggleTracking = onToggleTripTracking,
                    onSaveTrip = onSaveTrip,
                    onResetTrip = onResetTrip
                )

                if (settings.showCompassAndAltitude) {
                    CompassAndAltitudeStrip(
                        telemetry = telemetry,
                        speedUnit = settings.speedUnit
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun PermissionOrGpsBanner(
    signalStatus: GpsSignalStatus,
    onRequestPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    when (signalStatus) {
        GpsSignalStatus.PERMISSION_NEEDED -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        RoundedCornerShape(16.dp)
                    )
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationSearching,
                    contentDescription = "Izin Lokasi",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Izin Lokasi GPS Diperlukan",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Aktifkan GPS untuk membaca kecepatan & jarak tempuh motor matic Anda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("grant_gps_permission_button")
                ) {
                    Text("Aktifkan")
                }
            }
        }
        GpsSignalStatus.GPS_DISABLED -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpeedDangerRed.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                    .border(1.dp, SpeedDangerRed, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.GpsOff,
                    contentDescription = "GPS Mati",
                    tint = SpeedDangerRed,
                    modifier = Modifier.size(26.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Layanan Lokasi (GPS) Mati",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Nyalakan GPS perangkat agar speedometer berfungsi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Button(
                    onClick = onOpenLocationSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = SpeedDangerRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("open_location_settings_button")
                ) {
                    Text("Buka")
                }
            }
        }
        GpsSignalStatus.SEARCHING -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GpsSearchingAmber.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .border(1.dp, GpsSearchingAmber.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationSearching,
                    contentDescription = "Mencari satelit GPS",
                    tint = GpsSearchingAmber,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Mencari sinyal satelit GPS…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GpsSearchingAmber
                )
            }
        }
        else -> Unit
    }
}

@Composable
private fun QuickCockpitChipsRow(
    speedUnit: SpeedUnit,
    speedProfile: SpeedProfile,
    warningEnabled: Boolean,
    warningLimitInUnit: Int,
    onCycleUnit: () -> Unit,
    onCycleProfile: () -> Unit,
    onToggleWarning: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickChip(
            icon = Icons.Rounded.SwapHoriz,
            label = "UNIT",
            value = speedUnit.label,
            active = true,
            onClick = onCycleUnit,
            testTag = "quick_unit_chip",
            modifier = Modifier.weight(1f)
        )
        QuickChip(
            icon = Icons.Rounded.Speed,
            label = "MATIC",
            value = "${speedProfile.maxGaugeValue}",
            active = true,
            onClick = onCycleProfile,
            testTag = "quick_profile_chip",
            modifier = Modifier.weight(1f)
        )
        QuickChip(
            icon = if (warningEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
            label = "ALARM",
            value = if (warningEnabled) "$warningLimitInUnit" else "MATI",
            active = warningEnabled,
            onClick = onToggleWarning,
            testTag = "quick_warning_chip",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) CockpitSurface else CockpitSurfaceVariant)
            .border(
                1.dp,
                if (active) accent.copy(alpha = 0.35f) else CockpitBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) accent else TextMuted,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (active) TextPrimary else TextSecondary
            )
        }
    }
}
