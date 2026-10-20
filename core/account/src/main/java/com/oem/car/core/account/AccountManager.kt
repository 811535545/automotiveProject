package com.oem.car.core.account

import com.oem.car.core.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccountManager {
    private val _profile = MutableStateFlow(UserProfile(isFaceIdVerified = false))
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    fun verifyFace() {
        _profile.value = _profile.value.copy(isFaceIdVerified = true)
    }

    fun cycleSeatPosition() {
        val current = _profile.value
        _profile.value = current.copy(seatMemoryPosition = current.seatMemoryPosition % 4 + 1)
    }

    fun adjustPreferredTemp(delta: Float) {
        val current = _profile.value
        _profile.value = current.copy(preferredTemp = (current.preferredTemp + delta).coerceIn(16f, 30f))
    }
}
