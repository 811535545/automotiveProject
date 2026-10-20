package com.oem.car.core.data

import com.oem.car.core.model.*
import kotlinx.coroutines.flow.StateFlow

interface VehicleRepository {
    val isConnected: StateFlow<Boolean>
    val speedSignal: StateFlow<CarSignal<Float>>
    val gearSignal: StateFlow<CarSignal<VehicleGear>>
    val powerSignal: StateFlow<CarSignal<PowerState>>
    val uxRestrictionState: StateFlow<UxRestrictionState>
    val audioZoneState: StateFlow<AudioZoneState>
    val climateState: StateFlow<ClimateState>
    val driveMode: StateFlow<DriveMode>

    fun initialize()
    fun injectSpeed(speed: Float)
    fun injectGear(gear: VehicleGear)
    fun injectPower(powerState: PowerState)
    fun triggerPtt()
    fun updateClimate(transform: (ClimateState) -> ClimateState)
    fun setDriveMode(mode: DriveMode)
}
