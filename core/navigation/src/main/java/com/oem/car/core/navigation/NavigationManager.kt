package com.oem.car.core.navigation

import com.oem.car.core.model.NavigationDestination
import com.oem.car.core.model.NavigationState
import com.oem.car.core.model.RouteStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

class NavigationManager {
    private val _state = MutableStateFlow(NavigationState())
    val state: StateFlow<NavigationState> = _state.asStateFlow()

    val presets = listOf(
        NavigationDestination("公司", "科技大道 88 号", 18.6f, 26),
        NavigationDestination("家", "滨江花园", 12.4f, 19),
        NavigationDestination("望京充电站", "望京 SOHO B2", 6.8f, 12)
    )

    private val instructions = listOf(
        "前方 200 米右转进入科技大道",
        "沿主路直行 1.2 公里",
        "前方 500 米靠右驶出匝道",
        "即将到达目的地，请减速"
    )

    private var step = 0

    fun startRoute(destination: NavigationDestination) {
        step = 0
        _state.value = NavigationState(
            status = RouteStatus.NAVIGATING,
            destination = destination,
            nextInstruction = instructions[0],
            distanceToTurnMeters = 200,
            remainingKm = destination.distanceKm,
            etaMinutes = destination.etaMinutes,
            speedLimitKph = if (destination.name == "望京充电站") 40 else 80,
            laneHint = "保持左侧车道",
            progress = 0f
        )
    }

    fun advance() {
        val current = _state.value
        val destination = current.destination ?: return
        if (current.status != RouteStatus.NAVIGATING) return

        step++
        if (step >= instructions.size) {
            _state.value = current.copy(
                status = RouteStatus.ARRIVED,
                nextInstruction = "已到达 ${destination.name}",
                distanceToTurnMeters = 0,
                remainingKm = 0f,
                etaMinutes = 0,
                laneHint = "",
                progress = 1f
            )
            return
        }

        val progress = step.toFloat() / instructions.size
        _state.value = current.copy(
            nextInstruction = instructions[step],
            distanceToTurnMeters = 200 + step * 350,
            remainingKm = max(0.3f, destination.distanceKm * (1f - progress)),
            etaMinutes = max(1, (destination.etaMinutes * (1f - progress)).toInt()),
            laneHint = if (step == 1) "保持中间车道" else "靠右车道",
            progress = progress
        )
    }

    fun stopRoute() {
        step = 0
        _state.value = NavigationState()
    }
}
