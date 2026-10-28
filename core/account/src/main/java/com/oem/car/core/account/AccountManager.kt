package com.oem.car.core.account

import com.oem.car.core.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ============================================================================
 * DMS 人脸识别与多账户 Profile 管理器 (Account & DMS Manager)
 * ============================================================================
 *
 * 【核心职责】
 * 管理驾驶员人脸识别 (Face ID) 验证状态、座椅记忆位置、后视镜角度、偏好温度以及个性化 Profile 应用。
 */
class AccountManager {
    private val _profile = MutableStateFlow(UserProfile(isFaceIdVerified = false))
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    /** 验证驾驶员人脸识别 (Face ID) */
    fun verifyFace() {
        _profile.value = _profile.value.copy(isFaceIdVerified = true)
    }

    /** 循环切换座椅记忆位置 (1~4) */
    fun cycleSeatPosition() {
        val current = _profile.value
        _profile.value = current.copy(seatMemoryPosition = current.seatMemoryPosition % 4 + 1)
    }

    /** 调整偏好温度 (限制在 16°C ~ 30°C 范围内) */
    fun adjustPreferredTemp(delta: Float) {
        val current = _profile.value
        _profile.value = current.copy(preferredTemp = (current.preferredTemp + delta).coerceIn(16f, 30f))
    }
}
