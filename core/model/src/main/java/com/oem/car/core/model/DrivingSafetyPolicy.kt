package com.oem.car.core.model

/**
 * 行车防分心规则的唯一实现（演示阈值，非法规符合性实现）。
 *
 * 服务端 CarCoreDaemon 只关心 isDriving；客户端 CarUxRestrictionsEngine 还用它
 * 计算 noVideo/noKeyboard/maxListLength。两边共用同一函数，避免规则分叉。
 * 边界为严格大于：0.5 km/h 不触发；15 km/h 仍返回 10 条列表上限。
 */
object DrivingSafetyPolicy {

    const val DRIVING_SPEED_THRESHOLD_KMH = 0.5f
    const val HIGH_SPEED_THRESHOLD_KMH = 15.0f
    const val RESTRICTED_MAX_LIST_LENGTH = 10
    const val HIGH_SPEED_MAX_LIST_LENGTH = 5

    fun isDriving(gear: VehicleGear, speedKmh: Float): Boolean =
        isDriving(gear.code, speedKmh)

    fun isDriving(gearCode: Int, speedKmh: Float): Boolean =
        gearCode == VehicleGear.DRIVE.code || speedKmh > DRIVING_SPEED_THRESHOLD_KMH

    fun maxListLength(gear: VehicleGear, speedKmh: Float): Int =
        maxListLength(gear.code, speedKmh)

    fun maxListLength(gearCode: Int, speedKmh: Float): Int = when {
        !isDriving(gearCode, speedKmh) -> Int.MAX_VALUE
        speedKmh > HIGH_SPEED_THRESHOLD_KMH -> HIGH_SPEED_MAX_LIST_LENGTH
        else -> RESTRICTED_MAX_LIST_LENGTH
    }
}
