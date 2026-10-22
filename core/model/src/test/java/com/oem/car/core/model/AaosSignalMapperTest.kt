package com.oem.car.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AaosSignalMapperTest {

    @Test
    fun `0 mps maps to 0 kmh`() {
        assertEquals(0f, AaosSignalMapper.speedMpsToKmh(0f), 0.001f)
    }

    @Test
    fun `80 kmh is about 22 point 22 mps`() {
        assertEquals(80f, AaosSignalMapper.speedMpsToKmh(22.2222f), 0.01f)
    }

    @Test
    fun `aaos park code maps to internal park`() {
        assertEquals(VehicleGear.PARK, AaosSignalMapper.gearFromAaosCode(0x0004))
    }

    @Test
    fun `aaos neutral code maps to internal neutral`() {
        assertEquals(VehicleGear.NEUTRAL, AaosSignalMapper.gearFromAaosCode(0x0001))
    }

    @Test
    fun `aaos drive and reverse keep their meaning`() {
        assertEquals(VehicleGear.DRIVE, AaosSignalMapper.gearFromAaosCode(0x0008))
        assertEquals(VehicleGear.REVERSE, AaosSignalMapper.gearFromAaosCode(0x0002))
    }

    @Test
    fun `unknown aaos gear falls back to drive for fail safe restriction`() {
        assertEquals(VehicleGear.DRIVE, AaosSignalMapper.gearFromAaosCode(0x9999))
    }

    @Test
    fun `aaos available status maps to valid`() {
        assertEquals(SignalStatus.VALID, AaosSignalMapper.statusFromAaos(0))
    }

    @Test
    fun `unavailable and error statuses are preserved`() {
        assertEquals(SignalStatus.UNAVAILABLE, AaosSignalMapper.statusFromAaos(1))
        assertEquals(SignalStatus.ERROR, AaosSignalMapper.statusFromAaos(2))
    }

    @Test
    fun `unknown status maps to invalid`() {
        assertEquals(SignalStatus.INVALID, AaosSignalMapper.statusFromAaos(99))
    }
}
