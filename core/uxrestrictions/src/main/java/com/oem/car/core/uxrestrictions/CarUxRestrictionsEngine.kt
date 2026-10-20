package com.oem.car.core.uxrestrictions

import com.oem.car.core.common.CarLogger
import com.oem.car.core.model.UxRestrictionState
import com.oem.car.core.model.VehicleGear
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CarUxRestrictionsEngine {
    private val _uxRestrictionState = MutableStateFlow(UxRestrictionState())
    val uxRestrictionState: StateFlow<UxRestrictionState> = _uxRestrictionState.asStateFlow()

    fun evaluate(speed: Float, gear: VehicleGear) {
        // 车规安全防分心逻辑 (遵循 GB 44495 / NHTSA 标准)
        val isDriving = gear == VehicleGear.DRIVE || speed > 0.5f
        val isHighSpeed = speed > 15.0f

        val newState = UxRestrictionState(
            isRestricted = isDriving,
            noVideo = isDriving,
            noKeyboard = isDriving,
            maxListLength = if (isHighSpeed) 5 else if (isDriving) 10 else Int.MAX_VALUE
        )

        if (_uxRestrictionState.value != newState) {
            _uxRestrictionState.value = newState
            CarLogger.i("CarUxRestrictions", "Safety restriction state updated: Driving=$isDriving, NoVideo=${newState.noVideo}")
        }
    }
}
