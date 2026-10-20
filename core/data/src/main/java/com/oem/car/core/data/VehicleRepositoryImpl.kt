package com.oem.car.core.data

import com.oem.car.core.audio.CarAudioManagerDelegate
import com.oem.car.core.model.*
import com.oem.car.core.power.CarPowerLifecycleManager
import com.oem.car.core.uxrestrictions.CarUxRestrictionsEngine
import com.oem.car.core.vehicleservice.VehiclePropertyAdapter
import com.oem.car.core.vehicleservice.VehicleServiceConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object VehicleRepositoryImpl : VehicleRepository {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val serviceManager = VehicleServiceConnectionManager(scope)
    private val uxEngine = CarUxRestrictionsEngine()
    private val powerManager = CarPowerLifecycleManager()
    private val audioDelegate = CarAudioManagerDelegate()

    override val isConnected: StateFlow<Boolean> = serviceManager.isConnected
    override val speedSignal: StateFlow<CarSignal<Float>> = VehiclePropertyAdapter.speedSignal
    override val gearSignal: StateFlow<CarSignal<VehicleGear>> = VehiclePropertyAdapter.gearSignal
    override val powerSignal: StateFlow<CarSignal<PowerState>> = VehiclePropertyAdapter.powerSignal
    override val uxRestrictionState: StateFlow<UxRestrictionState> = uxEngine.uxRestrictionState
    override val audioZoneState: StateFlow<AudioZoneState> = audioDelegate.audioZoneState

    private val _climateState = MutableStateFlow(ClimateState())
    override val climateState: StateFlow<ClimateState> = _climateState.asStateFlow()

    private val _driveMode = MutableStateFlow(DriveMode.COMFORT)
    override val driveMode: StateFlow<DriveMode> = _driveMode.asStateFlow()

    override fun initialize() {
        serviceManager.connect()

        // Combine signals to drive safety rules and power states
        scope.launch {
            speedSignal.collect { speed ->
                uxEngine.evaluate(speed.value, gearSignal.value.value)
            }
        }
        scope.launch {
            gearSignal.collect { gear ->
                uxEngine.evaluate(speedSignal.value.value, gear.value)
            }
        }
        scope.launch {
            powerSignal.collect { power ->
                powerManager.onPowerStateChanged(power.value)
            }
        }
    }

    override fun injectSpeed(speed: Float) {
        serviceManager.injectSpeed(speed)
    }

    override fun injectGear(gear: VehicleGear) {
        serviceManager.injectGear(gear)
    }

    override fun injectPower(powerState: PowerState) {
        serviceManager.injectPower(powerState)
    }

    override fun triggerPtt() {
        audioDelegate.triggerPttEvent()
        serviceManager.triggerPtt()
    }

    override fun updateClimate(transform: (ClimateState) -> ClimateState) {
        _climateState.value = transform(_climateState.value)
    }

    override fun setDriveMode(mode: DriveMode) {
        _driveMode.value = mode
    }
}
