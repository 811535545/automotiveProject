package com.oem.car.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanMessageCodecTest {

    @Test
    fun testCanFrameEncodingAndDecoding() {
        val speed = 65.5f
        val gear = 8 // D gear
        val power = 0 // RUNNING
        val counter = 5

        val frame = CanMessageCodec.encodeVehicleSignal(
            speed = speed,
            gear = gear,
            powerState = power,
            counter = counter
        )

        assertEquals(CanMessageCodec.CAN_ID_VEHICLE_DYNAMIC, frame.id)
        assertEquals(8, frame.dlc)

        val decoded = CanMessageCodec.decodeVehicleSignal(frame)
        assertNotNull(decoded)
        decoded?.let {
            assertEquals(speed, it.speed, 0.01f)
            assertEquals(gear, it.gear)
            assertEquals(power, it.powerState)
            assertEquals(counter, it.counter)
            assertTrue("CRC8 checksum must be valid", it.isCrcValid)
        }
    }
}
