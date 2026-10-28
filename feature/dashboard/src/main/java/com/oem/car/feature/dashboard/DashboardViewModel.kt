package com.oem.car.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oem.car.core.data.VehicleRepository
import com.oem.car.core.data.VehicleRepositoryImpl
import com.oem.car.core.model.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ============================================================================
 * 中控仪表 ViewModel (Dashboard ViewModel)
 * ============================================================================
 *
 * 【核心职责】
 * 1. 为 DashboardScreen 提供响应式状态源（连接状态、车速、档位、电源、防分心状态、音频焦点）；
 * 2. 封装总线仿真 Injector 操作方法（注入模拟车速、档位、电源、触发 PTT），解耦 UI 与 Repository。
 */
class DashboardViewModel(
    private val repository: VehicleRepository = VehicleRepositoryImpl
) : ViewModel() {

    /** 车载服务连接状态 */
    val isConnected: StateFlow<Boolean> = repository.isConnected

    /** 车速信号流 */
    val speedSignal: StateFlow<CarSignal<Float>> = repository.speedSignal

    /** 档位信号流 */
    val gearSignal: StateFlow<CarSignal<VehicleGear>> = repository.gearSignal

    /** 电源状态信号流 */
    val powerSignal: StateFlow<CarSignal<PowerState>> = repository.powerSignal

    /** 行车防分心策略状态 */
    val uxRestrictionState: StateFlow<UxRestrictionState> = repository.uxRestrictionState

    /** 音区音频焦点与 PTT 状态 */
    val audioZoneState: StateFlow<AudioZoneState> = repository.audioZoneState

    /** 注入模拟车速 */
    fun injectSpeed(speed: Float) = repository.injectSpeed(speed)

    /** 注入模拟档位 */
    fun injectGear(gear: VehicleGear) = repository.injectGear(gear)

    /** 注入模拟电源状态 */
    fun injectPower(powerState: PowerState) = repository.injectPower(powerState)

    /** 触发方向盘 PTT 语音按键 */
    fun triggerPtt() = repository.triggerPtt()
}
