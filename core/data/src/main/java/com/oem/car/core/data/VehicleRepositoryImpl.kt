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

/**
 * ============================================================================
 * 车载核心统一数据门面实现类 (Vehicle Repository Implementation)
 * ============================================================================
 *
 * 【核心职责】
 * 1. 作为座舱应用的中央数据仓库 (Single Source of Truth)；
 * 2. 聚合 10 多个领域子系统（空调、数字钥匙、T-BOX、ADAS、账号、导航、电话、能量、健康）；
 * 3. 持有并管理 `VehiclePropertyProvider`（双通道 VHAL / Mock Binder 桥接）；
 * 4. 监听车辆信号（车速、档位、电源），并实时驱动行车防分心引擎与电源状态机。
 */
object VehicleRepositoryImpl : VehicleRepository {
    private val scope = CoroutineScope(Dispatchers.Default)

    /** 底层车辆属性服务提供者（双通道：Native AAOS 或 Mock Binder） */
    private var propertyProvider: com.oem.car.core.vehicleservice.VehiclePropertyProvider? = null

    /** 车载服务连接状态通道 */
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    /** 各业务领域子系统实例（通过依赖注入容器获取） */
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

    /** 信号流暴露代理 */
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

    /** 空调状态可变通道 */
    private val _climateState = MutableStateFlow(ClimateState())
    override val climateState: StateFlow<ClimateState> = _climateState.asStateFlow()

    /** 驾驶模式可变通道 */
    private val _driveMode = MutableStateFlow(DriveMode.COMFORT)
    override val driveMode: StateFlow<DriveMode> = _driveMode.asStateFlow()

    /**
     * 初始化仓库。通过 AtomicBoolean 确保全局只执行一次。
     * 负责启动 VehiclePropertyProvider 并建立信号监听流以驱动防分心和电源状态机。
     */
    override fun initialize(context: Context) {
        if (!initialized.compareAndSet(false, true)) return

        val provider = VehicleServiceContainer.getPropertyProvider(context.applicationContext)
        propertyProvider = provider
        scope.launch {
            provider.isConnected.collect { _isConnected.value = it }
        }
        provider.connect()

        // 监听车速与档位信号，实时驱动行车防分心安全引擎
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
        // 监听电源状态，实时驱动电源生命周期管理
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
