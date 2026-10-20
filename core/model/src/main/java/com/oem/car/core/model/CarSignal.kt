package com.oem.car.core.model

enum class SignalStatus {
    VALID,
    INVALID,
    UNAVAILABLE,
    ERROR
}

data class CarSignal<T>(
    val value: T,
    val status: SignalStatus = SignalStatus.VALID,
    val timestamp: Long = System.currentTimeMillis(),
    val areaId: Int = 0
) {
    val isValid: Boolean get() = status == SignalStatus.VALID
}
