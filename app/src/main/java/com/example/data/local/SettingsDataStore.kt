package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.CockpitAccent
import com.example.data.model.JukenMapSlot
import com.example.data.model.JukenTuningConfig
import com.example.data.model.OrientationMode
import com.example.data.model.SpeedProfile
import com.example.data.model.SpeedUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.speedProDataStore: DataStore<Preferences> by preferencesDataStore(name = "speed_pro_prefs")

data class SpeedProSettings(
    val speedUnit: SpeedUnit = SpeedUnit.KMH,
    val speedProfile: SpeedProfile = SpeedProfile.MAXI_160,
    val cockpitAccent: CockpitAccent = CockpitAccent.CYAN_NEON,
    val orientationMode: OrientationMode = OrientationMode.LANDSCAPE,
    val speedWarningEnabled: Boolean = true,
    val speedWarningLimitKmh: Int = 95,
    val speedWarningSoundEnabled: Boolean = true,
    val speedWarningVibrateEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val showCompassAndAltitude: Boolean = false,
    val odometerMeters: Double = 0.0,
    val tripAMeters: Double = 0.0,
    val tripBMeters: Double = 0.0,
    val lastJukenDeviceAddress: String = "",
    val lastJukenDeviceName: String = "",
    val jukenTuning: JukenTuningConfig = JukenTuningConfig(),
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val SPEED_UNIT = stringPreferencesKey("speed_unit")
        val SPEED_PROFILE = stringPreferencesKey("speed_profile")
        val COCKPIT_ACCENT = stringPreferencesKey("cockpit_accent")
        val ORIENTATION_MODE = stringPreferencesKey("orientation_mode")
        val WARNING_ENABLED = booleanPreferencesKey("warning_enabled")
        val WARNING_LIMIT_KMH = intPreferencesKey("warning_limit_kmh")
        val WARNING_SOUND = booleanPreferencesKey("warning_sound")
        val WARNING_VIBRATE = booleanPreferencesKey("warning_vibrate")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SHOW_COMPASS_ALT = booleanPreferencesKey("show_compass_alt")
        val ODOMETER_METERS = doublePreferencesKey("odometer_meters")
        val TRIP_A_METERS = doublePreferencesKey("trip_a_meters")
        val TRIP_B_METERS = doublePreferencesKey("trip_b_meters")
        val LAST_JUKEN_ADDR = stringPreferencesKey("last_juken_addr")
        val LAST_JUKEN_NAME = stringPreferencesKey("last_juken_name")
        val JUKEN_MAP = stringPreferencesKey("juken_map")
        val JUKEN_FUEL_LOW = intPreferencesKey("juken_fuel_low")
        val JUKEN_FUEL_MID = intPreferencesKey("juken_fuel_mid")
        val JUKEN_FUEL_HIGH = intPreferencesKey("juken_fuel_high")
        val JUKEN_IGN_OFFSET = intPreferencesKey("juken_ign_offset")
        val JUKEN_REV_LIMITER = intPreferencesKey("juken_rev_limiter")
        val JUKEN_JJS_ENABLED = booleanPreferencesKey("juken_jjs_enabled")
        val JUKEN_JJS_FUEL = intPreferencesKey("juken_jjs_fuel")
        val JUKEN_IDLE_RPM = intPreferencesKey("juken_idle_rpm")
    }

    val settingsFlow: Flow<SpeedProSettings> = context.speedProDataStore.data.map { prefs ->
        SpeedProSettings(
            speedUnit = prefs[Keys.SPEED_UNIT]?.let { runCatching { SpeedUnit.valueOf(it) }.getOrNull() }
                ?: SpeedUnit.KMH,
            speedProfile = prefs[Keys.SPEED_PROFILE]?.let { runCatching { SpeedProfile.valueOf(it) }.getOrNull() }
                ?: SpeedProfile.MAXI_160,
            cockpitAccent = prefs[Keys.COCKPIT_ACCENT]?.let { runCatching { CockpitAccent.valueOf(it) }.getOrNull() }
                ?: CockpitAccent.CYAN_NEON,
            orientationMode = prefs[Keys.ORIENTATION_MODE]?.let { runCatching { OrientationMode.valueOf(it) }.getOrNull() }
                ?: OrientationMode.LANDSCAPE,
            speedWarningEnabled = prefs[Keys.WARNING_ENABLED] ?: true,
            speedWarningLimitKmh = prefs[Keys.WARNING_LIMIT_KMH] ?: 95,
            speedWarningSoundEnabled = prefs[Keys.WARNING_SOUND] ?: true,
            speedWarningVibrateEnabled = prefs[Keys.WARNING_VIBRATE] ?: true,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
            showCompassAndAltitude = prefs[Keys.SHOW_COMPASS_ALT] ?: false,
            odometerMeters = prefs[Keys.ODOMETER_METERS] ?: 0.0,
            tripAMeters = prefs[Keys.TRIP_A_METERS] ?: 0.0,
            tripBMeters = prefs[Keys.TRIP_B_METERS] ?: 0.0,
            lastJukenDeviceAddress = prefs[Keys.LAST_JUKEN_ADDR] ?: "",
            lastJukenDeviceName = prefs[Keys.LAST_JUKEN_NAME] ?: "",
            jukenTuning = JukenTuningConfig(
                activeMap = prefs[Keys.JUKEN_MAP]?.let { runCatching { JukenMapSlot.valueOf(it) }.getOrNull() }
                    ?: JukenMapSlot.MAP_1_HARIAN,
                fuelLowPercent = prefs[Keys.JUKEN_FUEL_LOW] ?: 0,
                fuelMidPercent = prefs[Keys.JUKEN_FUEL_MID] ?: 0,
                fuelHighPercent = prefs[Keys.JUKEN_FUEL_HIGH] ?: 0,
                ignitionOffsetDeg = prefs[Keys.JUKEN_IGN_OFFSET] ?: 0,
                revLimiterRpm = prefs[Keys.JUKEN_REV_LIMITER] ?: 10500,
                jjsEnabled = prefs[Keys.JUKEN_JJS_ENABLED] ?: false,
                jjsFuelPercent = prefs[Keys.JUKEN_JJS_FUEL] ?: 4,
                idleRpmTarget = prefs[Keys.JUKEN_IDLE_RPM] ?: 1500,
            )
        )
    }

    suspend fun setSpeedUnit(unit: SpeedUnit) {
        context.speedProDataStore.edit { it[Keys.SPEED_UNIT] = unit.name }
    }

    suspend fun setSpeedProfile(profile: SpeedProfile) {
        context.speedProDataStore.edit {
            it[Keys.SPEED_PROFILE] = profile.name
            it[Keys.WARNING_LIMIT_KMH] = profile.defaultWarningKmh
        }
    }

    suspend fun setCockpitAccent(accent: CockpitAccent) {
        context.speedProDataStore.edit { it[Keys.COCKPIT_ACCENT] = accent.name }
    }

    suspend fun setOrientationMode(mode: OrientationMode) {
        context.speedProDataStore.edit { it[Keys.ORIENTATION_MODE] = mode.name }
    }

    suspend fun setSpeedWarningEnabled(enabled: Boolean) {
        context.speedProDataStore.edit { it[Keys.WARNING_ENABLED] = enabled }
    }

    suspend fun setSpeedWarningLimitKmh(limitKmh: Int) {
        context.speedProDataStore.edit { it[Keys.WARNING_LIMIT_KMH] = limitKmh.coerceIn(20, 200) }
    }

    suspend fun setSpeedWarningSoundEnabled(enabled: Boolean) {
        context.speedProDataStore.edit { it[Keys.WARNING_SOUND] = enabled }
    }

    suspend fun setSpeedWarningVibrateEnabled(enabled: Boolean) {
        context.speedProDataStore.edit { it[Keys.WARNING_VIBRATE] = enabled }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.speedProDataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setShowCompassAndAltitude(enabled: Boolean) {
        context.speedProDataStore.edit { it[Keys.SHOW_COMPASS_ALT] = enabled }
    }

    suspend fun saveDistances(odometerMeters: Double, tripAMeters: Double, tripBMeters: Double) {
        context.speedProDataStore.edit {
            it[Keys.ODOMETER_METERS] = odometerMeters
            it[Keys.TRIP_A_METERS] = tripAMeters
            it[Keys.TRIP_B_METERS] = tripBMeters
        }
    }

    suspend fun setOdometerMeters(odometerMeters: Double) {
        context.speedProDataStore.edit { it[Keys.ODOMETER_METERS] = odometerMeters.coerceAtLeast(0.0) }
    }

    suspend fun saveLastJukenDevice(name: String, address: String) {
        context.speedProDataStore.edit {
            it[Keys.LAST_JUKEN_NAME] = name
            it[Keys.LAST_JUKEN_ADDR] = address
        }
    }

    suspend fun saveJukenTuning(config: JukenTuningConfig) {
        context.speedProDataStore.edit {
            it[Keys.JUKEN_MAP] = config.activeMap.name
            it[Keys.JUKEN_FUEL_LOW] = config.fuelLowPercent.coerceIn(-30, 30)
            it[Keys.JUKEN_FUEL_MID] = config.fuelMidPercent.coerceIn(-30, 30)
            it[Keys.JUKEN_FUEL_HIGH] = config.fuelHighPercent.coerceIn(-30, 30)
            it[Keys.JUKEN_IGN_OFFSET] = config.ignitionOffsetDeg.coerceIn(-10, 10)
            it[Keys.JUKEN_REV_LIMITER] = config.revLimiterRpm.coerceIn(8000, 13500)
            it[Keys.JUKEN_JJS_ENABLED] = config.jjsEnabled
            it[Keys.JUKEN_JJS_FUEL] = config.jjsFuelPercent.coerceIn(-15, 15)
            it[Keys.JUKEN_IDLE_RPM] = config.idleRpmTarget.coerceIn(1200, 2200)
        }
    }
}
