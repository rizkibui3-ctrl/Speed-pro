package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.EcuTelemetrySample
import com.example.data.model.JukenBluetoothDevice
import com.example.data.model.JukenConnectionState
import com.example.data.model.JukenEcuTelemetry
import com.example.data.model.JukenMapSlot
import com.example.data.model.JukenTuningConfig
import com.example.ui.components.AfrSpectrumCard
import com.example.ui.components.CoolantTempGaugeCard
import com.example.ui.components.EcuLiveTelemetryChart
import com.example.ui.components.RpmTachometerGauge
import com.example.ui.components.ThrottlePositionGaugeCard
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.GpsLockedGreen
import com.example.ui.theme.GpsSearchingAmber
import com.example.ui.theme.SpeedDangerRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.roundToInt

private enum class JukenSubTab(val label: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Dashboard ECU", Icons.Rounded.Dashboard, "juken_subtab_dashboard"),
    TUNING("Live Tuning", Icons.Rounded.Tune, "juken_subtab_tuning"),
    CONNECTION("Koneksi BT", Icons.Rounded.Bluetooth, "juken_subtab_connection"),
}

@Composable
fun JukenEcuScreen(
    connectionState: JukenConnectionState,
    statusMessage: String,
    connectedDevice: JukenBluetoothDevice?,
    discoveredDevices: List<JukenBluetoothDevice>,
    ecuTelemetry: JukenEcuTelemetry,
    ecuHistory: List<EcuTelemetrySample>,
    savedTuning: JukenTuningConfig,
    lastDeviceName: String,
    lastDeviceAddress: String,
    onRequestBluetoothPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onStartScan: () -> Unit,
    onConnectDevice: (JukenBluetoothDevice) -> Unit,
    onDisconnect: () -> Unit,
    onApplyLiveTuning: (JukenTuningConfig, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeSubTab by remember { mutableStateOf(JukenSubTab.DASHBOARD) }
    var draftTuning by remember(savedTuning) { mutableStateOf(savedTuning) }
    val isConnected = connectionState == JukenConnectionState.CONNECTED

    val (stateColor, stateIcon) = when (connectionState) {
        JukenConnectionState.CONNECTED -> GpsLockedGreen to Icons.Rounded.BluetoothConnected
        JukenConnectionState.SCANNING, JukenConnectionState.CONNECTING ->
            GpsSearchingAmber to Icons.Rounded.BluetoothSearching
        JukenConnectionState.BLUETOOTH_OFF, JukenConnectionState.ERROR ->
            SpeedDangerRed to Icons.Rounded.BluetoothDisabled
        else -> MaterialTheme.colorScheme.primary to Icons.Rounded.Bluetooth
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Header + 3-Segment Mode Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = stateIcon,
                    contentDescription = "Status ECU Juken 5+",
                    tint = stateColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isConnected) "JUKEN 5+ • ${connectedDevice?.name ?: "Terhubung"}" else "JUKEN 5+ ECU",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1
                )
            }

            // Connection shortcut pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(50))
                    .background(stateColor.copy(alpha = 0.14f))
                    .border(1.dp, stateColor.copy(alpha = 0.55f), RoundedCornerShape(50))
                    .clickable { activeSubTab = JukenSubTab.CONNECTION }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("juken_status_pill")
            ) {
                Text(
                    text = if (isConnected) "TERHUBUNG" else "HUBUNGKAN ECU",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = stateColor
                )
            }
        }

        // Sub-Tab Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CockpitSurface, RoundedCornerShape(14.dp))
                .border(1.dp, CockpitBorder, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            JukenSubTab.entries.forEach { tab ->
                val selected = activeSubTab == tab
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primaryContainer
                            else CockpitSurface
                        )
                        .clickable { activeSubTab = tab }
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .testTag(tab.tag),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) MaterialTheme.colorScheme.primary else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.primary else TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }

        // Sub-Tab Content
        when (activeSubTab) {
            JukenSubTab.DASHBOARD -> {
                JukenRealTimeDashboardView(
                    isConnected = isConnected,
                    connectionState = connectionState,
                    statusMessage = statusMessage,
                    ecuTelemetry = ecuTelemetry,
                    ecuHistory = ecuHistory,
                    tuningConfig = draftTuning,
                    onGoToConnect = { activeSubTab = JukenSubTab.CONNECTION },
                    modifier = Modifier.weight(1f)
                )
            }

            JukenSubTab.TUNING -> {
                JukenTuningView(
                    draftTuning = draftTuning,
                    isConnected = isConnected,
                    onDraftChange = { draftTuning = it },
                    onApplyLiveTuning = onApplyLiveTuning,
                    modifier = Modifier.weight(1f)
                )
            }

            JukenSubTab.CONNECTION -> {
                JukenConnectionView(
                    connectionState = connectionState,
                    statusMessage = statusMessage,
                    connectedDevice = connectedDevice,
                    discoveredDevices = discoveredDevices,
                    lastDeviceName = lastDeviceName,
                    lastDeviceAddress = lastDeviceAddress,
                    onRequestBluetoothPermissions = onRequestBluetoothPermissions,
                    onEnableBluetooth = onEnableBluetooth,
                    onStartScan = onStartScan,
                    onConnectDevice = onConnectDevice,
                    onDisconnect = onDisconnect,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun JukenRealTimeDashboardView(
    isConnected: Boolean,
    connectionState: JukenConnectionState,
    statusMessage: String,
    ecuTelemetry: JukenEcuTelemetry,
    ecuHistory: List<EcuTelemetrySample>,
    tuningConfig: JukenTuningConfig,
    onGoToConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLive = isConnected && ecuTelemetry.hasLivePacket

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 560.dp || maxWidth > maxHeight

        if (isWide) {
            // Adaptive Landscape ECU Instrument Cluster
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    RpmTachometerGauge(
                        rpm = ecuTelemetry.rpm,
                        revLimiterRpm = tuningConfig.revLimiterRpm,
                        activeMap = tuningConfig.activeMap,
                        isConnected = isConnected,
                        hasLivePacket = ecuTelemetry.hasLivePacket,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isConnected) {
                        EcuConnectCalloutBanner(
                            statusMessage = statusMessage,
                            isConnecting = connectionState == JukenConnectionState.CONNECTING,
                            onGoToConnect = onGoToConnect
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThrottlePositionGaugeCard(
                            tpsPercent = ecuTelemetry.tpsPercent,
                            isLive = isLive,
                            modifier = Modifier.weight(1f)
                        )
                        CoolantTempGaugeCard(
                            tempC = ecuTelemetry.engineTempC,
                            isLive = isLive,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    AfrSpectrumCard(
                        afr = ecuTelemetry.afr,
                        isLive = isLive
                    )

                    EcuSecondaryMetricsRow(
                        ecuTelemetry = ecuTelemetry,
                        isLive = isLive
                    )

                    EcuLiveTelemetryChart(
                        samples = ecuHistory,
                        revLimiterRpm = tuningConfig.revLimiterRpm
                    )
                }
            }
        } else {
            // Portrait ECU Instrument Cluster
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isConnected) {
                    EcuConnectCalloutBanner(
                        statusMessage = statusMessage,
                        isConnecting = connectionState == JukenConnectionState.CONNECTING,
                        onGoToConnect = onGoToConnect
                    )
                }

                RpmTachometerGauge(
                    rpm = ecuTelemetry.rpm,
                    revLimiterRpm = tuningConfig.revLimiterRpm,
                    activeMap = tuningConfig.activeMap,
                    isConnected = isConnected,
                    hasLivePacket = ecuTelemetry.hasLivePacket,
                    modifier = Modifier
                        .fillMaxWidth(0.84f)
                        .widthIn(max = 310.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThrottlePositionGaugeCard(
                        tpsPercent = ecuTelemetry.tpsPercent,
                        isLive = isLive,
                        modifier = Modifier.weight(1f)
                    )
                    CoolantTempGaugeCard(
                        tempC = ecuTelemetry.engineTempC,
                        isLive = isLive,
                        modifier = Modifier.weight(1f)
                    )
                }

                AfrSpectrumCard(
                    afr = ecuTelemetry.afr,
                    isLive = isLive
                )

                EcuSecondaryMetricsRow(
                    ecuTelemetry = ecuTelemetry,
                    isLive = isLive
                )

                EcuLiveTelemetryChart(
                    samples = ecuHistory,
                    revLimiterRpm = tuningConfig.revLimiterRpm
                )

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun EcuConnectCalloutBanner(
    statusMessage: String,
    isConnecting: Boolean,
    onGoToConnect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Menunggu Koneksi ECU Juken 5+",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onGoToConnect,
            enabled = !isConnecting,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("ecu_banner_connect_button")
        ) {
            Text(if (isConnecting) "Menyambung…" else "Hubungkan")
        }
    }
}

@Composable
private fun EcuSecondaryMetricsRow(
    ecuTelemetry: JukenEcuTelemetry,
    isLive: Boolean,
) {
    val voltageColor = when {
        !isLive || ecuTelemetry.batteryVoltage <= 0f -> TextPrimary
        ecuTelemetry.batteryVoltage < 11.8f || ecuTelemetry.batteryVoltage > 15.2f -> SpeedDangerRed
        else -> GpsLockedGreen
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EcuSecondaryTile(
            icon = Icons.Rounded.BatteryChargingFull,
            label = "AKI / KIPROK",
            value = if (isLive && ecuTelemetry.batteryVoltage > 0f) {
                String.format(Locale.US, "%.1f V", ecuTelemetry.batteryVoltage)
            } else "--.- V",
            valueColor = voltageColor,
            modifier = Modifier.weight(1f)
        )
        EcuSecondaryTile(
            icon = Icons.Rounded.Bolt,
            label = "TIMING API",
            value = if (isLive) {
                String.format(Locale.US, "%.1f°", ecuTelemetry.ignitionTimingDeg)
            } else "--°",
            modifier = Modifier.weight(1f)
        )
        EcuSecondaryTile(
            icon = Icons.Rounded.LocalGasStation,
            label = "KOREKSI BBM",
            value = if (isLive) {
                val sign = if (ecuTelemetry.fuelCorrectionPercent > 0) "+" else ""
                "$sign${ecuTelemetry.fuelCorrectionPercent}%"
            } else "--%",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EcuSecondaryTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = TextPrimary,
) {
    Column(
        modifier = modifier
            .background(CockpitSurface, RoundedCornerShape(14.dp))
            .border(1.dp, CockpitBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
            maxLines = 1
        )
    }
}

@Composable
private fun JukenConnectionView(
    connectionState: JukenConnectionState,
    statusMessage: String,
    connectedDevice: JukenBluetoothDevice?,
    discoveredDevices: List<JukenBluetoothDevice>,
    lastDeviceName: String,
    lastDeviceAddress: String,
    onRequestBluetoothPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onStartScan: () -> Unit,
    onConnectDevice: (JukenBluetoothDevice) -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConnected = connectionState == JukenConnectionState.CONNECTED
    val (stateColor, stateIcon) = when (connectionState) {
        JukenConnectionState.CONNECTED -> GpsLockedGreen to Icons.Rounded.BluetoothConnected
        JukenConnectionState.SCANNING, JukenConnectionState.CONNECTING ->
            GpsSearchingAmber to Icons.Rounded.BluetoothSearching
        JukenConnectionState.BLUETOOTH_OFF, JukenConnectionState.ERROR ->
            SpeedDangerRed to Icons.Rounded.BluetoothDisabled
        else -> MaterialTheme.colorScheme.primary to Icons.Rounded.Bluetooth
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CockpitSurface, RoundedCornerShape(18.dp))
                    .border(1.dp, stateColor.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = stateIcon,
                            contentDescription = "Status Bluetooth ECU",
                            tint = stateColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = connectedDevice?.name ?: "Modul Bluetooth Juken 5+",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = stateColor
                            )
                        }
                    }

                    if (connectionState == JukenConnectionState.SCANNING ||
                        connectionState == JukenConnectionState.CONNECTING
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = stateColor
                        )
                    }
                }

                when (connectionState) {
                    JukenConnectionState.PERMISSION_NEEDED -> {
                        Button(
                            onClick = onRequestBluetoothPermissions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("juken_grant_bt_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Izinkan Bluetooth")
                        }
                    }
                    JukenConnectionState.BLUETOOTH_OFF -> {
                        Button(
                            onClick = onEnableBluetooth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("juken_enable_bt_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Nyalakan Bluetooth")
                        }
                    }
                    JukenConnectionState.CONNECTED -> {
                        OutlinedButton(
                            onClick = onDisconnect,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("juken_disconnect_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SpeedDangerRed)
                        ) {
                            Text("Putuskan Koneksi")
                        }
                    }
                    else -> {
                        Button(
                            onClick = onStartScan,
                            enabled = connectionState != JukenConnectionState.SCANNING &&
                                connectionState != JukenConnectionState.CONNECTING,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("juken_scan_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Pindai Perangkat ECU",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (connectionState == JukenConnectionState.SCANNING) "Memindai…"
                                else "Pindai Modul Juken 5+"
                            )
                        }
                    }
                }

                if (!isConnected && lastDeviceAddress.isNotBlank()) {
                    val quickDevice = discoveredDevices.firstOrNull { it.address == lastDeviceAddress }
                    if (quickDevice != null) {
                        FilledTonalButton(
                            onClick = { onConnectDevice(quickDevice) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("juken_quick_reconnect_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Sambung Ulang: $lastDeviceName")
                        }
                    }
                }
            }
        }

        if (discoveredDevices.isNotEmpty()) {
            item {
                Text(
                    text = "Perangkat Bluetooth Tersedia (${discoveredDevices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
            items(discoveredDevices, key = { it.address }) { dev ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CockpitSurface)
                        .border(
                            1.dp,
                            if (dev.isLikelyJuken) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else CockpitBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onConnectDevice(dev) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("juken_device_${dev.address}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = dev.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            if (dev.isLikelyJuken) {
                                Text(
                                    text = "ECU",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${dev.address} • ${dev.transportType.label}" +
                                if (dev.isPaired) " • Terpasang" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                    }
                    Text(
                        text = "Hubung",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun JukenTuningView(
    draftTuning: JukenTuningConfig,
    isConnected: Boolean,
    onDraftChange: (JukenTuningConfig) -> Unit,
    onApplyLiveTuning: (JukenTuningConfig, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CockpitSurface, RoundedCornerShape(18.dp))
                    .border(1.dp, CockpitBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Tuning Juken 5+ (Matic)",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isConnected) "ECU Online" else "Mode Offline",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isConnected) GpsLockedGreen else TextMuted
                    )
                }

                // Dual Map Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    JukenMapSlot.entries.forEach { slot ->
                        val selected = draftTuning.activeMap == slot
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else CockpitSurfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else CockpitBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onDraftChange(draftTuning.copy(activeMap = slot)) }
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                .testTag("juken_map_${slot.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = slot.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.primary else TextSecondary
                            )
                        }
                    }
                }

                TuningSlider(
                    label = "BBM Low (Tarikan Awal CVT)",
                    valueText = formatSignedPercent(draftTuning.fuelLowPercent),
                    value = draftTuning.fuelLowPercent.toFloat(),
                    range = -30f..30f,
                    onValueChange = { onDraftChange(draftTuning.copy(fuelLowPercent = it.roundToInt())) }
                )
                TuningSlider(
                    label = "BBM Mid (Putaran Menengah)",
                    valueText = formatSignedPercent(draftTuning.fuelMidPercent),
                    value = draftTuning.fuelMidPercent.toFloat(),
                    range = -30f..30f,
                    onValueChange = { onDraftChange(draftTuning.copy(fuelMidPercent = it.roundToInt())) }
                )
                TuningSlider(
                    label = "BBM High (Top Speed)",
                    valueText = formatSignedPercent(draftTuning.fuelHighPercent),
                    value = draftTuning.fuelHighPercent.toFloat(),
                    range = -30f..30f,
                    onValueChange = { onDraftChange(draftTuning.copy(fuelHighPercent = it.roundToInt())) }
                )
                TuningSlider(
                    label = "Timing Pengapian",
                    valueText = "${if (draftTuning.ignitionOffsetDeg > 0) "+" else ""}${draftTuning.ignitionOffsetDeg}°",
                    value = draftTuning.ignitionOffsetDeg.toFloat(),
                    range = -10f..10f,
                    onValueChange = { onDraftChange(draftTuning.copy(ignitionOffsetDeg = it.roundToInt())) }
                )
                TuningSlider(
                    label = "Rev Limiter (Pengaman RPM)",
                    valueText = "${draftTuning.revLimiterRpm} RPM",
                    value = draftTuning.revLimiterRpm.toFloat(),
                    range = 8000f..13500f,
                    onValueChange = {
                        val stepped = (it / 250f).roundToInt() * 250
                        onDraftChange(draftTuning.copy(revLimiterRpm = stepped))
                    }
                )
                TuningSlider(
                    label = "Langsam / Idle RPM",
                    valueText = "${draftTuning.idleRpmTarget} RPM",
                    value = draftTuning.idleRpmTarget.toFloat(),
                    range = 1200f..2200f,
                    onValueChange = {
                        val stepped = (it / 50f).roundToInt() * 50
                        onDraftChange(draftTuning.copy(idleRpmTarget = stepped))
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CockpitSurfaceVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mode JJS (Jalan-Jalan Sore)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Haluskan bukaan gas awal (2–40% TPS) untuk santai/macet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = draftTuning.jjsEnabled,
                        onCheckedChange = { onDraftChange(draftTuning.copy(jjsEnabled = it)) },
                        modifier = Modifier.testTag("juken_jjs_switch")
                    )
                }

                if (draftTuning.jjsEnabled) {
                    TuningSlider(
                        label = "Koreksi BBM JJS",
                        valueText = formatSignedPercent(draftTuning.jjsFuelPercent),
                        value = draftTuning.jjsFuelPercent.toFloat(),
                        range = -15f..15f,
                        onValueChange = { onDraftChange(draftTuning.copy(jjsFuelPercent = it.roundToInt())) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onApplyLiveTuning(draftTuning, false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("juken_apply_live_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Terapkan Live",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Terapkan Live")
                    }

                    Button(
                        onClick = { onApplyLiveTuning(draftTuning, true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("juken_save_ecu_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Save,
                            contentDescription = "Simpan ke ECU",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan ke ECU")
                    }
                }
            }
        }
    }
}

@Composable
private fun TuningSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}

private fun formatSignedPercent(value: Int): String {
    return if (value > 0) "+$value%" else "$value%"
}
