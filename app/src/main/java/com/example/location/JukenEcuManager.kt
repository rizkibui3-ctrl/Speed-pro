package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.model.JukenBluetoothDevice
import com.example.data.model.JukenConnectionState
import com.example.data.model.JukenEcuTelemetry
import com.example.data.model.JukenTransportType
import com.example.data.model.JukenTuningConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

class JukenEcuManager(private val context: Context) {

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

        // Common BLE UART UUIDs used by BRT / HM-10 / Nordic UART dongles
        private val NORDIC_UART_SERVICE: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        private val NORDIC_UART_RX: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        private val NORDIC_UART_TX: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        private val HM10_SERVICE: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        private val HM10_CHAR: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _connectionState = MutableStateFlow(JukenConnectionState.DISCONNECTED)
    val connectionState: StateFlow<JukenConnectionState> = _connectionState.asStateFlow()

    private val _statusMessage = MutableStateFlow("ECU Juken 5+ belum terhubung")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _connectedDevice = MutableStateFlow<JukenBluetoothDevice?>(null)
    val connectedDevice: StateFlow<JukenBluetoothDevice?> = _connectedDevice.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<JukenBluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<JukenBluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _ecuTelemetry = MutableStateFlow(JukenEcuTelemetry())
    val ecuTelemetry: StateFlow<JukenEcuTelemetry> = _ecuTelemetry.asStateFlow()

    private var sppSocket: BluetoothSocket? = null
    private var sppReaderJob: Job? = null
    private var scanTimeoutJob: Job? = null
    private var bleGatt: BluetoothGatt? = null
    private var bleWriteChar: BluetoothGattCharacteristic? = null
    private var bleScanner: BluetoothLeScanner? = null
    private var isDiscoveryReceiverRegistered = false
    private val asciiLineBuffer = StringBuilder()

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                        .takeIf { it != Short.MIN_VALUE }?.toInt()
                    device?.let { addOrUpdateDevice(it, isPaired = false, rssi = rssi) }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    if (_connectionState.value == JukenConnectionState.SCANNING) {
                        _connectionState.value = JukenConnectionState.DISCONNECTED
                        _statusMessage.value = if (_discoveredDevices.value.isEmpty()) {
                            "Tidak ada modul Juken 5+ ditemukan"
                        } else {
                            "Pemindaian selesai • ${_discoveredDevices.value.size} perangkat"
                        }
                    }
                }
            }
        }
    }

    private val bleScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val dev = result?.device ?: return
            addOrUpdateDevice(dev, isPaired = false, rssi = result.rssi)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _statusMessage.value = "Tersambung ke GATT, membaca layanan ECU…"
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                cleanupConnection()
                _connectionState.value = JukenConnectionState.DISCONNECTED
                _statusMessage.value = "Koneksi BLE ECU terputus"
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                cleanupConnection()
                _connectionState.value = JukenConnectionState.ERROR
                _statusMessage.value = "Gagal membaca layanan BLE ECU (kode $status)"
                return
            }

            val nordicService = gatt.getService(NORDIC_UART_SERVICE)
            val hm10Service = gatt.getService(HM10_SERVICE)

            val notifyChar: BluetoothGattCharacteristic?
            val writeChar: BluetoothGattCharacteristic?

            when {
                nordicService != null -> {
                    notifyChar = nordicService.getCharacteristic(NORDIC_UART_TX)
                    writeChar = nordicService.getCharacteristic(NORDIC_UART_RX)
                }
                hm10Service != null -> {
                    notifyChar = hm10Service.getCharacteristic(HM10_CHAR)
                    writeChar = hm10Service.getCharacteristic(HM10_CHAR)
                }
                else -> {
                    // Fallback: pick any writable + notifiable characteristic
                    val allChars = gatt.services.flatMap { it.characteristics }
                    notifyChar = allChars.firstOrNull {
                        (it.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                    }
                    writeChar = allChars.firstOrNull {
                        (it.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or
                            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0
                    }
                }
            }

            bleWriteChar = writeChar
            if (notifyChar != null) {
                gatt.setCharacteristicNotification(notifyChar, true)
                val descriptor = notifyChar.getDescriptor(CCCD_UUID)
                if (descriptor != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    } else {
                        @Suppress("DEPRECATION")
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        @Suppress("DEPRECATION")
                        gatt.writeDescriptor(descriptor)
                    }
                }
            }

            _connectionState.value = JukenConnectionState.CONNECTED
            _statusMessage.value = "Terhubung ke ${_connectedDevice.value?.name ?: "ECU Juken 5+"}"
            requestLiveStreamHandshake()
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            @Suppress("DEPRECATION")
            val bytes = characteristic.value ?: return
            parseIncomingBytes(bytes)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            parseIncomingBytes(value)
        }
    }

    fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        if (!isBluetoothSupported()) {
            _statusMessage.value = "Perangkat tidak mendukung Bluetooth"
            return
        }
        if (!hasBluetoothPermissions()) {
            _connectionState.value = JukenConnectionState.PERMISSION_NEEDED
            _statusMessage.value = "Izin Bluetooth diperlukan untuk terhubung ke Juken 5+"
            return
        }
        if (!isBluetoothEnabled()) {
            _connectionState.value = JukenConnectionState.BLUETOOTH_OFF
            _statusMessage.value = "Nyalakan Bluetooth untuk mencari modul Juken 5+"
            return
        }
        val paired = bluetoothAdapter?.bondedDevices.orEmpty()
        paired.forEach { addOrUpdateDevice(it, isPaired = true, rssi = null) }
        if (_connectionState.value == JukenConnectionState.PERMISSION_NEEDED ||
            _connectionState.value == JukenConnectionState.BLUETOOTH_OFF
        ) {
            _connectionState.value = JukenConnectionState.DISCONNECTED
            _statusMessage.value = "Siap menghubungkan ECU Juken 5+"
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!isBluetoothSupported()) {
            _connectionState.value = JukenConnectionState.ERROR
            _statusMessage.value = "Bluetooth tidak tersedia di perangkat ini"
            return
        }
        if (!hasBluetoothPermissions()) {
            _connectionState.value = JukenConnectionState.PERMISSION_NEEDED
            _statusMessage.value = "Berikan izin Perangkat Sekitar / Bluetooth terlebih dahulu"
            return
        }
        if (!isBluetoothEnabled()) {
            _connectionState.value = JukenConnectionState.BLUETOOTH_OFF
            _statusMessage.value = "Bluetooth nonaktif — nyalakan untuk memindai ECU"
            return
        }

        refreshPairedDevices()
        stopScanInternal()

        _connectionState.value = JukenConnectionState.SCANNING
        _statusMessage.value = "Memindai modul Bluetooth Juken 5+…"

        if (!isDiscoveryReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(discoveryReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(discoveryReceiver, filter)
            }
            isDiscoveryReceiverRegistered = true
        }

        runCatching { bluetoothAdapter?.startDiscovery() }
        runCatching {
            bleScanner = bluetoothAdapter?.bluetoothLeScanner
            bleScanner?.startScan(bleScanCallback)
        }

        scanTimeoutJob?.cancel()
        scanTimeoutJob = scope.launch {
            delay(10_000L)
            stopScanInternal()
            if (_connectionState.value == JukenConnectionState.SCANNING) {
                _connectionState.value = JukenConnectionState.DISCONNECTED
                _statusMessage.value = "Pemindaian selesai • ${_discoveredDevices.value.size} perangkat"
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceInfo: JukenBluetoothDevice) {
        if (!hasBluetoothPermissions()) {
            _connectionState.value = JukenConnectionState.PERMISSION_NEEDED
            return
        }
        if (!isBluetoothEnabled()) {
            _connectionState.value = JukenConnectionState.BLUETOOTH_OFF
            return
        }

        stopScanInternal()
        disconnect()

        val btDevice = runCatching { bluetoothAdapter?.getRemoteDevice(deviceInfo.address) }.getOrNull()
        if (btDevice == null) {
            _connectionState.value = JukenConnectionState.ERROR
            _statusMessage.value = "Alamat Bluetooth ECU tidak valid"
            return
        }

        _connectedDevice.value = deviceInfo
        _connectionState.value = JukenConnectionState.CONNECTING
        _statusMessage.value = "Menghubungkan ke ${deviceInfo.name}…"

        if (deviceInfo.transportType == JukenTransportType.BLE_GATT) {
            bleGatt = btDevice.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            sppReaderJob = scope.launch {
                connectSppAndListen(btDevice, deviceInfo)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectSppAndListen(btDevice: BluetoothDevice, info: JukenBluetoothDevice) {
        var socket: BluetoothSocket? = null
        try {
            socket = runCatching {
                btDevice.createRfcommSocketToServiceRecord(SPP_UUID)
            }.getOrNull() ?: btDevice.createInsecureRfcommSocketToServiceRecord(SPP_UUID)

            socket.connect()
            sppSocket = socket
            _connectionState.value = JukenConnectionState.CONNECTED
            _statusMessage.value = "Terhubung ke ${info.name}"
            requestLiveStreamHandshake()

            val input = socket.inputStream
            val buffer = ByteArray(1024)
            while (scope.isActive && socket.isConnected) {
                val read = input.read(buffer)
                if (read > 0) {
                    parseIncomingBytes(buffer.copyOf(read))
                } else if (read == -1) {
                    break
                }
            }
        } catch (e: IOException) {
            if (_connectionState.value == JukenConnectionState.CONNECTING ||
                _connectionState.value == JukenConnectionState.CONNECTED
            ) {
                _connectionState.value = JukenConnectionState.ERROR
                _statusMessage.value = "Gagal terhubung ke ${info.name}: pastikan kontak motor ON & modul Juken aktif"
            }
        } finally {
            runCatching { socket?.close() }
            if (sppSocket == socket) {
                sppSocket = null
            }
            if (_connectionState.value == JukenConnectionState.CONNECTED) {
                _connectionState.value = JukenConnectionState.DISCONNECTED
                _statusMessage.value = "Koneksi ECU terputus"
            }
        }
    }

    fun disconnect() {
        stopScanInternal()
        cleanupConnection()
        _connectionState.value = JukenConnectionState.DISCONNECTED
        _statusMessage.value = "ECU Juken 5+ terputus"
        _ecuTelemetry.value = JukenEcuTelemetry()
    }

    fun sendTuningConfig(config: JukenTuningConfig, saveToEeprom: Boolean): Boolean {
        if (_connectionState.value != JukenConnectionState.CONNECTED) {
            return false
        }
        val payload = buildString {
            append("JUKEN_CMD:")
            append("MAP=${config.activeMap.mapIndex},")
            append("FL=${config.fuelLowPercent},")
            append("FM=${config.fuelMidPercent},")
            append("FH=${config.fuelHighPercent},")
            append("IGN=${config.ignitionOffsetDeg},")
            append("LIM=${config.revLimiterRpm},")
            append("JJS=${if (config.jjsEnabled) 1 else 0},")
            append("JJSF=${config.jjsFuelPercent},")
            append("IDLE=${config.idleRpmTarget},")
            append("SAVE=${if (saveToEeprom) 1 else 0}")
            append("\r\n")
        }.toByteArray(Charsets.UTF_8)

        return writeBytes(payload)
    }

    private fun requestLiveStreamHandshake() {
        writeBytes("JUKEN_LIVE:1\r\n".toByteArray(Charsets.UTF_8))
    }

    @SuppressLint("MissingPermission")
    private fun writeBytes(bytes: ByteArray): Boolean {
        sppSocket?.let { sock ->
            return runCatching {
                scope.launch {
                    runCatching {
                        sock.outputStream.write(bytes)
                        sock.outputStream.flush()
                    }
                }
                true
            }.getOrDefault(false)
        }

        val gatt = bleGatt
        val char = bleWriteChar
        if (gatt != null && char != null) {
            return runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(
                        char,
                        bytes,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    ) == android.bluetooth.BluetoothStatusCodes.SUCCESS
                } else {
                    @Suppress("DEPRECATION")
                    char.value = bytes
                    @Suppress("DEPRECATION")
                    gatt.writeCharacteristic(char)
                }
            }.getOrDefault(false)
        }
        return false
    }

    private fun parseIncomingBytes(bytes: ByteArray) {
        // 1. Check for binary framed packet: [0xAA, 0x55, rpmHi, rpmLo, tps, afrX10, voltX10, temp, ignPlus40, fuelPlus50]
        if (bytes.size >= 10 && bytes[0] == 0xAA.toByte() && bytes[1] == 0x55.toByte()) {
            val rpm = ((bytes[2].toInt() and 0xFF) shl 8) or (bytes[3].toInt() and 0xFF)
            val tps = (bytes[4].toInt() and 0xFF).coerceIn(0, 100).toFloat()
            val afr = (bytes[5].toInt() and 0xFF) / 10f
            val volt = (bytes[6].toInt() and 0xFF) / 10f
            val temp = (bytes[7].toInt() and 0xFF)
            val ign = (bytes[8].toInt() and 0xFF) - 40f
            val fuel = (bytes[9].toInt() and 0xFF) - 50
            _ecuTelemetry.update {
                it.copy(
                    rpm = rpm.coerceIn(0, 18000),
                    tpsPercent = tps,
                    afr = afr,
                    batteryVoltage = volt,
                    engineTempC = temp,
                    ignitionTimingDeg = ign,
                    fuelCorrectionPercent = fuel,
                    hasLivePacket = true,
                    lastPacketMillis = System.currentTimeMillis()
                )
            }
            return
        }

        // 2. Parse ASCII key-value telemetry lines (e.g. RPM:5200,TPS:35.0,AFR:13.2,VOLT:13.8,TEMP:86,IGN:24.5,FUEL:4)
        val chunk = bytes.toString(Charsets.UTF_8)
        asciiLineBuffer.append(chunk)
        var newlineIdx = asciiLineBuffer.indexOf("\n")
        while (newlineIdx >= 0) {
            val line = asciiLineBuffer.substring(0, newlineIdx).trim()
            asciiLineBuffer.delete(0, newlineIdx + 1)
            if (line.isNotEmpty()) {
                parseAsciiTelemetryLine(line)
            }
            newlineIdx = asciiLineBuffer.indexOf("\n")
        }
        if (asciiLineBuffer.length > 512) {
            asciiLineBuffer.clear()
        }
    }

    private fun parseAsciiTelemetryLine(line: String) {
        val pairs = line.split(",", ";", " ")
        var updated = _ecuTelemetry.value
        var parsedAny = false
        for (token in pairs) {
            val kv = token.split(":", "=")
            if (kv.size != 2) continue
            val key = kv[0].trim().uppercase()
            val valStr = kv[1].trim()
            when (key) {
                "RPM" -> valStr.toIntOrNull()?.let {
                    updated = updated.copy(rpm = it.coerceIn(0, 18000)); parsedAny = true
                }
                "TPS" -> valStr.toFloatOrNull()?.let {
                    updated = updated.copy(tpsPercent = it.coerceIn(0f, 100f)); parsedAny = true
                }
                "AFR" -> valStr.toFloatOrNull()?.let {
                    updated = updated.copy(afr = it.coerceIn(8f, 22f)); parsedAny = true
                }
                "VOLT", "BAT", "VBAT" -> valStr.toFloatOrNull()?.let {
                    updated = updated.copy(batteryVoltage = it.coerceIn(0f, 20f)); parsedAny = true
                }
                "TEMP", "EOT", "CHT", "ECT", "CLT", "COOLANT", "WT" -> valStr.toIntOrNull()?.let {
                    updated = updated.copy(engineTempC = it.coerceIn(-20, 220)); parsedAny = true
                }
                "IGN", "TIMING" -> valStr.toFloatOrNull()?.let {
                    updated = updated.copy(ignitionTimingDeg = it); parsedAny = true
                }
                "FUEL", "CORR" -> valStr.toIntOrNull()?.let {
                    updated = updated.copy(fuelCorrectionPercent = it); parsedAny = true
                }
                "DUTY", "INJ" -> valStr.toFloatOrNull()?.let {
                    updated = updated.copy(injectorDutyPercent = it.coerceIn(0f, 100f)); parsedAny = true
                }
            }
        }
        if (parsedAny) {
            _ecuTelemetry.value = updated.copy(
                hasLivePacket = true,
                lastPacketMillis = System.currentTimeMillis()
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun addOrUpdateDevice(device: BluetoothDevice, isPaired: Boolean, rssi: Int?) {
        val rawName = runCatching { device.name }.getOrNull()
        val displayName = rawName?.takeIf { it.isNotBlank() } ?: "Perangkat BT (${device.address.takeLast(5)})"
        val transport = if (device.type == BluetoothDevice.DEVICE_TYPE_LE) {
            JukenTransportType.BLE_GATT
        } else {
            JukenTransportType.CLASSIC_SPP
        }
        val item = JukenBluetoothDevice(
            name = displayName,
            address = device.address,
            transportType = transport,
            isPaired = isPaired,
            rssi = rssi
        )
        _discoveredDevices.update { list ->
            val without = list.filterNot { it.address == item.address }
            (without + item).sortedWith(
                compareByDescending<JukenBluetoothDevice> { it.isLikelyJuken }
                    .thenByDescending { it.isPaired }
                    .thenBy { it.name }
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanInternal() {
        scanTimeoutJob?.cancel()
        runCatching { bluetoothAdapter?.cancelDiscovery() }
        runCatching { bleScanner?.stopScan(bleScanCallback) }
        if (isDiscoveryReceiverRegistered) {
            runCatching { context.unregisterReceiver(discoveryReceiver) }
            isDiscoveryReceiverRegistered = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun cleanupConnection() {
        sppReaderJob?.cancel()
        sppReaderJob = null
        runCatching { sppSocket?.close() }
        sppSocket = null
        runCatching {
            bleGatt?.disconnect()
            bleGatt?.close()
        }
        bleGatt = null
        bleWriteChar = null
    }

    fun release() {
        disconnect()
    }
}
