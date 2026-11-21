package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccelerationRunEntity
import com.example.data.local.SettingsDataStore
import com.example.data.local.SpeedProDatabase
import com.example.data.local.SpeedProRepository
import com.example.data.local.SpeedProSettings
import com.example.data.local.TripEntity
import com.example.data.model.AccelerationPhase
import com.example.data.model.AccelerationTimerState
import com.example.data.model.ActiveTripChannel
import com.example.data.model.CockpitAccent
import com.example.data.model.EcuTelemetrySample
import com.example.data.model.GpsTelemetry
import com.example.data.model.JukenBluetoothDevice
import com.example.data.model.JukenConnectionState
import com.example.data.model.JukenEcuTelemetry
import com.example.data.model.JukenTuningConfig
import com.example.data.model.LiveTripState
import com.example.data.model.OrientationMode
import com.example.data.model.SpeedProfile
import com.example.data.model.SpeedUnit
import com.example.location.JukenEcuManager
import com.example.location.TelemetryManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SpeedProViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SpeedProRepository
    private val telemetryManager = TelemetryManager(application)
    private val jukenManager = JukenEcuManager(application)

    val telemetry: StateFlow<GpsTelemetry> = telemetryManager.telemetry
    val jukenConnectionState: StateFlow<JukenConnectionState> = jukenManager.connectionState
    val jukenStatusMessage: StateFlow<String> = jukenManager.statusMessage
    val jukenConnectedDevice: StateFlow<JukenBluetoothDevice?> = jukenManager.connectedDevice
    val jukenDiscoveredDevices: StateFlow<List<JukenBluetoothDevice>> = jukenManager.discoveredDevices
    val jukenTelemetry: StateFlow<JukenEcuTelemetry> = jukenManager.ecuTelemetry

    private val _ecuHistory = MutableStateFlow<List<EcuTelemetrySample>>(emptyList())
    val ecuHistory: StateFlow<List<EcuTelemetrySample>> = _ecuHistory.asStateFlow()

    val settings: StateFlow<SpeedProSettings>
    val trips: StateFlow<List<TripEntity>>
    val accelerationRuns: StateFlow<List<AccelerationRunEntity>>

    private val _liveTrip = MutableStateFlow(LiveTripState())
    val liveTrip: StateFlow<LiveTripState> = _liveTrip.asStateFlow()

    private val _acceleration = MutableStateFlow(AccelerationTimerState())
    val acceleration: StateFlow<AccelerationTimerState> = _acceleration.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var prevDistanceLocation: Location? = null
    private var tripStartedAtMillis: Long = System.currentTimeMillis()
    private var lastDistancePersistMillis: Long = 0L
    private var lastOverspeedAlertMillis: Long = 0L
    private var accelStartMillis: Long = 0L
    private var prevAccelLocation: Location? = null

    init {
        val db = SpeedProDatabase.getInstance(application)
        val dataStore = SettingsDataStore(application)
        repository = SpeedProRepository(db.speedProDao(), dataStore)

        settings = repository.settings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SpeedProSettings()
        )
        trips = repository.trips.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        accelerationRuns = repository.accelerationRuns.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            val initial = repository.settings.first()
            _liveTrip.update {
                it.copy(
                    odometerMeters = initial.odometerMeters,
                    tripADistanceMeters = initial.tripAMeters,
                    tripBDistanceMeters = initial.tripBMeters,
                )
            }
        }

        viewModelScope.launch {
            telemetryManager.lastValidLocation.collect { loc ->
                if (loc != null) {
                    processLocationForTripAndAccel(loc)
                }
            }
        }

        viewModelScope.launch {
            telemetryManager.telemetry.collect { tel ->
                checkOverspeedWarning(tel)
                updateAccelerationFromSpeed(tel)
            }
        }

        // Track peak RPM & rolling telemetry history from Juken 5+
        viewModelScope.launch {
            jukenManager.ecuTelemetry.collect { ecu ->
                if (_acceleration.value.phase == AccelerationPhase.RUNNING && ecu.rpm > 0) {
                    _acceleration.update { acc ->
                        acc.copy(peakRpm = maxOf(acc.peakRpm, ecu.rpm))
                    }
                }
                if (ecu.hasLivePacket) {
                    val sample = EcuTelemetrySample(
                        timestampMillis = ecu.lastPacketMillis,
                        rpm = ecu.rpm,
                        tpsPercent = ecu.tpsPercent,
                        engineTempC = ecu.engineTempC,
                    )
                    _ecuHistory.update { list ->
                        (list + sample).takeLast(60)
                    }
                }
            }
        }

        // 10Hz ticker for smooth stopwatch & moving/total trip timers
        viewModelScope.launch {
            var lastTick = System.currentTimeMillis()
            while (isActive) {
                delay(100L)
                val now = System.currentTimeMillis()
                val dt = (now - lastTick).coerceIn(0L, 1000L)
                lastTick = now

                val currentTel = telemetryManager.telemetry.value
                val trip = _liveTrip.value
                if (trip.isTrackingTrip) {
                    val isMoving = currentTel.speedMps >= 0.5f
                    _liveTrip.update {
                        it.copy(
                            totalTimeMillis = it.totalTimeMillis + dt,
                            movingTimeMillis = if (isMoving) it.movingTimeMillis + dt else it.movingTimeMillis
                        )
                    }
                }

                if (_acceleration.value.phase == AccelerationPhase.RUNNING) {
                    val elapsed = now - accelStartMillis
                    _acceleration.update { it.copy(elapsedMillis = elapsed) }
                }
            }
        }
    }

    fun startTelemetry() {
        telemetryManager.refreshPermissionAndProviderState()
        jukenManager.refreshPairedDevices()
    }

    fun stopTelemetry() {
        telemetryManager.stop()
        persistCurrentDistances()
    }

    // Juken 5+ Bluetooth ECU Actions
    fun hasBluetoothPermissions(): Boolean = jukenManager.hasBluetoothPermissions()
    fun isBluetoothEnabled(): Boolean = jukenManager.isBluetoothEnabled()
    fun refreshPairedJukenDevices() = jukenManager.refreshPairedDevices()
    fun startJukenScan() = jukenManager.startScan()

    fun connectJukenDevice(device: JukenBluetoothDevice) {
        jukenManager.connectToDevice(device)
        viewModelScope.launch {
            repository.saveLastJukenDevice(device.name, device.address)
        }
    }

    fun disconnectJukenDevice() = jukenManager.disconnect()

    fun updateJukenTuningLocal(config: JukenTuningConfig) {
        viewModelScope.launch {
            repository.saveJukenTuning(config)
        }
    }

    fun sendJukenTuningToEcu(config: JukenTuningConfig, saveToEeprom: Boolean) {
        viewModelScope.launch {
            repository.saveJukenTuning(config)
            val sent = jukenManager.sendTuningConfig(config, saveToEeprom)
            _snackbarMessage.value = if (sent) {
                if (saveToEeprom) "Setting Juken 5+ dikirim & disimpan permanen ke ECU"
                else "Live tuning diterapkan ke ECU Juken 5+"
            } else {
                "Profil disimpan di HP • Hubungkan ECU Juken 5+ untuk mengirim"
            }
        }
    }

    // Trip Computer Actions
    fun toggleTripChannel() {
        _liveTrip.update {
            val next = if (it.activeChannel == ActiveTripChannel.TRIP_A) {
                ActiveTripChannel.TRIP_B
            } else {
                ActiveTripChannel.TRIP_A
            }
            it.copy(activeChannel = next)
        }
    }

    fun toggleTripTracking() {
        _liveTrip.update { it.copy(isTrackingTrip = !it.isTrackingTrip) }
    }

    fun resetActiveTrip() {
        _liveTrip.update { state ->
            when (state.activeChannel) {
                ActiveTripChannel.TRIP_A -> state.copy(
                    tripADistanceMeters = 0.0,
                    maxSpeedMps = 0f,
                    movingTimeMillis = 0L,
                    totalTimeMillis = 0L,
                    minAltitudeMeters = null,
                    maxAltitudeMeters = null,
                )
                ActiveTripChannel.TRIP_B -> state.copy(
                    tripBDistanceMeters = 0.0,
                    maxSpeedMps = 0f,
                    movingTimeMillis = 0L,
                    totalTimeMillis = 0L,
                    minAltitudeMeters = null,
                    maxAltitudeMeters = null,
                )
            }
        }
        tripStartedAtMillis = System.currentTimeMillis()
        persistCurrentDistances()
        _snackbarMessage.value = "Data trip direset"
    }

    fun saveCurrentTrip(customTitle: String? = null) {
        val state = _liveTrip.value
        val distance = state.activeTripDistanceMeters
        if (distance < 10.0 && state.totalTimeMillis < 5000L) {
            _snackbarMessage.value = "Belum ada jarak perjalanan untuk disimpan"
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val channelName = if (state.activeChannel == ActiveTripChannel.TRIP_A) "Trip A" else "Trip B"
            val title = customTitle?.takeIf { it.isNotBlank() } ?: "$channelName • ${trips.value.size + 1}"
            repository.saveTrip(
                TripEntity(
                    title = title,
                    startedAtMillis = tripStartedAtMillis,
                    endedAtMillis = now,
                    distanceMeters = distance,
                    durationMillis = state.totalTimeMillis,
                    movingTimeMillis = state.movingTimeMillis,
                    maxSpeedMps = state.maxSpeedMps,
                    avgSpeedMps = state.averageSpeedMps,
                    minAltitudeMeters = state.minAltitudeMeters,
                    maxAltitudeMeters = state.maxAltitudeMeters,
                )
            )
            _snackbarMessage.value = "Perjalanan berhasil disimpan ke Riwayat"
        }
    }

    fun deleteTrip(id: Long) {
        viewModelScope.launch { repository.deleteTrip(id) }
    }

    fun clearAllTrips() {
        viewModelScope.launch { repository.clearTrips() }
    }

    // Motor Matic Acceleration / 201m Sprint Timer Controls
    fun armAccelerationTimer() {
        prevAccelLocation = null
        _acceleration.value = AccelerationTimerState(phase = AccelerationPhase.ARMED)
    }

    fun cancelOrResetAccelerationTimer() {
        prevAccelLocation = null
        _acceleration.value = AccelerationTimerState(phase = AccelerationPhase.IDLE)
    }

    fun finishAndSaveAccelerationRun() {
        val current = _acceleration.value
        if (current.phase != AccelerationPhase.RUNNING && current.phase != AccelerationPhase.COMPLETED) return
        _acceleration.update { it.copy(phase = AccelerationPhase.COMPLETED) }
        viewModelScope.launch {
            if (current.time0To60KmhMillis != null ||
                current.time0To80KmhMillis != null ||
                current.time0To100KmhMillis != null ||
                current.time201mMillis != null ||
                current.peakSpeedMps > 2f
            ) {
                repository.saveAccelerationRun(
                    AccelerationRunEntity(
                        timestampMillis = System.currentTimeMillis(),
                        time0To60KmhMillis = current.time0To60KmhMillis,
                        time0To80KmhMillis = current.time0To80KmhMillis,
                        time0To100KmhMillis = current.time0To100KmhMillis,
                        time201mMillis = current.time201mMillis,
                        peakSpeedMps = current.peakSpeedMps,
                        peakRpm = current.peakRpm,
                    )
                )
                _snackbarMessage.value = "Catatan akselerasi matic disimpan"
            }
        }
    }

    fun deleteAccelerationRun(id: Long) {
        viewModelScope.launch { repository.deleteAccelerationRun(id) }
    }

    fun clearAccelerationRuns() {
        viewModelScope.launch { repository.clearAccelerationRuns() }
    }

    // Settings updates
    fun setSpeedUnit(unit: SpeedUnit) = viewModelScope.launch { repository.setSpeedUnit(unit) }
    fun setSpeedProfile(profile: SpeedProfile) = viewModelScope.launch { repository.setSpeedProfile(profile) }
    fun setCockpitAccent(accent: CockpitAccent) = viewModelScope.launch { repository.setCockpitAccent(accent) }
    fun setOrientationMode(mode: OrientationMode) = viewModelScope.launch { repository.setOrientationMode(mode) }

    fun cycleOrientationMode() {
        val modes = OrientationMode.entries
        val next = modes[(settings.value.orientationMode.ordinal + 1) % modes.size]
        setOrientationMode(next)
        _snackbarMessage.value = "Orientasi: ${next.label}"
    }

    fun setSpeedWarningEnabled(enabled: Boolean) = viewModelScope.launch { repository.setSpeedWarningEnabled(enabled) }
    fun setSpeedWarningLimitKmh(limit: Int) = viewModelScope.launch { repository.setSpeedWarningLimitKmh(limit) }
    fun setSpeedWarningSoundEnabled(enabled: Boolean) = viewModelScope.launch { repository.setSpeedWarningSoundEnabled(enabled) }
    fun setSpeedWarningVibrateEnabled(enabled: Boolean) = viewModelScope.launch { repository.setSpeedWarningVibrateEnabled(enabled) }
    fun setKeepScreenOn(enabled: Boolean) = viewModelScope.launch { repository.setKeepScreenOn(enabled) }
    fun setShowCompassAndAltitude(enabled: Boolean) = viewModelScope.launch { repository.setShowCompassAndAltitude(enabled) }

    fun setOdometerKm(km: Double) {
        val meters = (km * 1000.0).coerceAtLeast(0.0)
        _liveTrip.update { it.copy(odometerMeters = meters) }
        viewModelScope.launch { repository.setOdometerMeters(meters) }
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    private fun processLocationForTripAndAccel(location: Location) {
        val prev = prevDistanceLocation
        prevDistanceLocation = location

        val accuracy = if (location.hasAccuracy()) location.accuracy else 99f
        val speed = telemetry.value.speedMps

        if (_liveTrip.value.isTrackingTrip && prev != null && accuracy <= 35f) {
            val delta = prev.distanceTo(location).toDouble()
            val dtSec = (location.time - prev.time) / 1000.0
            val impliedSpeed = if (dtSec > 0.1) delta / dtSec else 0.0
            if (delta >= 0.8 && impliedSpeed <= 100.0 && speed >= 0.4f) {
                _liveTrip.update { current ->
                    val newAlt = if (location.hasAltitude()) location.altitude else null
                    current.copy(
                        odometerMeters = current.odometerMeters + delta,
                        tripADistanceMeters = current.tripADistanceMeters + delta,
                        tripBDistanceMeters = current.tripBDistanceMeters + delta,
                        maxSpeedMps = maxOf(current.maxSpeedMps, speed),
                        minAltitudeMeters = when {
                            newAlt == null -> current.minAltitudeMeters
                            current.minAltitudeMeters == null -> newAlt
                            else -> minOf(current.minAltitudeMeters, newAlt)
                        },
                        maxAltitudeMeters = when {
                            newAlt == null -> current.maxAltitudeMeters
                            current.maxAltitudeMeters == null -> newAlt
                            else -> maxOf(current.maxAltitudeMeters, newAlt)
                        },
                    )
                }
            }
        }

        // Update 201m sprint distance
        if (_acceleration.value.phase == AccelerationPhase.RUNNING) {
            val prevA = prevAccelLocation
            prevAccelLocation = location
            if (prevA != null && accuracy <= 30f) {
                val deltaA = prevA.distanceTo(location).toDouble()
                if (deltaA in 0.2..120.0) {
                    _acceleration.update { acc ->
                        val newDist = acc.distanceMeters + deltaA
                        val time201 = if (acc.time201mMillis == null && newDist >= 201.0) {
                            System.currentTimeMillis() - accelStartMillis
                        } else acc.time201mMillis
                        acc.copy(
                            distanceMeters = newDist,
                            time201mMillis = time201,
                        )
                    }
                }
            }
        }

        val now = System.currentTimeMillis()
        if (now - lastDistancePersistMillis >= 10_000L) {
            lastDistancePersistMillis = now
            persistCurrentDistances()
        }
    }

    private fun updateAccelerationFromSpeed(tel: GpsTelemetry) {
        val current = _acceleration.value
        val kmh = tel.speedMps * 3.6f

        when (current.phase) {
            AccelerationPhase.ARMED -> {
                if (kmh >= 2.5f) {
                    accelStartMillis = System.currentTimeMillis()
                    prevAccelLocation = telemetryManager.lastValidLocation.value
                    _acceleration.update {
                        it.copy(
                            phase = AccelerationPhase.RUNNING,
                            elapsedMillis = 0L,
                            distanceMeters = 0.0,
                            peakSpeedMps = tel.speedMps,
                            peakRpm = jukenManager.ecuTelemetry.value.rpm,
                        )
                    }
                }
            }
            AccelerationPhase.RUNNING -> {
                val elapsed = System.currentTimeMillis() - accelStartMillis
                _acceleration.update { acc ->
                    val t60 = acc.time0To60KmhMillis ?: if (kmh >= 60f) elapsed else null
                    val t80 = acc.time0To80KmhMillis ?: if (kmh >= 80f) elapsed else null
                    val t100 = acc.time0To100KmhMillis ?: if (kmh >= 100f) elapsed else null
                    acc.copy(
                        elapsedMillis = elapsed,
                        peakSpeedMps = maxOf(acc.peakSpeedMps, tel.speedMps),
                        time0To60KmhMillis = t60,
                        time0To80KmhMillis = t80,
                        time0To100KmhMillis = t100,
                    )
                }
            }
            else -> Unit
        }
    }

    private fun checkOverspeedWarning(tel: GpsTelemetry) {
        val currentSettings = settings.value
        if (!currentSettings.speedWarningEnabled) return
        val currentKmh = tel.speedMps * 3.6f
        if (currentKmh > currentSettings.speedWarningLimitKmh) {
            val now = System.currentTimeMillis()
            if (now - lastOverspeedAlertMillis >= 3500L) {
                lastOverspeedAlertMillis = now
                if (currentSettings.speedWarningVibrateEnabled) {
                    triggerAlertVibration()
                }
                if (currentSettings.speedWarningSoundEnabled) {
                    triggerAlertBeep()
                }
            }
        }
    }

    private fun triggerAlertVibration() {
        runCatching {
            val ctx = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(180L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(180L)
            }
        }
    }

    private fun triggerAlertBeep() {
        runCatching {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
        }
    }

    private fun persistCurrentDistances() {
        val state = _liveTrip.value
        viewModelScope.launch {
            repository.saveDistances(
                odometerMeters = state.odometerMeters,
                tripAMeters = state.tripADistanceMeters,
                tripBMeters = state.tripBDistanceMeters,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        telemetryManager.stop()
        jukenManager.release()
        persistCurrentDistances()
    }
}
