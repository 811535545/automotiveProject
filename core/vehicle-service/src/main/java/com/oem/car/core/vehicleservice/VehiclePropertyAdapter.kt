package com.oem.car.core.vehicleservice

import com.oem.car.core.common.CarLogger
import com.oem.car.core.model.CarSignal
import com.oem.car.core.model.PowerState
import com.oem.car.core.model.SignalStatus
import com.oem.car.core.model.VehicleGear
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 车规信号转换适配器（含信号超时降级看门狗 Signal Timeout Watchdog）。
 */
object VehiclePropertyAdapter {
    private const val SIGNAL_TIMEOUT_MS = 5000L
    private const val TAG = "VehiclePropertyAdapter"

    private val _speedSignal = MutableStateFlow(CarSignal(0f))
    val speedSignal: StateFlow<CarSignal<Float>> = _speedSignal.asStateFlow()

    private val _gearSignal = MutableStateFlow(CarSignal(VehicleGear.PARK))
    val gearSignal: StateFlow<CarSignal<VehicleGear>> = _gearSignal.asStateFlow()

    private val _powerSignal = MutableStateFlow(CarSignal(PowerState.RUNNING))
    val powerSignal: StateFlow<CarSignal<PowerState>> = _powerSignal.asStateFlow()

    private val _distractionSignal = MutableStateFlow(CarSignal(false))
    val distractionSignal: StateFlow<CarSignal<Boolean>> = _distractionSignal.asStateFlow()

    fun startSignalTimeoutWatchdog(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
        scope.launch {
            while (isActive) {
                delay(2000L)
                val now = SystemClockOrWall.currentTimeMillis()
                val speedSig = _speedSignal.value
                if (speedSig.isValid && (now - speedSig.timestamp > SIGNAL_TIMEOUT_MS)) {
                    CarLogger.w(TAG, "Speed signal timeout (> ${SIGNAL_TIMEOUT_MS}ms); degrading to UNAVAILABLE")
                    _speedSignal.value = speedSig.copy(status = SignalStatus.UNAVAILABLE)
                }
            }
        }
    }

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

    private object SystemClockOrWall {
        fun currentTimeMillis(): Long = System.currentTimeMillis()
    }
}
