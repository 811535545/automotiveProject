package com.oem.car.core.digitalkey

import com.oem.car.core.model.DigitalKeyState
import com.oem.car.core.model.KeyTech
import com.oem.car.core.model.ProximityZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DigitalKeyManager {
    private val _state = MutableStateFlow(
        DigitalKeyState(
            proximityZone = ProximityZone.FAR,
            distanceMeters = 12f,
            isDoorUnlocked = false,
            isEngineStartAllowed = false
        )
    )
    val state: StateFlow<DigitalKeyState> = _state.asStateFlow()

    fun moveCloser() = moveZone(1)

    fun moveAway() = moveZone(-1)

    fun cycleTech() {
        val next = KeyTech.entries[(_state.value.activeTech.ordinal + 1) % KeyTech.entries.size]
        _state.value = _state.value.copy(activeTech = next)
    }

    fun toggleDoorLock() {
        _state.value = _state.value.copy(isDoorUnlocked = !_state.value.isDoorUnlocked)
    }

    private fun moveZone(delta: Int) {
        val current = _state.value
        val nextOrdinal = (current.proximityZone.ordinal + delta)
            .coerceIn(0, ProximityZone.entries.lastIndex)
        val nextZone = ProximityZone.entries[nextOrdinal]
        val doorUnlocked = when (nextZone) {
            ProximityZone.FAR, ProximityZone.APPROACHING -> false
            ProximityZone.WELCOME -> current.isDoorUnlocked
            ProximityZone.UNLOCK, ProximityZone.CABIN_INSIDE -> true
        }
        _state.value = current.copy(
            proximityZone = nextZone,
            distanceMeters = distanceFor(nextZone),
            isDoorUnlocked = doorUnlocked,
            isEngineStartAllowed = nextZone == ProximityZone.CABIN_INSIDE && current.isSeAuthenticated
        )
    }

    private fun distanceFor(zone: ProximityZone): Float = when (zone) {
        ProximityZone.FAR -> 12f
        ProximityZone.APPROACHING -> 8f
        ProximityZone.WELCOME -> 4f
        ProximityZone.UNLOCK -> 1.2f
        ProximityZone.CABIN_INSIDE -> 0.5f
    }
}
