package com.oem.car.core.energy

import com.oem.car.core.model.EnergyState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ============================================================================
 * 能量管理与充放电管理器 (Vehicle Energy & Charging Manager)
 * ============================================================================
 *
 * 【核心职责】
 * 管理电池 SOC (State of Charge)、续航里程、充电过程模拟（每 800ms 动态增加 SOC 与续航）、
 * 充电限值调节、预约充电以及电池预热功能。
 */
class EnergyManager(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(EnergyState())
    val state: StateFlow<EnergyState> = _state.asStateFlow()

    private var chargingJob: Job? = null

    /** 切换充电开始/停止状态 */
    fun toggleCharging() {
        if (_state.value.isCharging) stopCharging() else startCharging()
    }

    /** 调整充电限值百分比 (限制在 50% ~ 100% 之间) */
    fun adjustChargeLimit(delta: Int) {
        val newLimit = (_state.value.chargeLimitPercent + delta).coerceIn(50, 100)
        _state.update { it.copy(chargeLimitPercent = newLimit) }
        if (_state.value.isCharging && _state.value.socPercent >= newLimit) stopCharging()
    }

    /** 切换预约充电开关 */
    fun toggleScheduledCharging() {
        _state.update { it.copy(isScheduledCharging = !it.isScheduledCharging) }
    }

    /** 切换电池预热开关 */
    fun togglePreconditioning() {
        _state.update { it.copy(isPreconditioning = !it.isPreconditioning) }
    }

    /** 开始充电模拟协程（每 800ms SOC +1%，到达限值自动停止） */
    private fun startCharging() {
        val current = _state.value
        if (current.socPercent >= current.chargeLimitPercent) return

        _state.update { it.copy(isCharging = true, chargingPowerKw = 60f) }
        chargingJob = scope.launch {
            while (isActive) {
                delay(800)
                val state = _state.value
                if (!state.isCharging) break

                val nextSoc = (state.socPercent + 1).coerceAtMost(state.chargeLimitPercent)
                _state.update {
                    it.copy(socPercent = nextSoc, rangeKm = it.rangeKm + 6)
                }
                if (nextSoc >= state.chargeLimitPercent) {
                    _state.update { it.copy(isCharging = false, chargingPowerKw = 0f) }
                    break
                }
            }
        }
    }

    /** 停止充电 */
    private fun stopCharging() {
        chargingJob?.cancel()
        chargingJob = null
        _state.update { it.copy(isCharging = false, chargingPowerKw = 0f) }
    }
}
