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

// ================= CCC 3.0 数字钥匙数据模型 =================
enum class KeyTech(val display: String) {
    UWB("UWB 超宽带 (厘米级测距)"),
    BLE("BLE 低功耗蓝牙"),
    NFC("NFC 贴刷触控")
}

enum class ProximityZone(val display: String) {
    FAR("远距离 (>10m)"),
    APPROACHING("靠近中 (5m - 10m)"),
    WELCOME("迎宾区 (2m - 5m)"),
    UNLOCK("解封区 (<1.5m)"),
    CABIN_INSIDE("座舱内部 (允许启动)")
}

data class DigitalKeyState(
    val isDevicePaired: Boolean = true,
    val activeTech: KeyTech = KeyTech.UWB,
    val proximityZone: ProximityZone = ProximityZone.CABIN_INSIDE,
    val distanceMeters: Float = 0.5f,
    val isDoorUnlocked: Boolean = true,
    val isEngineStartAllowed: Boolean = true,
    val isSeAuthenticated: Boolean = true // SE (Secure Element) 芯片挑战鉴权成功
)

// ================= 车联网与 T-BOX / OTA 数据模型 =================
enum class OtaState(val display: String) {
    IDLE("系统已是最新版"),
    CHECKING("正在检查车云云端更新..."),
    DOWNLOADING("正在下载 SOTA 差分包..."),
    VERIFYING("HSM 验签与 A/B 双分区准备..."),
    APPLYING("重启后无感无缝切换..."),
    SUCCESS("升级成功 (V2.5.0 车规系统)")
}

data class TelematicsState(
    val isConnectedToCloud: Boolean = true,
    val signalStrengthDbm: Int = -75,
    val isSentryModeActive: Boolean = false,
    val otaState: OtaState = OtaState.IDLE,
    val otaProgressPercent: Int = 0,
    val lastRemoteCommand: String = "无"
)

// ================= ADAS 智驾感知与 SR 3D 渲染数据模型 =================
data class PerceptionTarget(
    val id: Int,
    val type: String, // "Sedan", "Pedestrian", "Truck", "Cone"
    val distanceX: Float, // 米
    val distanceY: Float, // 米
    val speed: Float     // km/h
)

data class AdasPerceptionState(
    val isNoaActive: Boolean = true, // NOA (Navigate on Autopilot) 领航辅助
    val cruiseSpeed: Float = 80.0f,
    val laneKeepActive: Boolean = true,
    val targets: List<PerceptionTarget> = listOf(
        PerceptionTarget(1, "前车(轿车)", 25.0f, 0.0f, 78.0f),
        PerceptionTarget(2, "左侧卡车", 12.0f, -3.5f, 70.0f),
        PerceptionTarget(3, "行人", 45.0f, 2.1f, 5.0f)
    )
)

// ================= DMS 人脸识别与多账户 Profile 数据模型 =================
data class UserProfile(
    val userId: String = "USR_001",
    val userName: String = "驾驶员 (张先生)",
    val seatMemoryPosition: Int = 2,
    val mirrorAngleDeg: Int = 12,
    val preferredTemp: Float = 21.5f,
    val ambientColorHex: String = "#00E5FF",
    val isFaceIdVerified: Boolean = true
)

// ================= 导航与路线规划数据模型 =================
enum class RouteStatus(val display: String) {
    IDLE("未开始导航"),
    NAVIGATING("导航中"),
    ARRIVED("已到达")
}

data class NavigationDestination(
    val name: String,
    val address: String,
    val distanceKm: Float,
    val etaMinutes: Int
)

data class NavigationState(
    val status: RouteStatus = RouteStatus.IDLE,
    val destination: NavigationDestination? = null,
    val nextInstruction: String = "未设置目的地",
    val distanceToTurnMeters: Int = 0,
    val remainingKm: Float = 0f,
    val etaMinutes: Int = 0,
    val speedLimitKph: Int = 80,
    val laneHint: String = "",
    val progress: Float = 0f
)

// ================= 电话与蓝牙通话数据模型 =================
enum class CallStatus(val display: String) {
    IDLE("空闲"),
    DIALING("呼出中"),
    RINGING("来电"),
    ACTIVE("通话中")
}

data class Contact(
    val name: String,
    val phone: String
)

data class PhoneState(
    val isBluetoothConnected: Boolean = true,
    val deviceName: String = "Pixel 8 Pro",
    val contacts: List<Contact> = listOf(
        Contact("张先生", "138 0000 1234"),
        Contact("李女士", "139 0000 5678"),
        Contact("道路救援", "400 800 8888")
    ),
    val callStatus: CallStatus = CallStatus.IDLE,
    val currentContact: Contact? = null,
    val callDurationSec: Int = 0,
    val isMuted: Boolean = false,
    val missedCalls: Int = 2,
    val audioRoute: String = "车机扬声器"
)

// ================= 能量管理与充电数据模型 =================
data class EnergyState(
    val socPercent: Int = 68,
    val rangeKm: Int = 412,
    val isCharging: Boolean = false,
    val chargingPowerKw: Float = 0f,
    val chargeLimitPercent: Int = 80,
    val isScheduledCharging: Boolean = true,
    val scheduledTime: String = "23:00",
    val isPreconditioning: Boolean = false,
    val batteryTempC: Float = 26f,
    val consumptionKwh100Km: Float = 15.8f,
    val chargerType: String = "直流快充 120 kW"
)

// ================= 车辆健康与诊断数据模型 =================
enum class HealthSeverity(val display: String) {
    NORMAL("正常"),
    WARNING("注意"),
    FAULT("故障")
}

data class TirePressure(
    val position: String,
    val bar: Float
)

data class SystemHealth(
    val name: String,
    val severity: HealthSeverity,
    val detail: String
)

data class DtcCode(
    val code: String,
    val description: String,
    val severity: HealthSeverity
)

data class VehicleHealthState(
    val tires: List<TirePressure> = listOf(
        TirePressure("左前", 2.4f),
        TirePressure("右前", 2.4f),
        TirePressure("左后", 2.3f),
        TirePressure("右后", 2.1f)
    ),
    val battery12vVoltage: Float = 12.6f,
    val odometerKm: Int = 18642,
    val serviceDueKm: Int = 20000,
    val isScanning: Boolean = false,
    val lastScanResult: String = "尚未扫描",
    val dtcs: List<DtcCode> = emptyList(),
    val systems: List<SystemHealth> = listOf(
        SystemHealth("制动系统 ABS", HealthSeverity.NORMAL, "自检通过"),
        SystemHealth("车身稳定 ESP", HealthSeverity.NORMAL, "自检通过"),
        SystemHealth("安全气囊 SRS", HealthSeverity.NORMAL, "自检通过"),
        SystemHealth("ADAS 感知", HealthSeverity.WARNING, "摄像头标定建议")
    )
)
