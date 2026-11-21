package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ActiveTripChannel
import com.example.data.model.LiveTripState
import com.example.data.model.SpeedUnit
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.GpsLockedGreen
import com.example.ui.theme.GpsSearchingAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun TripComputerPanel(
    liveTrip: LiveTripState,
    speedUnit: SpeedUnit,
    onToggleChannel: () -> Unit,
    onToggleTracking: () -> Unit,
    onSaveTrip: () -> Unit,
    onResetTrip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeLabel = if (liveTrip.activeChannel == ActiveTripChannel.TRIP_A) "TRIP A" else "TRIP B"
    val tripDist = speedUnit.convertDistance(liveTrip.activeTripDistanceMeters)
    val odoDist = speedUnit.convertDistance(liveTrip.odometerMeters)
    val maxSpd = speedUnit.convertSpeed(liveTrip.maxSpeedMps).roundToInt()
    val avgSpd = speedUnit.convertSpeed(liveTrip.averageSpeedMps).roundToInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CockpitSurface, RoundedCornerShape(18.dp))
            .border(1.dp, CockpitBorder, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top 3 telemetry cells: TRIP A/B (clickable), ODO, TIME
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tappable Trip A/B Switcher
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(onClick = onToggleChannel)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("trip_channel_toggle")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "TAP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = "${formatDistance(tripDist)} ${speedUnit.distanceLabel}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }

            // Odometer
            TelemetryCell(
                label = "ODO TOTAL",
                value = "${formatDistance(odoDist, 1)} ${speedUnit.distanceLabel}",
                modifier = Modifier.weight(1f)
            )

            // Moving Time
            TelemetryCell(
                label = "WAKTU",
                value = formatDurationHms(liveTrip.movingTimeMillis),
                modifier = Modifier.weight(1f)
            )
        }

        // Bottom 2 cells + action buttons: MAX, AVG, Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TelemetryCell(
                label = "MAX",
                value = "$maxSpd ${speedUnit.label}",
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            TelemetryCell(
                label = "AVG",
                value = "$avgSpd ${speedUnit.label}",
                modifier = Modifier.weight(1f)
            )
            TelemetryCell(
                label = "STATUS",
                value = if (liveTrip.isTrackingTrip) "AKTIF" else "JEDA",
                valueColor = if (liveTrip.isTrackingTrip) GpsLockedGreen else GpsSearchingAmber,
                modifier = Modifier.weight(0.9f)
            )
        }

        // Trip Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onToggleTracking,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("trip_pause_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (liveTrip.isTrackingTrip) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (liveTrip.isTrackingTrip) "Jeda Trip" else "Lanjut Trip",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (liveTrip.isTrackingTrip) "Jeda" else "Lanjut",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            FilledTonalButton(
                onClick = onSaveTrip,
                modifier = Modifier
                    .weight(1.1f)
                    .height(44.dp)
                    .testTag("trip_save_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.BookmarkAdd,
                    contentDescription = "Simpan Trip",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Simpan",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            OutlinedButton(
                onClick = onResetTrip,
                modifier = Modifier
                    .weight(0.9f)
                    .height(44.dp)
                    .testTag("trip_reset_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = "Reset Trip",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun TelemetryCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = TextPrimary,
) {
    Column(
        modifier = modifier
            .background(CockpitSurfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
            maxLines = 1
        )
    }
}
