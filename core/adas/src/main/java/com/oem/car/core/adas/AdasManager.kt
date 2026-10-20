package com.oem.car.core.adas

import com.oem.car.core.model.AdasPerceptionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdasManager {
    private val _state = MutableStateFlow(AdasPerceptionState())
    val state: StateFlow<AdasPerceptionState> = _state.asStateFlow()

    fun toggleNoa() {
        _state.value = _state.value.copy(isNoaActive = !_state.value.isNoaActive)
    }

    fun toggleLaneKeep() {
        _state.value = _state.value.copy(laneKeepActive = !_state.value.laneKeepActive)
    }

    fun adjustCruiseSpeed(delta: Float) {
        _state.value = _state.value.copy(
            cruiseSpeed = (_state.value.cruiseSpeed + delta).coerceIn(30f, 130f)
        )
    }
}
