package com.oem.car.core.model

/**
 * 车载原生属性 ID 定义 (符合 Android Automotive OS / AAOS 官方属性标准)。
 *
 * 对应 android.car.VehiclePropertyIds，用于映射底层 VHAL 属性与上层业务信号。
 */
object VehiclePropertyIds {
    /** 车速，单位 km/h 或 m/s，float 类型 (0x11600207) */
    const val PERF_VEHICLE_SPEED = 0x11600207

    /** 当前档位，Int 类型 (0x11400400) */
    const val GEAR_SELECTION = 0x11400400

    /** 电源状态/Ignition 状态，Int 类型 (0x11400401) */
    const val IGNITION_STATE = 0x11400401

    /** 电池电量/SOC，float 类型 (0x11600209) */
    const val EV_BATTERY_LEVEL = 0x11600209

    /** 空调温度设定，float 类型 (0x12400503) */
    const val HVAC_TEMPERATURE_SET = 0x12400503

    /** 空调风速设定，Int 类型 (0x12400500) */
    const val HVAC_FAN_SPEED = 0x12400500

    /** 驾驶防分心限制状态，Int / Boolean 类型 (0x11200407) */
    const val UX_RESTRICTION_STATE = 0x11200407

    /** 车门锁状态，Int / Boolean 类型 (0x11400502) */
    const val DOOR_LOCK = 0x11400502

    /** 胎压状态，float 类型 (0x11600305) */
    const val TIRE_PRESSURE = 0x11600305
}
