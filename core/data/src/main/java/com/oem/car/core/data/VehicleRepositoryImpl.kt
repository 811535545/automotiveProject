package com.oem.car.core.data

import android.content.Context
import com.oem.car.core.model.AdasPerceptionState
import com.oem.car.core.model.AudioZoneState
import com.oem.car.core.model.CarSignal
import com.oem.car.core.model.ClimateState
import com.oem.car.core.model.Contact
import com.oem.car.core.model.DigitalKeyState
import com.oem.car.core.model.DriveMode
import com.oem.car.core.model.EnergyState
import com.oem.car.core.model.NavigationDestination
import com.oem.car.core.model.NavigationState
import com.oem.car.core.model.PhoneState
import com.oem.car.core.model.PowerState
import com.oem.car.core.model.TelematicsState
import com.oem.car.core.model.UserProfile
import com.oem.car.core.model.UxRestrictionState
import com.oem.car.core.model.VehicleGear
import com.oem.car.core.model.VehicleHealthState
import com.oem.car.core.vehicleservice.VehiclePropertyAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object VehicleRepositoryImpl : VehicleRepository {
    private val scope = CoroutineScope(Dispatchers.Default)

    private var propertyProvider: com.oem.car.core.vehicleservice.VehiclePropertyProvider? = null

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val uxEngine = VehicleServiceContainer.uxEngine
    private val powerManager = VehicleServiceContainer.powerManager
    private val audioDelegate = VehicleServiceContainer.audioDelegate
    private val digitalKeyManager = VehicleServiceContainer.digitalKeyManager
    private val telematicsManager = VehicleServiceContainer.telematicsManager
    private val adasManager = VehicleServiceContainer.adasManager
    private val accountManager = VehicleServiceContainer.accountManager
    private val navigationManager = VehicleServiceContainer.navigationManager
    private val phoneManager = VehicleServiceContainer.phoneManager
    private val energyManager = VehicleServiceContainer.energyManager
    private val healthManager = VehicleServiceContainer.healthManager
    private val initialized = AtomicBoolean(false)

    override val speedSignal: StateFlow<CarSignal<Float>> = VehiclePropertyAdapter.speedSignal
    override val gearSignal: StateFlow<CarSignal<VehicleGear>> = VehiclePropertyAdapter.gearSignal
    override val powerSignal: StateFlow<CarSignal<PowerState>> = VehiclePropertyAdapter.powerSignal
    override val uxRestrictionState: StateFlow<UxRestrictionState> = uxEngine.uxRestrictionState
    override val audioZoneState: StateFlow<AudioZoneState> = audioDelegate.audioZoneState
    override val digitalKeyState: StateFlow<DigitalKeyState> = digitalKeyManager.state
    override val telematicsState: StateFlow<TelematicsState> = telematicsManager.state
    override val adasState: StateFlow<AdasPerceptionState> = adasManager.state
    override val userProfile: StateFlow<UserProfile> = accountManager.profile
    override val navigationState: StateFlow<NavigationState> = navigationManager.state
    override val phoneState: StateFlow<PhoneState> = phoneManager.state
    override val energyState: StateFlow<EnergyState> = energyManager.state
    override val vehicleHealthState: StateFlow<VehicleHealthState> = healthManager.state
    override val navigationPresets: List<NavigationDestination> = navigationManager.presets

    private val _climateState = MutableStateFlow(ClimateState())
    override val climateState: StateFlow<ClimateState> = _climateState.asStateFlow()

    private val _driveMode = MutableStateFlow(DriveMode.COMFORT)
    override val driveMode: StateFlow<DriveMode> = _driveMode.asStateFlow()

    override fun initialize(context: Context) {
        if (!initialized.compareAndSet(false, true)) return

        val provider = VehicleServiceContainer.getPropertyProvider(context.applicationContext)
        propertyProvider = provider
        scope.launch {
            provider.isConnected.collect { _isConnected.value = it }
        }
        provider.connect()

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
        propertyProvider?.injectSpeed(speed)
    }

    override fun injectGear(gear: VehicleGear) {
        propertyProvider?.injectGear(gear)
    }

    override fun injectPower(powerState: PowerState) {
        propertyProvider?.injectPower(powerState)
    }

    override fun triggerPtt() {
        audioDelegate.triggerPttEvent()
        propertyProvider?.triggerPtt()
    }

    override fun releasePtt() {
        audioDelegate.releasePttEvent()
    }

    override fun updateClimate(transform: (ClimateState) -> ClimateState) {
        _climateState.value = transform(_climateState.value)
    }

    override fun setDriveMode(mode: DriveMode) {
        _driveMode.value = mode
    }

    override fun moveKeyCloser() = digitalKeyManager.moveCloser()

    override fun moveKeyAway() = digitalKeyManager.moveAway()

    override fun cycleKeyTech() = digitalKeyManager.cycleTech()

    override fun toggleDoorLock() = digitalKeyManager.toggleDoorLock()

    override fun toggleSentryMode() = telematicsManager.toggleSentryMode()

    override fun sendRemoteCommand(command: String) = telematicsManager.sendRemoteCommand(command)

    override fun startOtaUpdate() = telematicsManager.startOtaUpdate()

    override fun toggleNoa() = adasManager.toggleNoa()

    override fun toggleLaneKeep() = adasManager.toggleLaneKeep()

    override fun adjustCruiseSpeed(delta: Float) = adasManager.adjustCruiseSpeed(delta)

    override fun verifyDriverFace() = accountManager.verifyFace()

    override fun cycleSeatPosition() = accountManager.cycleSeatPosition()

    override fun adjustPreferredTemp(delta: Float) = accountManager.adjustPreferredTemp(delta)

    override fun applyUserProfile() {
        val profile = accountManager.profile.value
        if (!profile.isFaceIdVerified) return
        _climateState.value = _climateState.value.copy(driverTemp = profile.preferredTemp)
    }

    override fun startNavigation(destination: NavigationDestination) = navigationManager.startRoute(destination)

    override fun advanceNavigation() = navigationManager.advance()

    override fun stopNavigation() = navigationManager.stopRoute()

    override fun togglePhoneBluetooth() = phoneManager.toggleBluetooth()

    override fun dialContact(contact: Contact) = phoneManager.dial(contact)

    override fun simulateIncomingCall(contact: Contact) = phoneManager.simulateIncomingCall(contact)

    override fun answerCall() = phoneManager.answerCall()

    override fun hangUpCall() = phoneManager.hangUp()

    override fun toggleCallMute() = phoneManager.toggleMute()

    override fun toggleCharging() = energyManager.toggleCharging()

    override fun adjustChargeLimit(delta: Int) = energyManager.adjustChargeLimit(delta)

    override fun toggleScheduledCharging() = energyManager.toggleScheduledCharging()

    override fun togglePreconditioning() = energyManager.togglePreconditioning()

    override fun scanVehicleHealth() = healthManager.scan()

    override fun clearDtcs() = healthManager.clearDtcs()
}
