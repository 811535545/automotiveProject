package com.oem.car.core.model

enum class VehicleGear(val code: Int, val display: String) {
    PARK(1, "P (驻车)"),
    REVERSE(2, "R (倒车)"),
    NEUTRAL(4, "N (空档)"),
    DRIVE(8, "D (行车)");

    companion object {
        fun fromCode(code: Int): VehicleGear = entries.firstOrNull { it.code == code } ?: DRIVE
    }
}

enum class PowerState(val code: Int, val display: String) {
    RUNNING(0, "NORMAL 运行中"),
    STR_SLEEP(1, "STR 内存休眠挂起"),
    GARAGE_MODE(2, "Garage Mode 车库维护"),
    SHUTDOWN_PREPARE(3, "关机预备模式");

    companion object {
        fun fromCode(code: Int): PowerState = entries.firstOrNull { it.code == code } ?: RUNNING
    }
}

data class UxRestrictionState(
    val isRestricted: Boolean = false,
    val noVideo: Boolean = false,
    val noKeyboard: Boolean = false,
    val maxListLength: Int = Int.MAX_VALUE
)

data class AudioZoneState(
    val isPttActive: Boolean = false,
    val lastPttTimestamp: Long = 0L,
    val audioFocusDuck: Boolean = false
)

data class ClimateState(
    val driverTemp: Float = 22.0f,
    val passengerTemp: Float = 22.0f,
    val fanSpeed: Int = 3,
    val isAcOn: Boolean = true,
    val isAutoMode: Boolean = true
)

enum class DriveMode(val display: String) {
    ECO("经济模式"),
    COMFORT("舒适模式"),
    SPORT("运动模式"),
    CUSTOM("自定义模式")
}
