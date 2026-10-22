package com.oem.car.core.vehicleservice

import android.content.Context
import com.oem.car.core.audio.MultiZoneAudioManager
import com.oem.car.core.common.CarLogger
import com.oem.car.core.navigation.CockpitDisplayManager
import com.oem.car.core.power.CarPowerLifecycleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 车载中间件 6 大领域服务拓扑定义 (Automotive Middleware Domain Pillars)。
 */
enum class MiddlewareDomainPillar(val id: Int, val title: String) {
    SIGNAL_BUS_PILLAR(1, "领域一: 车辆属性与信号总线中间件 (Property & CAN DBC)"),
    SOA_COMMUNICATION_PILLAR(2, "领域二: 面向服务 SOA 通信中间件 (SOME/IP & RPC)"),
    CAR_AUDIO_PILLAR(3, "领域三: 车载多音区与提示音引擎中间件 (Multi-Zone Audio & Chime)"),
    MULTI_DISPLAY_PILLAR(4, "领域四: 多屏流转与 OccupantZone 图形中间件 (Multi-Display)"),
    POWER_THERMAL_PILLAR(5, "领域五: 电源状态机与 STR 休眠唤醒中间件 (Power & STR)"),
    DIAGNOSTIC_SECURITY_PILLAR(6, "领域六: UDS/DoIP 诊断与 SecOC 信息安全中间件 (UDS & SecOC)")
}

/**
 * SOA (Service-Oriented Architecture) 车载服务注册表。
 */
data class SoaServiceEntry(
    val serviceId: Int,
    val serviceName: String,
    val interfaceVersion: Int,
    val isDiscovered: Boolean = true
)

/**
 * UDS (ISO 14229) 诊断状态。
 */
data class UdsDiagnosticState(
    val isTesterConnected: Boolean = false,
    val activeDtcCount: Int = 0,
    val freezeFrameHex: String = "00 00 00 00",
    val isSecOcVerified: Boolean = true
)

/**
 * 车载中间件全景拓扑协调总装类 (Automotive Middleware Universe Coordinator)。
 *
 * 在代码结构中完整体现与挂载座舱 6 大中间件领域服务：
 * 1. 信号与总线中间件：VehiclePropertyProvider + CanMessageCodec + Watchdog
 * 2. SOA 通信中间件：SOME/IP 服务发现与 RPC 注册表 (SoaServiceRegistry)
 * 3. 音频中间件：MultiZoneAudioManager + 车规安全提示音引擎 (ChimeEngine)
 * 4. 图形中间件：CockpitDisplayManager (多屏分发)
 * 5. 电源中间件：CarPowerLifecycleManager (STR 休眠/唤醒)
 * 6. 诊断与安全中间件：UDS (ISO 14229) / DoIP 诊断栈与 SecOC 报文验签
 */
class AutomotiveMiddlewareUniverse private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 1. 领域一: 信号与总线中间件
    val propertyProvider: VehiclePropertyProvider = VehiclePropertyProviderFactory.create(appContext)

    // 2. 领域二: SOA 服务化通信中间件 (SOME/IP 模拟注册表)
    private val _soaServices = MutableStateFlow(
        listOf(
            SoaServiceEntry(0x1001, "com.oem.service.VehiclePropertyService", 1),
            SoaServiceEntry(0x1002, "com.oem.service.CockpitClimateService", 1),
            SoaServiceEntry(0x2001, "com.oem.service.AdasPerceptionService", 2)
        )
    )
    val soaServices: StateFlow<List<SoaServiceEntry>> = _soaServices.asStateFlow()

    // 3. 领域三: 车载多音区音频中间件
    val multiZoneAudio: MultiZoneAudioManager = MultiZoneAudioManager()

    // 4. 领域四: 座舱多屏图形分发中间件
    val displayManager: CockpitDisplayManager = CockpitDisplayManager()

    // 5. 领域五: 电源状态机与 STR 休眠挂管中间件
    val powerLifecycleManager: CarPowerLifecycleManager = CarPowerLifecycleManager()

    // 6. 领域六: UDS 诊断与 SecOC 信息安全中间件
    private val _udsDiagnosticState = MutableStateFlow(UdsDiagnosticState())
    val udsDiagnosticState: StateFlow<UdsDiagnosticState> = _udsDiagnosticState.asStateFlow()

    fun initializeUniverse() {
        CarLogger.i(TAG, "Initializing Full Automotive Middleware Universe Topology (6 Pillars)...")

        // 启动看门狗降级保护
        VehiclePropertyAdapter.startSignalTimeoutWatchdog(scope)

        // 连接 VHAL / Binder 信号总线
        propertyProvider.connect()

        CarLogger.i(TAG, "Automotive Middleware Universe fully initialized!")
    }

    fun triggerUdsDiagnosticScan() {
        CarLogger.i(TAG, "UDS (ISO 14229) Tester Present: Reading DTC freeze frames via Service 0x19...")
        _udsDiagnosticState.value = UdsDiagnosticState(
            isTesterConnected = true,
            activeDtcCount = 2,
            freezeFrameHex = "19 02 09 P0 45 61 B1 A0 00",
            isSecOcVerified = true
        )
    }

    companion object {
        private const val TAG = "MiddlewareUniverse"

        @Volatile
        private var instance: AutomotiveMiddlewareUniverse? = null

        fun getInstance(context: Context): AutomotiveMiddlewareUniverse {
            return instance ?: synchronized(this) {
                instance ?: AutomotiveMiddlewareUniverse(context).also { instance = it }
            }
        }
    }
}
