package com.oem.car.core.model

/**
 * AAOS 原生属性值到项目内部信号模型的纯函数映射。
 *
 * 放在 core:model 而不是桥接类里，是为了让这些换算可以在 JVM 单测中直接验证，
 * 不需要 AAOS 车机、CarService 或 Binder 环境。
 */
object AaosSignalMapper {

    /** m/s -> km/h。AAOS 的 PERF_VEHICLE_SPEED 单位是 m/s，项目 UI 用 km/h。 */
    private const val MPS_TO_KMH = 3.6f

    // android.car.VehicleGear 的标准值
    private const val AAOS_GEAR_NEUTRAL = 0x0001
    private const val AAOS_GEAR_REVERSE = 0x0002
    private const val AAOS_GEAR_PARK = 0x0004
    private const val AAOS_GEAR_DRIVE = 0x0008

    // android.car.hardware.CarPropertyValue 的标准状态值
    private const val AAOS_STATUS_AVAILABLE = 0
    private const val AAOS_STATUS_UNAVAILABLE = 1
    private const val AAOS_STATUS_ERROR = 2

    fun speedMpsToKmh(speedMps: Float): Float = speedMps * MPS_TO_KMH

    /**
     * AAOS 档位码转项目内部档位。
     *
     * 未知档位按 DRIVE 处理，而不是 PARK：这样防分心限制会保持开启，
     * 属于失效安全（fail-safe）策略，避免因为信号缺失反而放开视频/键盘限制。
     */
    fun gearFromAaosCode(gearCode: Int): VehicleGear = when (gearCode) {
        AAOS_GEAR_NEUTRAL -> VehicleGear.NEUTRAL
        AAOS_GEAR_REVERSE -> VehicleGear.REVERSE
        AAOS_GEAR_PARK -> VehicleGear.PARK
        AAOS_GEAR_DRIVE -> VehicleGear.DRIVE
        else -> VehicleGear.DRIVE
    }

    /**
     * CarPropertyValue.status 转项目 SignalStatus。
     *
     * AAOS 只定义 0/1/2 三种状态；未知值按 INVALID 处理，调用方不应把它当有效数据。
     */
    fun statusFromAaos(status: Int): SignalStatus = when (status) {
        AAOS_STATUS_AVAILABLE -> SignalStatus.VALID
        AAOS_STATUS_UNAVAILABLE -> SignalStatus.UNAVAILABLE
        AAOS_STATUS_ERROR -> SignalStatus.ERROR
        else -> SignalStatus.INVALID
    }
}
