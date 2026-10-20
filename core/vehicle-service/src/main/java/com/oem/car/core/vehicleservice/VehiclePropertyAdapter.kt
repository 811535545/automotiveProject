package com.oem.car.core.vehicleservice

import com.oem.car.core.model.CarSignal
import com.oem.car.core.model.PowerState
import com.oem.car.core.model.SignalStatus
import com.oem.car.core.model.VehicleGear
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VehiclePropertyAdapter {
    private val _speedSignal = MutableStateFlow(CarSignal(0f))
    val speedSignal: StateFlow<CarSignal<Float>> = _speedSignal.asStateFlow()

    private val _gearSignal = MutableStateFlow(CarSignal(VehicleGear.PARK))
    val gearSignal: StateFlow<CarSignal<VehicleGear>> = _gearSignal.asStateFlow()

    private val _powerSignal = MutableStateFlow(CarSignal(PowerState.RUNNING))
    val powerSignal: StateFlow<CarSignal<PowerState>> = _powerSignal.asStateFlow()

    private val _distractionSignal = MutableStateFlow(CarSignal(false))
    val distractionSignal: StateFlow<CarSignal<Boolean>> = _distractionSignal.asStateFlow()

    fun updateSpeed(speed: Float) {
        _speedSignal.value = CarSignal(
            value = speed,
            status = if (speed in 0f..300f) SignalStatus.VALID else SignalStatus.INVALID
        )
    }

    fun updateGear(gearCode: Int) {
        _gearSignal.value = CarSignal(VehicleGear.fromCode(gearCode))
    }

    fun updatePowerState(powerCode: Int) {
        _powerSignal.value = CarSignal(PowerState.fromCode(powerCode))
    }

    fun updateDistractionState(isRestricted: Boolean) {
        _distractionSignal.value = CarSignal(isRestricted)
    }
}
