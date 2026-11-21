package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmberGT
import com.example.ui.theme.AmberGTDim
import com.example.ui.theme.CrimsonSport
import com.example.ui.theme.CrimsonSportDim
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanNeonDim
import com.example.ui.theme.EmeraldTrack
import com.example.ui.theme.EmeraldTrackDim

enum class SpeedUnit(
    val label: String,
    val distanceLabel: String,
    val altitudeLabel: String,
    val speedMultiplierFromMps: Float,
    val distanceMetersPerUnit: Double,
    val altitudeMultiplierFromMeters: Float,
) {
    KMH(
        label = "KM/H",
        distanceLabel = "km",
        altitudeLabel = "m",
        speedMultiplierFromMps = 3.6f,
        distanceMetersPerUnit = 1000.0,
        altitudeMultiplierFromMeters = 1f,
    ),
    MPH(
        label = "MPH",
        distanceLabel = "mi",
        altitudeLabel = "ft",
        speedMultiplierFromMps = 2.2369363f,
        distanceMetersPerUnit = 1609.344,
        altitudeMultiplierFromMeters = 3.28084f,
    );

    fun convertSpeed(mps: Float): Float = (mps.coerceAtLeast(0f)) * speedMultiplierFromMps

    fun convertDistance(meters: Double): Double = meters.coerceAtLeast(0.0) / distanceMetersPerUnit

    fun convertAltitude(meters: Double): Double = meters * altitudeMultiplierFromMeters
}

enum class SpeedProfile(
    val title: String,
    val maxGaugeValue: Int,
    val majorStep: Int,
    val defaultWarningKmh: Int,
) {
    MATIC_120(
        title = "Matic 120",
        maxGaugeValue = 120,
        majorStep = 20,
        defaultWarningKmh = 70,
    ),
    MAXI_160(
        title = "Maxi 160",
        maxGaugeValue = 160,
        majorStep = 20,
        defaultWarningKmh = 95,
    ),
    RACING_200(
        title = "Bore Up 200",
        maxGaugeValue = 200,
        majorStep = 20,
        defaultWarningKmh = 120,
    ),
}

enum class CockpitAccent(
    val displayName: String,
    val primary: Color,
    val secondary: Color,
) {
    CYAN_NEON("Cyan Neon", CyanNeon, CyanNeonDim),
    AMBER_GT("Amber GT", AmberGT, AmberGTDim),
    EMERALD_TRACK("Emerald Track", EmeraldTrack, EmeraldTrackDim),
    CRIMSON_SPORT("Crimson Sport", CrimsonSport, CrimsonSportDim),
}

enum class OrientationMode(val label: String) {
    LANDSCAPE("Landscape"),
    PORTRAIT("Portrait"),
    AUTO("Otomatis"),
}
