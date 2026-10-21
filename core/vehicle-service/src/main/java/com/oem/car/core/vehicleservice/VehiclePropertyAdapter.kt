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

    /**
     * @param speedKmh 车速，单位 km/h。
     * @param status Mock 路径按 0..300 的合理区间推断；AAOS 路径直接透传
     *   CarPropertyValue.getStatus() 映射后的状态。
     * @param timestampMs 事件时间（毫秒）。Mock 路径是墙钟时间，AAOS 路径由
     *   elapsedRealtimeNanos 换算而来，只用于判断新旧，不参与业务计算。
     * @param areaId AAOS 的区域 ID；车速是 GLOBAL 属性，通常为 0。
     */
    fun updateSpeed(
        speedKmh: Float,
        status: SignalStatus = if (speedKmh in 0f..300f) {
            SignalStatus.VALID
        } else {
            SignalStatus.INVALID
        },
        timestampMs: Long = System.currentTimeMillis(),
        areaId: Int = 0
    ) {
        _speedSignal.value = CarSignal(
            value = speedKmh,
            status = status,
            timestamp = timestampMs,
            areaId = areaId
        )
    }

    /**
     * @param gearCode 项目内部档位码（1=P, 2=R, 4=N, 8=D），不是 AAOS 的档位码。
     *   AAOS 的 1=N/4=P 顺序不同，必须在桥接层先用 AaosSignalMapper 转换。
     */
    fun updateGear(
        gearCode: Int,
        status: SignalStatus = SignalStatus.VALID,
        timestampMs: Long = System.currentTimeMillis(),
        areaId: Int = 0
    ) {
        _gearSignal.value = CarSignal(
            value = VehicleGear.fromCode(gearCode),
            status = status,
            timestamp = timestampMs,
            areaId = areaId
        )
    }

    fun updatePowerState(powerCode: Int) {
        _powerSignal.value = CarSignal(PowerState.fromCode(powerCode))
    }

    fun updateDistractionState(isRestricted: Boolean) {
        _distractionSignal.value = CarSignal(isRestricted)
    }
}
