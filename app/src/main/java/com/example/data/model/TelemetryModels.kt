package com.example.data.model

enum class GpsSignalStatus {
    PERMISSION_NEEDED,
    GPS_DISABLED,
    SEARCHING,
    WEAK_FIX,
    LOCKED,
}

data class GpsTelemetry(
    val speedMps: Float = 0f,
    val bearingDegrees: Float = 0f,
    val hasBearing: Boolean = false,
    val altitudeMeters: Double = 0.0,
    val hasAltitude: Boolean = false,
    val accuracyMeters: Float = 0f,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val hasLocation: Boolean = false,
    val satellitesUsed: Int = 0,
    val satellitesVisible: Int = 0,
    val signalStatus: GpsSignalStatus = GpsSignalStatus.SEARCHING,
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val lastUpdatedMillis: Long = 0L,
)

enum class ActiveTripChannel {
    TRIP_A,
    TRIP_B,
}

data class LiveTripState(
    val activeChannel: ActiveTripChannel = ActiveTripChannel.TRIP_A,
    val tripADistanceMeters: Double = 0.0,
    val tripBDistanceMeters: Double = 0.0,
    val odometerMeters: Double = 0.0,
    val maxSpeedMps: Float = 0f,
    val movingTimeMillis: Long = 0L,
    val totalTimeMillis: Long = 0L,
    val minAltitudeMeters: Double? = null,
    val maxAltitudeMeters: Double? = null,
    val isTrackingTrip: Boolean = true,
) {
    val activeTripDistanceMeters: Double
        get() = if (activeChannel == ActiveTripChannel.TRIP_A) tripADistanceMeters else tripBDistanceMeters

    val averageSpeedMps: Float
        get() = if (movingTimeMillis >= 1000L) {
            (activeTripDistanceMeters / (movingTimeMillis / 1000.0)).toFloat()
        } else {
            0f
        }
}

enum class AccelerationPhase {
    IDLE,
    ARMED,
    RUNNING,
    COMPLETED,
}

data class AccelerationTimerState(
    val phase: AccelerationPhase = AccelerationPhase.IDLE,
    val elapsedMillis: Long = 0L,
    val distanceMeters: Double = 0.0,
    val peakSpeedMps: Float = 0f,
    val peakRpm: Int = 0,
    val time0To60KmhMillis: Long? = null,
    val time0To80KmhMillis: Long? = null,
    val time0To100KmhMillis: Long? = null,
    val time201mMillis: Long? = null,
)
