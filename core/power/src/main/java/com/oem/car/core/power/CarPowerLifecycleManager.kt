package com.oem.car.core.power

import com.oem.car.core.common.CarLogger
import com.oem.car.core.model.PowerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CarPowerLifecycleManager {
    private val _powerState = MutableStateFlow(PowerState.RUNNING)
    val powerState: StateFlow<PowerState> = _powerState.asStateFlow()

    private val listeners = mutableListOf<PowerStateListener>()

    interface PowerStateListener {
        fun onPrepareShutdown() {}
        fun onSuspendToRam() {}
        fun onWakeUp() {}
    }

    fun registerListener(listener: PowerStateListener) {
        listeners.add(listener)
    }

    fun unregisterListener(listener: PowerStateListener) {
        listeners.remove(listener)
    }

    fun onPowerStateChanged(newState: PowerState) {
        CarLogger.i("CarPowerManager", "Vehicle Power State Transition: ${_powerState.value} -> $newState")
        _powerState.value = newState

        when (newState) {
            PowerState.STR_SLEEP -> listeners.forEach { it.onSuspendToRam() }
            PowerState.SHUTDOWN_PREPARE -> listeners.forEach { it.onPrepareShutdown() }
            PowerState.RUNNING -> listeners.forEach { it.onWakeUp() }
            else -> {}
        }
    }
}
