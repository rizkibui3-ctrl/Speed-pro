package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AccelerationRunEntity
import com.example.data.model.AccelerationPhase
import com.example.data.model.AccelerationTimerState
import com.example.data.model.GpsTelemetry
import com.example.data.model.SpeedUnit
import com.example.ui.components.formatAccelSeconds
import com.example.ui.components.formatDateTime
import com.example.ui.theme.ChakraPetchFamily
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
fun PerformanceScreen(
    telemetry: GpsTelemetry,
    accelState: AccelerationTimerState,
    speedUnit: SpeedUnit,
    savedRuns: List<AccelerationRunEntity>,
    onArmTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onFinishAndSave: () -> Unit,
    onDeleteRun: (Long) -> Unit,
    onClearRuns: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val currentSpeed = speedUnit.convertSpeed(telemetry.speedMps).roundToInt()

    val (phaseLabel, phaseColor) = when (accelState.phase) {
        AccelerationPhase.IDLE -> "SIAP — Tekan Siapkan Timer untuk tes tarikan CVT / 201m" to TextSecondary
        AccelerationPhase.ARMED -> "TERKUNCI — Buka gas (>2.5 km/h) untuk start otomatis" to GpsSearchingAmber
        AccelerationPhase.RUNNING -> "BERJALAN — Mengukur akselerasi & sprint 201m…" to GpsLockedGreen
        AccelerationPhase.COMPLETED -> "SELESAI — Hasil tes matic tercatat" to accent
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Tes Akselerasi & 201m Matic",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Text(
                text = "Ukur tarikan CVT 0–60, 0–80, 0–100 km/h & jarak sprint 201m (+ Peak RPM ECU).",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Active Timer Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CockpitSurface, RoundedCornerShape(20.dp))
                    .border(1.dp, phaseColor.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = "Status timer akselerasi",
                            tint = phaseColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = accelState.phase.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = phaseColor
                        )
                    }
                    Text(
                        text = "$currentSpeed ${speedUnit.label}",
                        style = MaterialTheme.typography.titleMedium,
                        color = accent
                    )
                }

                Text(
                    text = phaseLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = formatAccelSeconds(accelState.elapsedMillis),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = ChakraPetchFamily,
                            fontSize = 48.sp,
                            lineHeight = 52.sp
                        ),
                        color = TextPrimary,
                        modifier = Modifier.testTag("accel_stopwatch_text")
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "JARAK • PEAK RPM",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = "${accelState.distanceMeters.roundToInt()} m • " +
                                if (accelState.peakRpm > 0) "${accelState.peakRpm} RPM" else "-- RPM",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                }

                // Split Grid (0-60, 0-80, 0-100, 201m)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SplitBox(
                        title = "0–60 km/h",
                        value = formatAccelSeconds(accelState.time0To60KmhMillis),
                        achieved = accelState.time0To60KmhMillis != null,
                        modifier = Modifier.weight(1f)
                    )
                    SplitBox(
                        title = "0–80 km/h",
                        value = formatAccelSeconds(accelState.time0To80KmhMillis),
                        achieved = accelState.time0To80KmhMillis != null,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SplitBox(
                        title = "0–100 km/h",
                        value = formatAccelSeconds(accelState.time0To100KmhMillis),
                        achieved = accelState.time0To100KmhMillis != null,
                        modifier = Modifier.weight(1f)
                    )
                    SplitBox(
                        title = "SPRINT 201m",
                        value = formatAccelSeconds(accelState.time201mMillis),
                        achieved = accelState.time201mMillis != null,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (accelState.phase == AccelerationPhase.IDLE || accelState.phase == AccelerationPhase.COMPLETED) {
                        Button(
                            onClick = onArmTimer,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("arm_accel_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Siapkan Timer",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Siapkan Timer")
                        }
                    } else {
                        if (accelState.phase == AccelerationPhase.RUNNING) {
                            Button(
                                onClick = onFinishAndSave,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("save_accel_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GpsLockedGreen)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Save,
                                    contentDescription = "Simpan Hasil",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Selesai & Simpan")
                            }
                        }
                        OutlinedButton(
                            onClick = onResetTimer,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("reset_accel_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = "Reset Timer",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Batal / Reset")
                        }
                    }
                }
            }
        }

        // Saved History Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rekor Akselerasi Matic (${savedRuns.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                if (savedRuns.isNotEmpty()) {
                    IconButton(
                        onClick = onClearRuns,
                        modifier = Modifier.testTag("clear_accel_runs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Hapus semua rekor",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }

        if (savedRuns.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CockpitSurface, RoundedCornerShape(16.dp))
                        .border(1.dp, CockpitBorder, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Flag,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Belum ada catatan tes 201m",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Tekan Siapkan Timer saat motor berhenti untuk menguji tarikan CVT atau settingan baru Juken 5+.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(savedRuns, key = { it.id }) { run ->
                AccelerationRunCard(
                    run = run,
                    speedUnit = speedUnit,
                    onDelete = { onDeleteRun(run.id) }
                )
            }
        }
    }
}

@Composable
private fun SplitBox(
    title: String,
    value: String,
    achieved: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .background(CockpitSurfaceVariant, RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (achieved) accent.copy(alpha = 0.6f) else CockpitBorder,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = if (achieved) accent else TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = if (achieved) TextPrimary else TextSecondary
        )
    }
}

@Composable
private fun AccelerationRunCard(
    run: AccelerationRunEntity,
    speedUnit: SpeedUnit,
    onDelete: () -> Unit,
) {
    val peak = speedUnit.convertSpeed(run.peakSpeedMps).roundToInt()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CockpitSurface, RoundedCornerShape(16.dp))
            .border(1.dp, CockpitBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = formatDateTime(run.timestampMillis),
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "0-60: ${formatAccelSeconds(run.time0To60KmhMillis)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Text(
                    text = "0-80: ${formatAccelSeconds(run.time0To80KmhMillis)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "201m: ${formatAccelSeconds(run.time201mMillis)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Text(
                    text = "Max: $peak ${speedUnit.label}" +
                        if (run.peakRpm > 0) " • ${run.peakRpm} RPM" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "Hapus catatan akselerasi",
                tint = TextMuted
            )
        }
    }
}
