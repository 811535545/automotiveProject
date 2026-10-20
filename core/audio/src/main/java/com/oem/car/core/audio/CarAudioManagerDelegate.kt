package com.oem.car.core.audio

import com.oem.car.core.common.CarLogger
import com.oem.car.core.model.AudioZoneState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CarAudioManagerDelegate {
    private val _audioZoneState = MutableStateFlow(AudioZoneState())
    val audioZoneState: StateFlow<AudioZoneState> = _audioZoneState.asStateFlow()

    fun triggerPttEvent() {
        CarLogger.i("CarAudioDelegate", "Steering wheel PTT pressed! Requesting transient audio focus & ducking music...")
        _audioZoneState.value = AudioZoneState(
            isPttActive = true,
            lastPttTimestamp = System.currentTimeMillis(),
            audioFocusDuck = true
        )
    }

    fun releasePttEvent() {
        CarLogger.i("CarAudioDelegate", "PTT session finished. Restoring background audio focus.")
        _audioZoneState.value = _audioZoneState.value.copy(
            isPttActive = false,
            audioFocusDuck = false
        )
    }
}
