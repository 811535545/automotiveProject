package com.oem.car.core.energy

import com.oem.car.core.model.EnergyState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class EnergyManager(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(EnergyState())
    val state: StateFlow<EnergyState> = _state.asStateFlow()

    private var chargingJob: Job? = null

    fun toggleCharging() {
        if (_state.value.isCharging) stopCharging() else startCharging()
    }

    fun adjustChargeLimit(delta: Int) {
        val newLimit = (_state.value.chargeLimitPercent + delta).coerceIn(50, 100)
        _state.update { it.copy(chargeLimitPercent = newLimit) }
        if (_state.value.isCharging && _state.value.socPercent >= newLimit) stopCharging()
    }

    fun toggleScheduledCharging() {
        _state.update { it.copy(isScheduledCharging = !it.isScheduledCharging) }
    }

    fun togglePreconditioning() {
        _state.update { it.copy(isPreconditioning = !it.isPreconditioning) }
    }

    private fun startCharging() {
        val current = _state.value
        if (current.socPercent >= current.chargeLimitPercent) return

        _state.update { it.copy(isCharging = true, chargingPowerKw = 60f) }
        chargingJob = scope.launch {
            while (isActive) {
                delay(800)
                val state = _state.value
                if (!state.isCharging) break

                val nextSoc = (state.socPercent + 1).coerceAtMost(state.chargeLimitPercent)
                _state.update {
                    it.copy(socPercent = nextSoc, rangeKm = it.rangeKm + 6)
                }
                if (nextSoc >= state.chargeLimitPercent) {
                    _state.update { it.copy(isCharging = false, chargingPowerKw = 0f) }
                    break
                }
            }
        }
    }

    private fun stopCharging() {
        chargingJob?.cancel()
        chargingJob = null
        _state.update { it.copy(isCharging = false, chargingPowerKw = 0f) }
    }
}
