package com.oem.car.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oem.car.core.data.VehicleRepository
import com.oem.car.core.data.VehicleRepositoryImpl
import com.oem.car.core.model.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    private val repository: VehicleRepository = VehicleRepositoryImpl
) : ViewModel() {

    val isConnected: StateFlow<Boolean> = repository.isConnected
    val speedSignal: StateFlow<CarSignal<Float>> = repository.speedSignal
    val gearSignal: StateFlow<CarSignal<VehicleGear>> = repository.gearSignal
    val powerSignal: StateFlow<CarSignal<PowerState>> = repository.powerSignal
    val uxRestrictionState: StateFlow<UxRestrictionState> = repository.uxRestrictionState
    val audioZoneState: StateFlow<AudioZoneState> = repository.audioZoneState

    init {
        repository.initialize()
    }

    fun injectSpeed(speed: Float) = repository.injectSpeed(speed)
    fun injectGear(gear: VehicleGear) = repository.injectGear(gear)
    fun injectPower(powerState: PowerState) = repository.injectPower(powerState)
    fun triggerPtt() = repository.triggerPtt()
}
