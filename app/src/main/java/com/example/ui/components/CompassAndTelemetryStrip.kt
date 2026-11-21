package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GpsNotFixed
import androidx.compose.material.icons.rounded.GpsOff
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.SatelliteAlt
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.GpsSignalStatus
import com.example.data.model.GpsTelemetry
import com.example.data.model.JukenConnectionState
import com.example.data.model.JukenEcuTelemetry
import com.example.data.model.OrientationMode
import com.example.data.model.SpeedUnit
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.GpsLockedGreen
import com.example.ui.theme.GpsOffGray
import com.example.ui.theme.GpsSearchingAmber
import com.example.ui.theme.SpeedDangerRed
import com.example.ui.theme.SpeedWarningAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CockpitHeaderBar(
    telemetry: GpsTelemetry,
    orientationMode: OrientationMode,
    jukenState: JukenConnectionState,
    onCycleOrientation: () -> Unit,
    onOpenJukenTab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (gpsColor, gpsLabel, gpsIcon) = when (telemetry.signalStatus) {
        GpsSignalStatus.LOCKED -> Triple(
            GpsLockedGreen,
            "GPS ±${telemetry.accuracyMeters.roundToInt()}m",
            Icons.Rounded.GpsFixed
        )
        GpsSignalStatus.WEAK_FIX -> Triple(
            GpsSearchingAmber,
            "GPS ±${telemetry.accuracyMeters.roundToInt()}m",
            Icons.Rounded.GpsNotFixed
        )
        GpsSignalStatus.SEARCHING -> Triple(
            GpsSearchingAmber,
            "Mencari GPS…",
            Icons.Rounded.GpsNotFixed
        )
        GpsSignalStatus.GPS_DISABLED -> Triple(
            SpeedDangerRed,
            "GPS Mati",
            Icons.Rounded.GpsOff
        )
        GpsSignalStatus.PERMISSION_NEEDED -> Triple(
            GpsOffGray,
            "Izin GPS",
            Icons.Rounded.GpsOff
        )
    }

    val (ecuColor, ecuLabel, ecuIcon) = when (jukenState) {
        JukenConnectionState.CONNECTED -> Triple(GpsLockedGreen, "JUKEN 5+", Icons.Rounded.BluetoothConnected)
        JukenConnectionState.CONNECTING, JukenConnectionState.SCANNING ->
            Triple(GpsSearchingAmber, "ECU…", Icons.Rounded.BluetoothSearching)
        else -> Triple(TextSecondary, "ECU Off", Icons.Rounded.Memory)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isCompactWidth = maxWidth < 410.dp

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand + GPS Signal Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isCompactWidth) "SP" else "SPEED PRO",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(CockpitSurfaceVariant, RoundedCornerShape(50))
                        .border(1.dp, gpsColor.copy(alpha = 0.45f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = gpsIcon,
                        contentDescription = gpsLabel,
                        tint = gpsColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = gpsLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = gpsColor,
                        maxLines = 1
                    )
                }
            }

            // Satellites + Battery + Orientation + Juken 5+ Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .background(CockpitSurface, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SatelliteAlt,
                        contentDescription = "Satelit GPS",
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${telemetry.satellitesUsed}/${telemetry.satellitesVisible}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                }

                if (!isCompactWidth) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .background(CockpitSurface, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = if (telemetry.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                            contentDescription = "Baterai ${telemetry.batteryPercent}%",
                            tint = if (telemetry.batteryPercent <= 20) SpeedDangerRed else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${telemetry.batteryPercent}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CockpitSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onCycleOrientation)
                        .testTag("orientation_toggle_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ScreenRotation,
                            contentDescription = "Ganti Orientasi Layar (${orientationMode.label})",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        if (!isCompactWidth) {
                            Text(
                                text = when (orientationMode) {
                                    OrientationMode.LANDSCAPE -> "LAND"
                                    OrientationMode.PORTRAIT -> "PORT"
                                    OrientationMode.AUTO -> "AUTO"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (jukenState == JukenConnectionState.CONNECTED) {
                        GpsLockedGreen.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        ecuColor.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenJukenTab)
                        .testTag("header_juken_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = ecuIcon,
                            contentDescription = "Buka Koneksi Juken 5+",
                            tint = ecuColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = ecuLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ecuColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JukenLiveStrip(
    jukenState: JukenConnectionState,
    ecuTelemetry: JukenEcuTelemetry,
    revLimiterRpm: Int,
    onOpenJukenClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConnected = jukenState == JukenConnectionState.CONNECTED
    val rpmFraction = if (revLimiterRpm > 0) (ecuTelemetry.rpm.toFloat() / revLimiterRpm).coerceIn(0f, 1f) else 0f
    val rpmBarColor = when {
        rpmFraction >= 0.9f -> SpeedDangerRed
        rpmFraction >= 0.72f -> SpeedWarningAmber
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CockpitSurface)
            .border(
                1.dp,
                if (isConnected) GpsLockedGreen.copy(alpha = 0.5f) else CockpitBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onOpenJukenClick)
            .padding(12.dp)
            .testTag("juken_live_strip"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Memory,
                    contentDescription = "ECU Juken 5+",
                    tint = if (isConnected) GpsLockedGreen else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isConnected) "JUKEN 5+ LIVE" else "ECU JUKEN 5+ • TERPUTUS",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isConnected) GpsLockedGreen else TextSecondary
                )
            }
            Text(
                text = if (isConnected) "${ecuTelemetry.rpm} RPM" else "Ketuk untuk hubungkan",
                style = MaterialTheme.typography.labelLarge,
                color = if (isConnected) rpmBarColor else MaterialTheme.colorScheme.primary
            )
        }

        // RPM Progress Bar
        LinearProgressIndicator(
            progress = { if (isConnected) rpmFraction else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = rpmBarColor,
            trackColor = CockpitSurfaceVariant,
            strokeCap = StrokeCap.Round
        )

        // TPS, AFR, AKI, SUHU Mini Telemetry Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            EcuMiniMetric(
                label = "TPS",
                value = if (isConnected && ecuTelemetry.hasLivePacket) {
                    "${ecuTelemetry.tpsPercent.roundToInt()}%"
                } else "--%",
            )
            EcuMiniMetric(
                label = "AFR",
                value = if (isConnected && ecuTelemetry.hasLivePacket && ecuTelemetry.afr > 0f) {
                    String.format(Locale.US, "%.1f", ecuTelemetry.afr)
                } else "--.-",
            )
            EcuMiniMetric(
                label = "AKI",
                value = if (isConnected && ecuTelemetry.hasLivePacket && ecuTelemetry.batteryVoltage > 0f) {
                    String.format(Locale.US, "%.1fV", ecuTelemetry.batteryVoltage)
                } else "--.-V",
                valueColor = if (isConnected && ecuTelemetry.batteryVoltage in 0.1f..11.8f) SpeedDangerRed else TextPrimary
            )
            EcuMiniMetric(
                label = "SUHU",
                value = if (isConnected && ecuTelemetry.hasLivePacket && ecuTelemetry.engineTempC > 0) {
                    "${ecuTelemetry.engineTempC}°C"
                } else "--°C",
            )
        }
    }
}

@Composable
private fun EcuMiniMetric(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = TextPrimary,
) {
    Column(
        modifier = Modifier
            .background(CockpitSurfaceVariant, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = valueColor
        )
    }
}

@Composable
fun CompassAndAltitudeStrip(
    telemetry: GpsTelemetry,
    speedUnit: SpeedUnit,
    modifier: Modifier = Modifier,
) {
    val animatedBearing by animateFloatAsState(
        targetValue = telemetry.bearingDegrees,
        label = "compass_bearing"
    )
    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CockpitSurface, RoundedCornerShape(16.dp))
            .border(1.dp, CockpitBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(CockpitSurfaceVariant, CircleShape)
                    .border(1.dp, CockpitBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(26.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    rotate(degrees = animatedBearing, pivot = center) {
                        val northPath = Path().apply {
                            moveTo(center.x, 2f)
                            lineTo(center.x - 5f, center.y)
                            lineTo(center.x + 5f, center.y)
                            close()
                        }
                        drawPath(northPath, color = SpeedDangerRed)

                        val southPath = Path().apply {
                            moveTo(center.x, size.height - 2f)
                            lineTo(center.x - 5f, center.y)
                            lineTo(center.x + 5f, center.y)
                            close()
                        }
                        drawPath(southPath, color = accent.copy(alpha = 0.65f))
                    }
                    drawCircle(color = TextPrimary, radius = 2.5f, center = center)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "KOMPAS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                val deg = ((telemetry.bearingDegrees % 360f) + 360f) % 360f
                Text(
                    text = if (telemetry.hasBearing) {
                        "${deg.roundToInt()}° ${bearingToCardinal(deg)}"
                    } else {
                        "---°"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Terrain,
                contentDescription = "Ketinggian",
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "ALTITUDE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                val altValue = speedUnit.convertAltitude(telemetry.altitudeMeters)
                Text(
                    text = if (telemetry.hasAltitude) "${altValue.roundToInt()} ${speedUnit.altitudeLabel}" else "-- ${speedUnit.altitudeLabel}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
        }
    }
}
