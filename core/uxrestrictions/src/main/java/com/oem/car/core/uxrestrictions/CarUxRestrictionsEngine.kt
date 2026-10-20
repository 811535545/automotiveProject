package com.oem.car.core.uxrestrictions

import com.oem.car.core.common.CarLogger
import com.oem.car.core.model.DrivingSafetyPolicy
import com.oem.car.core.model.UxRestrictionState
import com.oem.car.core.model.VehicleGear
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CarUxRestrictionsEngine {
    private val _uxRestrictionState = MutableStateFlow(UxRestrictionState())
    val uxRestrictionState: StateFlow<UxRestrictionState> = _uxRestrictionState.asStateFlow()

    fun evaluate(speed: Float, gear: VehicleGear) {
        // 演示用防分心规则：与 CarCoreDaemon 共用 DrivingSafetyPolicy，避免两套阈值分叉
        val isDriving = DrivingSafetyPolicy.isDriving(gear, speed)

        val newState = UxRestrictionState(
            isRestricted = isDriving,
            noVideo = isDriving,
            noKeyboard = isDriving,
            maxListLength = DrivingSafetyPolicy.maxListLength(gear, speed)
        )

        if (_uxRestrictionState.value != newState) {
            _uxRestrictionState.value = newState
            CarLogger.i("CarUxRestrictions", "Safety restriction state updated: Driving=$isDriving, NoVideo=${newState.noVideo}")
        }
    }
}
