package com.oem.car.core.audio

import com.oem.car.core.common.CarLogger
import com.oem.car.core.model.AudioZoneState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CarAudioManagerDelegate {
    val multiZoneManager = MultiZoneAudioManager()

    private val _audioZoneState = MutableStateFlow(AudioZoneState())
    val audioZoneState: StateFlow<AudioZoneState> = _audioZoneState.asStateFlow()

    fun triggerPttEvent() {
        CarLogger.i("CarAudioDelegate", "Steering wheel PTT pressed! Requesting transient audio focus & ducking music...")
        multiZoneManager.requestAudioFocus(AudioZone.DRIVER_ZONE_0, CarAudioUsage.VOICE_COMMAND)
        _audioZoneState.value = AudioZoneState(
            isPttActive = true,
            lastPttTimestamp = System.currentTimeMillis(),
            audioFocusDuck = true
        )
    }

    fun releasePttEvent() {
        CarLogger.i("CarAudioDelegate", "PTT session finished. Restoring background audio focus.")
        multiZoneManager.abandonAudioFocus(AudioZone.DRIVER_ZONE_0, CarAudioUsage.VOICE_COMMAND)
        _audioZoneState.value = _audioZoneState.value.copy(
            isPttActive = false,
            audioFocusDuck = false
        )
    }
}
