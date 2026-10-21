package com.oem.car.core.model

/**
 * 车载原生属性 ID 定义，取值与 android.car.VehiclePropertyIds 保持一致。
 *
 * 这些常量只用于和 AAOS / VHAL 对接，Mock 链路仍然使用项目自己的 AIDL。
 * 属性 ID 的 bit 位编码了分组、区域类型和数据类型，不能按业务随意编号。
 */
object VehiclePropertyIds {
    /** 车速，float，GLOBAL 区域，单位 m/s（不是 km/h） */
    const val PERF_VEHICLE_SPEED = 0x11600207

    /**
     * 档位选择，int，GLOBAL 区域。
     *
     * AAOS 的档位码与项目内部 VehicleGear 不同：
     * AAOS: 1=N, 2=R, 4=P, 8=D；项目内部: 1=P, 2=R, 4=N, 8=D。
     * 桥接时必须经过 AaosSignalMapper 转换。
     */
    const val GEAR_SELECTION = 0x11400400

    /** 点火/电源状态，int，GLOBAL 区域 */
    const val IGNITION_STATE = 0x11400409

    /** 动力电池 SOC，float，GLOBAL 区域，0..100 */
    const val EV_BATTERY_LEVEL = 0x11600309

    /** 空调温度设定，float，SEAT 区域 */
    const val HVAC_TEMPERATURE_SET = 0x15600503

    /** 空调风速设定，int，SEAT 区域 */
    const val HVAC_FAN_SPEED = 0x15400500

    /** 车门锁状态，boolean，DOOR 区域 */
    const val DOOR_LOCK = 0x16200B02

    /** 胎压，float，WHEEL 区域 */
    const val TIRE_PRESSURE = 0x17600309
}
