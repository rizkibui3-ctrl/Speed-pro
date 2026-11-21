package com.example.data.model

enum class JukenConnectionState {
    DISCONNECTED,
    PERMISSION_NEEDED,
    BLUETOOTH_OFF,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR,
}

enum class JukenTransportType(val label: String) {
    CLASSIC_SPP("BT Classic SPP"),
    BLE_GATT("BLE GATT"),
}

data class JukenBluetoothDevice(
    val name: String,
    val address: String,
    val transportType: JukenTransportType,
    val isPaired: Boolean,
    val rssi: Int? = null,
) {
    val isLikelyJuken: Boolean
        get() {
            val upper = name.uppercase()
            return upper.contains("JUKEN") ||
                upper.contains("BRT") ||
                upper.contains("ECU") ||
                upper.contains("HC-05") ||
                upper.contains("HC-06") ||
                upper.contains("JDY") ||
                upper.contains("UART")
        }
}

enum class JukenMapSlot(val label: String, val shortLabel: String, val mapIndex: Int) {
    MAP_1_HARIAN("Map 1 • Harian", "MAP 1", 1),
    MAP_2_RACING("Map 2 • Racing / Touring", "MAP 2", 2),
}

data class JukenEcuTelemetry(
    val rpm: Int = 0,
    val tpsPercent: Float = 0f,
    val afr: Float = 0f,
    val batteryVoltage: Float = 0f,
    val engineTempC: Int = 0,
    val ignitionTimingDeg: Float = 0f,
    val fuelCorrectionPercent: Int = 0,
    val injectorDutyPercent: Float = 0f,
    val hasLivePacket: Boolean = false,
    val lastPacketMillis: Long = 0L,
)

data class JukenTuningConfig(
    val activeMap: JukenMapSlot = JukenMapSlot.MAP_1_HARIAN,
    val fuelLowPercent: Int = 0,
    val fuelMidPercent: Int = 0,
    val fuelHighPercent: Int = 0,
    val ignitionOffsetDeg: Int = 0,
    val revLimiterRpm: Int = 10500,
    val jjsEnabled: Boolean = false,
    val jjsFuelPercent: Int = 4,
    val idleRpmTarget: Int = 1500,
)

data class EcuTelemetrySample(
    val timestampMillis: Long,
    val rpm: Int,
    val tpsPercent: Float,
    val engineTempC: Int,
)
