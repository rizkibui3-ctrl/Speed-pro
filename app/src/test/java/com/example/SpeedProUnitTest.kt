package com.example

import com.example.data.model.JukenBluetoothDevice
import com.example.data.model.JukenTransportType
import com.example.data.model.SpeedUnit
import com.example.ui.components.bearingToCardinal
import com.example.ui.components.formatAccelSeconds
import com.example.ui.components.formatDurationHms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedProUnitTest {

    @Test
    fun speedConversions_areAccurate() {
        // 10 m/s == 36 km/h
        assertEquals(36f, SpeedUnit.KMH.convertSpeed(10f), 0.01f)
        // 10 m/s == ~22.37 mph
        assertEquals(22.37f, SpeedUnit.MPH.convertSpeed(10f), 0.05f)
    }

    @Test
    fun jukenDeviceDetection_matchesBrtModules() {
        val juken = JukenBluetoothDevice(
            name = "BRT-JUKEN5PLUS",
            address = "00:11:22:33:44:55",
            transportType = JukenTransportType.CLASSIC_SPP,
            isPaired = true
        )
        val headset = JukenBluetoothDevice(
            name = "TWS Earbuds",
            address = "AA:BB:CC:DD:EE:FF",
            transportType = JukenTransportType.CLASSIC_SPP,
            isPaired = false
        )
        assertTrue(juken.isLikelyJuken)
        assertFalse(headset.isLikelyJuken)
    }

    @Test
    fun formattingHelpers_formatProperly() {
        assertEquals("01:01:05", formatDurationHms(3665_000L))
        assertEquals("4.52 s", formatAccelSeconds(4520L))
        assertEquals("--.-- s", formatAccelSeconds(null))
        assertEquals("N", bearingToCardinal(0f))
        assertEquals("NE", bearingToCardinal(45f))
        assertEquals("SW", bearingToCardinal(225f))
    }
}
