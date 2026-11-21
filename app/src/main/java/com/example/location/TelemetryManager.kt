package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.data.model.GpsSignalStatus
import com.example.data.model.GpsTelemetry
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

class TelemetryManager(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _telemetry = MutableStateFlow(GpsTelemetry())
    val telemetry: StateFlow<GpsTelemetry> = _telemetry.asStateFlow()

    private val _lastValidLocation = MutableStateFlow<Location?>(null)
    val lastValidLocation: StateFlow<Location?> = _lastValidLocation.asStateFlow()

    private var isLocationStarted = false
    private var isSensorsStarted = false
    private var sensorHeadingDegrees: Float? = null

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
        .setMinUpdateIntervalMillis(250L)
        .setWaitForAccurateLocation(false)
        .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            handleNewLocation(loc)
        }
    }

    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            var used = 0
            val count = status.satelliteCount
            for (i in 0 until count) {
                if (status.usedInFix(i)) used++
            }
            _telemetry.update { current ->
                current.copy(
                    satellitesUsed = used,
                    satellitesVisible = count,
                )
            }
        }
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuthRad = orientationAngles[0]
                val azimuthDeg = ((Math.toDegrees(azimuthRad.toDouble()) + 360.0) % 360.0).toFloat()
                sensorHeadingDegrees = azimuthDeg
                _telemetry.update { current ->
                    // Use magnetometer heading when stationary or when GPS bearing isn't present
                    if (current.speedMps < 1.2f || !current.hasBearing) {
                        current.copy(
                            bearingDegrees = azimuthDeg,
                            hasBearing = true,
                        )
                    } else {
                        current
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            if (level >= 0 && scale > 0) {
                val pct = ((level * 100f) / scale).roundToInt().coerceIn(0, 100)
                _telemetry.update { it.copy(batteryPercent = pct, isCharging = isCharging) }
            }
        }
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isGpsProviderEnabled(): Boolean {
        return runCatching {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(true)
    }

    fun refreshPermissionAndProviderState() {
        if (!hasLocationPermission()) {
            _telemetry.update {
                it.copy(signalStatus = GpsSignalStatus.PERMISSION_NEEDED, speedMps = 0f)
            }
            stopLocationUpdates()
            return
        }
        if (!isGpsProviderEnabled()) {
            _telemetry.update {
                it.copy(signalStatus = GpsSignalStatus.GPS_DISABLED, speedMps = 0f)
            }
        } else if (_telemetry.value.signalStatus == GpsSignalStatus.PERMISSION_NEEDED ||
            _telemetry.value.signalStatus == GpsSignalStatus.GPS_DISABLED
        ) {
            _telemetry.update { it.copy(signalStatus = GpsSignalStatus.SEARCHING) }
        }
        start()
    }

    @SuppressLint("MissingPermission")
    fun start() {
        startSensorsAndBattery()
        if (!hasLocationPermission()) {
            _telemetry.update { it.copy(signalStatus = GpsSignalStatus.PERMISSION_NEEDED) }
            return
        }
        if (isLocationStarted) return
        isLocationStarted = true

        _telemetry.update {
            it.copy(
                signalStatus = if (isGpsProviderEnabled()) GpsSignalStatus.SEARCHING
                else GpsSignalStatus.GPS_DISABLED
            )
        }

        runCatching {
            fusedClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.registerGnssStatusCallback(
                    ContextCompat.getMainExecutor(context),
                    gnssCallback
                )
            } else {
                @Suppress("DEPRECATION")
                locationManager.registerGnssStatusCallback(gnssCallback)
            }
        }
    }

    fun stop() {
        stopLocationUpdates()
        stopSensorsAndBattery()
    }

    private fun stopLocationUpdates() {
        if (!isLocationStarted) return
        isLocationStarted = false
        runCatching { fusedClient.removeLocationUpdates(locationCallback) }
        runCatching { locationManager.unregisterGnssStatusCallback(gnssCallback) }
    }

    private fun startSensorsAndBattery() {
        if (isSensorsStarted) return
        isSensorsStarted = true
        val rotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotSensor != null) {
            sensorManager.registerListener(
                sensorListener,
                rotSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        runCatching {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val sticky = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(batteryReceiver, filter)
            }
            sticky?.let { batteryReceiver.onReceive(context, it) }
        }
    }

    private fun stopSensorsAndBattery() {
        if (!isSensorsStarted) return
        isSensorsStarted = false
        runCatching { sensorManager.unregisterListener(sensorListener) }
        runCatching { context.unregisterReceiver(batteryReceiver) }
    }

    private fun handleNewLocation(location: Location) {
        val prev = _lastValidLocation.value
        val accuracy = if (location.hasAccuracy()) location.accuracy else 99f

        var rawSpeed = if (location.hasSpeed()) {
            location.speed
        } else if (prev != null) {
            val dtSec = (location.time - prev.time) / 1000f
            if (dtSec in 0.2f..5.0f && accuracy < 25f) {
                prev.distanceTo(location) / dtSec
            } else 0f
        } else {
            0f
        }

        // Suppress low-speed GPS jitter when stationary
        if (rawSpeed < 0.45f || accuracy > 50f) {
            rawSpeed = 0f
        }

        val gpsBearing = if (location.hasBearing() && rawSpeed >= 1.2f) {
            location.bearing
        } else {
            sensorHeadingDegrees ?: _telemetry.value.bearingDegrees
        }
        val hasBearing = (location.hasBearing() && rawSpeed >= 1.2f) ||
            sensorHeadingDegrees != null ||
            _telemetry.value.hasBearing

        val signalStatus = when {
            !isGpsProviderEnabled() -> GpsSignalStatus.GPS_DISABLED
            accuracy <= 20f -> GpsSignalStatus.LOCKED
            else -> GpsSignalStatus.WEAK_FIX
        }

        _lastValidLocation.value = location
        _telemetry.update { current ->
            current.copy(
                speedMps = rawSpeed,
                bearingDegrees = gpsBearing,
                hasBearing = hasBearing,
                altitudeMeters = if (location.hasAltitude()) location.altitude else current.altitudeMeters,
                hasAltitude = location.hasAltitude() || current.hasAltitude,
                accuracyMeters = accuracy,
                latitude = location.latitude,
                longitude = location.longitude,
                hasLocation = true,
                signalStatus = signalStatus,
                lastUpdatedMillis = System.currentTimeMillis(),
            )
        }
    }
}
