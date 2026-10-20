package com.oem.car.core.data

import com.oem.car.core.account.AccountManager
import com.oem.car.core.adas.AdasManager
import com.oem.car.core.audio.CarAudioManagerDelegate
import com.oem.car.core.digitalkey.DigitalKeyManager
import com.oem.car.core.energy.EnergyManager
import com.oem.car.core.health.VehicleHealthManager
import com.oem.car.core.model.*
import com.oem.car.core.navigation.NavigationManager
import com.oem.car.core.phone.PhoneManager
import com.oem.car.core.power.CarPowerLifecycleManager
import com.oem.car.core.telematics.TelematicsManager
import com.oem.car.core.uxrestrictions.CarUxRestrictionsEngine
import com.oem.car.core.vehicleservice.VehiclePropertyAdapter
import com.oem.car.core.vehicleservice.VehicleServiceConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object VehicleRepositoryImpl : VehicleRepository {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val serviceManager = VehicleServiceConnectionManager(scope)
    private val uxEngine = CarUxRestrictionsEngine()
    private val powerManager = CarPowerLifecycleManager()
    private val audioDelegate = CarAudioManagerDelegate()
    private val digitalKeyManager = DigitalKeyManager()
    private val telematicsManager = TelematicsManager(scope)
    private val adasManager = AdasManager()
    private val accountManager = AccountManager()
    private val navigationManager = NavigationManager()
    private val phoneManager = PhoneManager(scope)
    private val energyManager = EnergyManager(scope)
    private val healthManager = VehicleHealthManager(scope)
    private val initialized = AtomicBoolean(false)

    override val isConnected: StateFlow<Boolean> = serviceManager.isConnected
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

    override fun initialize() {
        if (!initialized.compareAndSet(false, true)) return

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
