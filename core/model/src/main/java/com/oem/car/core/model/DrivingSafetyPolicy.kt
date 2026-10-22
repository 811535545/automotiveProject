package com.oem.car.core.model

/**
 * 行车防分心规则的唯一实现（车规级迟滞滞后算法 & 防盲打策略）。
 *
 * 引入 Hysteresis (迟滞区间) 机制：
 * - 触发拦截：车速 > 0.5 km/h 或 档位 == DRIVE (D 档)
 * - 解除拦截：车速必须降低至 < 0.2 km/h 且 档位 == PARK (P 档)，防止临界车速频繁抖动/闪烁 (Anti-Flicker)。
 */
object DrivingSafetyPolicy {

    const val DRIVING_SPEED_TRIGGER_KMH = 0.5f
    const val DRIVING_SPEED_RELEASE_KMH = 0.2f
    const val HIGH_SPEED_THRESHOLD_KMH = 15.0f

    const val RESTRICTED_MAX_LIST_LENGTH = 10
    const val HIGH_SPEED_MAX_LIST_LENGTH = 5

    fun isDriving(gear: VehicleGear, speedKmh: Float, currentlyRestricted: Boolean = false): Boolean =
        isDriving(gear.code, speedKmh, currentlyRestricted)

    fun isDriving(gearCode: Int, speedKmh: Float, currentlyRestricted: Boolean = false): Boolean {
        // D 档无条件触发防分心
        if (gearCode == VehicleGear.DRIVE.code) return true

        // 迟滞区间计算：若当前已处于拦截状态，降速到 0.2 km/h 以下且非 D 档才解除
        return if (currentlyRestricted) {
            speedKmh >= DRIVING_SPEED_RELEASE_KMH
        } else {
            speedKmh > DRIVING_SPEED_TRIGGER_KMH
        }
    }

    fun maxListLength(gear: VehicleGear, speedKmh: Float): Int =
        maxListLength(gear.code, speedKmh)

    fun maxListLength(gearCode: Int, speedKmh: Float): Int = when {
        !isDriving(gearCode, speedKmh) -> Int.MAX_VALUE
        speedKmh > HIGH_SPEED_THRESHOLD_KMH -> HIGH_SPEED_MAX_LIST_LENGTH
        else -> RESTRICTED_MAX_LIST_LENGTH
    }
}
