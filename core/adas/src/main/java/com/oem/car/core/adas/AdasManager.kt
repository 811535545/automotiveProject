package com.oem.car.core.adas

import com.oem.car.core.model.AdasPerceptionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ============================================================================
 * ADAS 辅助驾驶与 SR 感知管理器 (ADAS Perception & Control Manager)
 * ============================================================================
 *
 * 【核心职责】
 * 管理 NOA (Navigate on Autopilot) 领航辅助开关、车道保持 (LKA)、巡航设定车速调节以及周边感知目标数据。
 */
class AdasManager {
    private val _state = MutableStateFlow(AdasPerceptionState())
    val state: StateFlow<AdasPerceptionState> = _state.asStateFlow()

    /** 切换 NOA 领航辅助开启/关闭状态 */
    fun toggleNoa() {
        _state.value = _state.value.copy(isNoaActive = !_state.value.isNoaActive)
    }

    /** 切换车道保持 (LKA) 开启/关闭状态 */
    fun toggleLaneKeep() {
        _state.value = _state.value.copy(laneKeepActive = !_state.value.laneKeepActive)
    }

    /** 调整巡航设定车速 (限制在 30 km/h ~ 130 km/h 范围内) */
    fun adjustCruiseSpeed(delta: Float) {
        _state.value = _state.value.copy(
            cruiseSpeed = (_state.value.cruiseSpeed + delta).coerceIn(30f, 130f)
        )
    }
}
