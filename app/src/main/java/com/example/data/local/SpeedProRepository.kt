package com.example.data.local

import com.example.data.model.CockpitAccent
import com.example.data.model.JukenTuningConfig
import com.example.data.model.OrientationMode
import com.example.data.model.SpeedProfile
import com.example.data.model.SpeedUnit
import kotlinx.coroutines.flow.Flow

class SpeedProRepository(
    private val dao: SpeedProDao,
    private val settingsDataStore: SettingsDataStore,
) {
    val settings: Flow<SpeedProSettings> = settingsDataStore.settingsFlow
    val trips: Flow<List<TripEntity>> = dao.getAllTrips()
    val accelerationRuns: Flow<List<AccelerationRunEntity>> = dao.getAllAccelerationRuns()

    suspend fun setSpeedUnit(unit: SpeedUnit) = settingsDataStore.setSpeedUnit(unit)
    suspend fun setSpeedProfile(profile: SpeedProfile) = settingsDataStore.setSpeedProfile(profile)
    suspend fun setCockpitAccent(accent: CockpitAccent) = settingsDataStore.setCockpitAccent(accent)
    suspend fun setOrientationMode(mode: OrientationMode) = settingsDataStore.setOrientationMode(mode)
    suspend fun setSpeedWarningEnabled(enabled: Boolean) = settingsDataStore.setSpeedWarningEnabled(enabled)
    suspend fun setSpeedWarningLimitKmh(limitKmh: Int) = settingsDataStore.setSpeedWarningLimitKmh(limitKmh)
    suspend fun setSpeedWarningSoundEnabled(enabled: Boolean) = settingsDataStore.setSpeedWarningSoundEnabled(enabled)
    suspend fun setSpeedWarningVibrateEnabled(enabled: Boolean) = settingsDataStore.setSpeedWarningVibrateEnabled(enabled)
    suspend fun setKeepScreenOn(enabled: Boolean) = settingsDataStore.setKeepScreenOn(enabled)
    suspend fun setShowCompassAndAltitude(enabled: Boolean) = settingsDataStore.setShowCompassAndAltitude(enabled)

    suspend fun saveDistances(odometerMeters: Double, tripAMeters: Double, tripBMeters: Double) {
        settingsDataStore.saveDistances(odometerMeters, tripAMeters, tripBMeters)
    }

    suspend fun setOdometerMeters(meters: Double) = settingsDataStore.setOdometerMeters(meters)

    suspend fun saveLastJukenDevice(name: String, address: String) =
        settingsDataStore.saveLastJukenDevice(name, address)

    suspend fun saveJukenTuning(config: JukenTuningConfig) =
        settingsDataStore.saveJukenTuning(config)

    suspend fun saveTrip(trip: TripEntity): Long = dao.insertTrip(trip)
    suspend fun deleteTrip(id: Long) = dao.deleteTripById(id)
    suspend fun clearTrips() = dao.clearAllTrips()

    suspend fun saveAccelerationRun(run: AccelerationRunEntity): Long = dao.insertAccelerationRun(run)
    suspend fun deleteAccelerationRun(id: Long) = dao.deleteAccelerationRunById(id)
    suspend fun clearAccelerationRuns() = dao.clearAllAccelerationRuns()
}
