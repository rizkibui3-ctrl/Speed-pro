package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val distanceMeters: Double,
    val durationMillis: Long,
    val movingTimeMillis: Long,
    val maxSpeedMps: Float,
    val avgSpeedMps: Float,
    val minAltitudeMeters: Double?,
    val maxAltitudeMeters: Double?,
)

@Entity(tableName = "acceleration_runs")
data class AccelerationRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestampMillis: Long,
    val time0To60KmhMillis: Long?,
    val time0To80KmhMillis: Long?,
    val time0To100KmhMillis: Long?,
    val time201mMillis: Long?,
    val peakSpeedMps: Float,
    val peakRpm: Int,
)
