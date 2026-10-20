package com.oem.car.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrivingSafetyPolicyTest {

    @Test
    fun `parked vehicle is not restricted`() {
        assertFalse(DrivingSafetyPolicy.isDriving(VehicleGear.PARK, 0f))
    }

    @Test
    fun `drive gear is restricted even at standstill`() {
        assertTrue(DrivingSafetyPolicy.isDriving(VehicleGear.DRIVE, 0f))
        // 服务端 CarCoreDaemon 只有 AIDL 传来的 int 档位，单独覆盖 raw code 重载
        assertTrue(DrivingSafetyPolicy.isDriving(VehicleGear.DRIVE.code, 0f))
    }

    @Test
    fun `reverse and neutral are not restricted at standstill`() {
        assertFalse(DrivingSafetyPolicy.isDriving(VehicleGear.REVERSE, 0f))
        assertFalse(DrivingSafetyPolicy.isDriving(VehicleGear.NEUTRAL, 0f))
    }

    @Test
    fun `speed above threshold is restricted regardless of gear`() {
        assertTrue(DrivingSafetyPolicy.isDriving(VehicleGear.PARK, 0.51f))
        assertTrue(DrivingSafetyPolicy.isDriving(VehicleGear.REVERSE, 30f))
        assertTrue(DrivingSafetyPolicy.isDriving(VehicleGear.NEUTRAL, 80f))
    }

    @Test
    fun `speed exactly at threshold is not restricted`() {
        assertFalse(DrivingSafetyPolicy.isDriving(VehicleGear.PARK, 0.5f))
    }

    @Test
    fun `list length is unlimited when parked`() {
        assertEquals(Int.MAX_VALUE, DrivingSafetyPolicy.maxListLength(VehicleGear.PARK, 0f))
    }

    @Test
    fun `list length is ten while driving below high speed`() {
        assertEquals(10, DrivingSafetyPolicy.maxListLength(VehicleGear.DRIVE, 0f))
        assertEquals(10, DrivingSafetyPolicy.maxListLength(VehicleGear.DRIVE.code, 0f))
        assertEquals(10, DrivingSafetyPolicy.maxListLength(VehicleGear.PARK, 15.0f))
    }

    @Test
    fun `list length is five above high speed threshold`() {
        assertEquals(5, DrivingSafetyPolicy.maxListLength(VehicleGear.PARK, 15.1f))
        assertEquals(5, DrivingSafetyPolicy.maxListLength(VehicleGear.DRIVE, 120f))
    }
}
