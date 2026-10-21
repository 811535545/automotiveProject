package com.oem.car.core.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class MultiZoneAudioManagerTest {

    @Test
    fun testDriverNavFocusDucksOnlyDriverZone() {
        val manager = MultiZoneAudioManager()

        // Driver Zone requests Navigation focus
        manager.requestAudioFocus(AudioZone.DRIVER_ZONE_0, CarAudioUsage.NAVIGATION)

        val statuses = manager.zoneStatuses.value

        // Driver zone should duck to 0.3f
        assertEquals(0.3f, statuses[AudioZone.DRIVER_ZONE_0]?.volumeMultiplier ?: 0f, 0.01f)

        // Passenger zone remains unaffected (1.0f)
        assertEquals(1.0f, statuses[AudioZone.PASSENGER_ZONE_1]?.volumeMultiplier ?: 0f, 0.01f)
    }

    @Test
    fun testGlobalVoiceCommandDucksAllZones() {
        val manager = MultiZoneAudioManager()

        // Voice Command triggered
        manager.requestAudioFocus(AudioZone.DRIVER_ZONE_0, CarAudioUsage.VOICE_COMMAND)

        val statuses = manager.zoneStatuses.value

        // All active zones ducked to 0.1f
        assertEquals(0.1f, statuses[AudioZone.DRIVER_ZONE_0]?.volumeMultiplier ?: 0f, 0.01f)
        assertEquals(0.1f, statuses[AudioZone.PASSENGER_ZONE_1]?.volumeMultiplier ?: 0f, 0.01f)
    }
}
