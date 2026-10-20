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
    val digitalKeyState: StateFlow<DigitalKeyState>
    val telematicsState: StateFlow<TelematicsState>
    val adasState: StateFlow<AdasPerceptionState>
    val userProfile: StateFlow<UserProfile>
    val navigationState: StateFlow<NavigationState>
    val phoneState: StateFlow<PhoneState>
    val energyState: StateFlow<EnergyState>
    val vehicleHealthState: StateFlow<VehicleHealthState>
    val navigationPresets: List<NavigationDestination>

    fun initialize()
    fun injectSpeed(speed: Float)
    fun injectGear(gear: VehicleGear)
    fun injectPower(powerState: PowerState)
    fun triggerPtt()
    fun releasePtt()
    fun updateClimate(transform: (ClimateState) -> ClimateState)
    fun setDriveMode(mode: DriveMode)
    fun moveKeyCloser()
    fun moveKeyAway()
    fun cycleKeyTech()
    fun toggleDoorLock()
    fun toggleSentryMode()
    fun sendRemoteCommand(command: String)
    fun startOtaUpdate()
    fun toggleNoa()
    fun toggleLaneKeep()
    fun adjustCruiseSpeed(delta: Float)
    fun verifyDriverFace()
    fun cycleSeatPosition()
    fun adjustPreferredTemp(delta: Float)
    fun applyUserProfile()
    fun startNavigation(destination: NavigationDestination)
    fun advanceNavigation()
    fun stopNavigation()
    fun togglePhoneBluetooth()
    fun dialContact(contact: Contact)
    fun simulateIncomingCall(contact: Contact)
    fun answerCall()
    fun hangUpCall()
    fun toggleCallMute()
    fun toggleCharging()
    fun adjustChargeLimit(delta: Int)
    fun toggleScheduledCharging()
    fun togglePreconditioning()
    fun scanVehicleHealth()
    fun clearDtcs()
}
