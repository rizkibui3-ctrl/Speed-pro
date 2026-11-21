package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.SpeedProSettings
import com.example.data.model.CockpitAccent
import com.example.data.model.OrientationMode
import com.example.data.model.SpeedProfile
import com.example.data.model.SpeedUnit
import com.example.ui.components.convertWarningKmhToUnit
import com.example.ui.components.formatDistance
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: SpeedProSettings,
    odometerMeters: Double,
    onSetUnit: (SpeedUnit) -> Unit,
    onSetProfile: (SpeedProfile) -> Unit,
    onSetAccent: (CockpitAccent) -> Unit,
    onSetOrientationMode: (OrientationMode) -> Unit,
    onSetWarningEnabled: (Boolean) -> Unit,
    onSetWarningLimitKmh: (Int) -> Unit,
    onSetWarningSound: (Boolean) -> Unit,
    onSetWarningVibrate: (Boolean) -> Unit,
    onSetKeepScreenOn: (Boolean) -> Unit,
    onSetShowCompassAlt: (Boolean) -> Unit,
    onSetOdometerKm: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showOdoDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Pengaturan Speed Pro Matic",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Text(
                text = "Kustomisasi kokpit motor matic, skala speedometer, & batas kecepatan.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Screen Orientation
        item {
            SettingsCard(title = "Orientasi Layar") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OrientationMode.entries.forEach { mode ->
                        val selected = settings.orientationMode == mode
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
                                .clickable { onSetOrientationMode(mode) }
                                .padding(vertical = 10.dp)
                                .testTag("orientation_option_${mode.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.primary else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Unit Selection
        item {
            SettingsCard(title = "Satuan Kecepatan") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpeedUnit.entries.forEach { unit ->
                        val selected = settings.speedUnit == unit
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
                                .clickable { onSetUnit(unit) }
                                .padding(vertical = 10.dp)
                                .testTag("unit_option_${unit.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = unit.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.primary else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Motor Matic Scale Profile Selection
        item {
            SettingsCard(title = "Profil Skala Motor Matic") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpeedProfile.entries.forEach { profile ->
                        ProfileChip(
                            profile = profile,
                            selected = settings.speedProfile == profile,
                            onClick = { onSetProfile(profile) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Cockpit Accent Color
        item {
            SettingsCard(title = "Tema Warna Kokpit") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CockpitAccent.entries.forEach { accent ->
                        val selected = settings.cockpitAccent == accent
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) accent.primary.copy(alpha = 0.14f)
                                    else CockpitSurfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (selected) accent.primary else CockpitBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onSetAccent(accent) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("accent_option_${accent.name}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(accent.primary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = accent.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selected) accent.primary else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Speed Warning
        item {
            val warningUnitVal = convertWarningKmhToUnit(
                settings.speedWarningLimitKmh,
                settings.speedUnit.speedMultiplierFromMps
            )
            SettingsCard(title = "Peringatan Batas Kecepatan") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsSwitchRow(
                        label = "Aktifkan Peringatan Kecepatan",
                        checked = settings.speedWarningEnabled,
                        onCheckedChange = onSetWarningEnabled,
                        testTag = "warning_enabled_switch"
                    )
                    if (settings.speedWarningEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Batas Maksimal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = "$warningUnitVal ${settings.speedUnit.label} (${settings.speedWarningLimitKmh} km/h)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = settings.speedWarningLimitKmh.toFloat(),
                            onValueChange = { onSetWarningLimitKmh(it.roundToInt()) },
                            valueRange = 30f..200f,
                            steps = 16,
                            modifier = Modifier.testTag("warning_limit_slider")
                        )
                        SettingsSwitchRow(
                            label = "Suara Beep Peringatan",
                            checked = settings.speedWarningSoundEnabled,
                            onCheckedChange = onSetWarningSound,
                            testTag = "warning_sound_switch"
                        )
                        SettingsSwitchRow(
                            label = "Getar Haptik",
                            checked = settings.speedWarningVibrateEnabled,
                            onCheckedChange = onSetWarningVibrate,
                            testTag = "warning_vibrate_switch"
                        )
                    }
                }
            }
        }

        // Display
        item {
            SettingsCard(title = "Tampilan Layar Stang") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsSwitchRow(
                        label = "Layar Tetap Menyala Saat Berkendara",
                        checked = settings.keepScreenOn,
                        onCheckedChange = onSetKeepScreenOn,
                        testTag = "keep_screen_on_switch"
                    )
                    SettingsSwitchRow(
                        label = "Tampilkan Kompas & Ketinggian (Touring)",
                        checked = settings.showCompassAndAltitude,
                        onCheckedChange = onSetShowCompassAlt,
                        testTag = "show_compass_switch"
                    )
                }
            }
        }

        // Odometer Calibration
        item {
            val odoDisplay = settings.speedUnit.convertDistance(odometerMeters)
            SettingsCard(title = "Kalibrasi Odometer Motor") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Odometer Saat Ini",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "${formatDistance(odoDisplay, 1)} ${settings.speedUnit.distanceLabel}",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                    OutlinedButton(
                        onClick = { showOdoDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("edit_odometer_button")
                    ) {
                        Text("Ubah")
                    }
                }
            }
        }
    }

    if (showOdoDialog) {
        var inputText by remember {
            mutableStateOf(String.format(java.util.Locale.US, "%.1f", odometerMeters / 1000.0))
        }
        AlertDialog(
            onDismissRequest = { showOdoDialog = false },
            title = { Text("Atur Odometer (km)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sesuaikan total kilometer motor matic Anda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text("Total km") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val km = inputText.toDoubleOrNull() ?: 0.0
                    onSetOdometerKm(km)
                    showOdoDialog = false
                }) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOdoDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CockpitSurface, RoundedCornerShape(18.dp))
            .border(1.dp, CockpitBorder, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
        content()
    }
}

@Composable
private fun ProfileChip(
    profile: SpeedProfile,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
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
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .testTag("profile_option_${profile.name}"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = profile.title,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
