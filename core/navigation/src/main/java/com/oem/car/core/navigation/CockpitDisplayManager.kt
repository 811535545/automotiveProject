package com.oem.car.core.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 座舱屏幕目标通道 (符合 AAOS 多屏控制 DisplayAreaPolicy 抽象)。
 */
enum class CockpitDisplayTarget(val displayId: Int, val title: String) {
    CENTER_CONSOLE(0, "中控大屏 (Center Console)"),
    INSTRUMENT_CLUSTER(1, "仪表屏 (Instrument Cluster)"),
    PASSENGER_SCREEN(2, "副驾娱乐屏 (Passenger Display)")
}

/**
 * 跨屏投射状态。
 */
data class DisplayProjectionState(
    val activeTarget: CockpitDisplayTarget = CockpitDisplayTarget.CENTER_CONSOLE,
    val isNavProjectedToCluster: Boolean = true,
    val clusterTurnByTurnText: String = "前方 200 米右转进入高架入口"
)

/**
 * 座舱多屏分发与投射管理器 (Cockpit Multi-Display Dispatcher)。
 */
class CockpitDisplayManager {
    private val _projectionState = MutableStateFlow(DisplayProjectionState())
    val projectionState: StateFlow<DisplayProjectionState> = _projectionState.asStateFlow()

    fun toggleNavClusterProjection() {
        val currentState = _projectionState.value
        _projectionState.value = currentState.copy(
            isNavProjectedToCluster = !currentState.isNavProjectedToCluster
        )
    }

    fun updateClusterTbtCard(instruction: String) {
        _projectionState.value = _projectionState.value.copy(
            clusterTurnByTurnText = instruction
        )
    }

    fun setActiveDisplay(target: CockpitDisplayTarget) {
        _projectionState.value = _projectionState.value.copy(
            activeTarget = target
        )
    }
}
