package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.TripEntity
import com.example.data.model.SpeedUnit
import com.example.ui.components.formatDateTime
import com.example.ui.components.formatDistance
import com.example.ui.components.formatDurationHms
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun TripsScreen(
    trips: List<TripEntity>,
    speedUnit: SpeedUnit,
    onDeleteTrip: (Long) -> Unit,
    onClearTrips: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalDistMeters = trips.sumOf { it.distanceMeters }
    val allTimeMaxMps = trips.maxOfOrNull { it.maxSpeedMps } ?: 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Riwayat Perjalanan",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Log trip tersimpan lokal di perangkat Anda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                if (trips.isNotEmpty()) {
                    IconButton(
                        onClick = onClearTrips,
                        modifier = Modifier.testTag("clear_all_trips_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Hapus semua trip",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }

        // Summary Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    label = "TOTAL TRIP",
                    value = "${trips.size}",
                    modifier = Modifier.weight(0.8f)
                )
                SummaryCard(
                    label = "TOTAL JARAK",
                    value = "${formatDistance(speedUnit.convertDistance(totalDistMeters), 1)} ${speedUnit.distanceLabel}",
                    modifier = Modifier.weight(1.2f)
                )
                SummaryCard(
                    label = "REKOR MAX",
                    value = "${speedUnit.convertSpeed(allTimeMaxMps).roundToInt()} ${speedUnit.label}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (trips.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CockpitSurface, RoundedCornerShape(18.dp))
                        .border(1.dp, CockpitBorder, RoundedCornerShape(18.dp))
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Route,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Belum ada perjalanan tersimpan",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Gunakan tombol Simpan di panel Trip Dasbor setelah berkendara untuk mencatat statistik perjalanan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(trips, key = { it.id }) { trip ->
                TripHistoryCard(
                    trip = trip,
                    speedUnit = speedUnit,
                    onDelete = { onDeleteTrip(trip.id) }
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(CockpitSurface, RoundedCornerShape(14.dp))
            .border(1.dp, CockpitBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TripHistoryCard(
    trip: TripEntity,
    speedUnit: SpeedUnit,
    onDelete: () -> Unit,
) {
    val dist = speedUnit.convertDistance(trip.distanceMeters)
    val maxSpd = speedUnit.convertSpeed(trip.maxSpeedMps).roundToInt()
    val avgSpd = speedUnit.convertSpeed(trip.avgSpeedMps).roundToInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CockpitSurface, RoundedCornerShape(16.dp))
            .border(1.dp, CockpitBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = trip.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = formatDateTime(trip.endedAtMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Hapus trip ${trip.title}",
                    tint = TextMuted
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CockpitSurfaceVariant, RoundedCornerShape(12.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TripMetric("JARAK", "${formatDistance(dist)} ${speedUnit.distanceLabel}")
            TripMetric("DURASI", formatDurationHms(trip.movingTimeMillis))
            TripMetric("MAX", "$maxSpd ${speedUnit.label}")
            TripMetric("AVG", "$avgSpd ${speedUnit.label}")
        }

        if (trip.minAltitudeMeters != null && trip.maxAltitudeMeters != null) {
            val minAlt = speedUnit.convertAltitude(trip.minAltitudeMeters).roundToInt()
            val maxAlt = speedUnit.convertAltitude(trip.maxAltitudeMeters).roundToInt()
            Text(
                text = "Elevasi: $minAlt – $maxAlt ${speedUnit.altitudeLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun TripMetric(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary
        )
    }
}
