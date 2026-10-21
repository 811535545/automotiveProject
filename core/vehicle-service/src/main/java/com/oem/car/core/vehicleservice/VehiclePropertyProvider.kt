package com.oem.car.core.vehicleservice

import android.content.Context
import android.content.pm.PackageManager
import com.oem.car.core.common.CarLogger
import com.oem.car.core.model.PowerState
import com.oem.car.core.model.VehicleGear
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 车辆属性服务提供者抽象接口 (VHAL 桥接抽象)。
 *
 * 提供两条互斥通道：
 * 1. MockBinderPropertyProvider：在普通手机/模拟器上，通过 Binder 连接本地模拟进程 (:vehicle)；
 * 2. AaosCarPropertyProvider：在原生 Android Automotive OS (AAOS) 镜像上，通过反射接入 CarPropertyManager。
 */
interface VehiclePropertyProvider {
    val isConnected: StateFlow<Boolean>

    fun connect()
    fun disconnect()

    fun injectSpeed(speed: Float)
    fun injectGear(gear: VehicleGear)
    fun injectPower(powerState: PowerState)
    fun triggerPtt()

    fun getProviderName(): String
}

/**
 * 本地 Binder 多进程 Mock 提供者 (默认实现)。
 */
class MockBinderPropertyProvider(
    context: Context,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : VehiclePropertyProvider {
    private val manager = VehicleServiceConnectionManager(context, scope)

    override val isConnected: StateFlow<Boolean> = manager.isConnected

    override fun connect() = manager.connect()

    override fun disconnect() = manager.disconnect()

    override fun injectSpeed(speed: Float) = manager.injectSpeed(speed)

    override fun injectGear(gear: VehicleGear) = manager.injectGear(gear)

    override fun injectPower(powerState: PowerState) = manager.injectPower(powerState)

    override fun triggerPtt() = manager.triggerPtt()

    override fun getProviderName(): String = "MockBinderProvider (:vehicle daemon)"
}

/**
 * AAOS / Mock 双通道提供者。
 *
 * 选择顺序：
 * 1. 设备存在 android.car.Car，并且 CarPropertyManager 能读到/订阅到车速和档位
 *    -> Native AAOS 通道（只读，属性来自 CarService/VHAL）；
 * 2. 否则 -> MockBinderPropertyProvider（连接 :vehicle 进程，可注入模拟值）。
 *
 * 普通手机/模拟器没有 android.car 类，会在第一步直接落到 Mock，不会加载
 * AaosCarPropertyBridge 里的任何反射代码。
 *
 * 注意：原生通道只桥接 PERF_VEHICLE_SPEED 和 GEAR_SELECTION，并且需要
 * CAR_SPEED / CAR_POWERTRAIN 权限（多数系统镜像里是 signature|privileged）。
 * 权限不足或属性不可用时，不是“部分原生 + 部分 Mock”，而是整体回退 Mock，
 * 避免两个数据源混在一起。
 */
class AaosCarPropertyProvider(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : VehiclePropertyProvider {
    private val appContext = context.applicationContext
    private val fallbackProvider = MockBinderPropertyProvider(appContext)

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val isAaosAvailable: Boolean = runCatching {
        Class.forName("android.car.Car")
        true
    }.getOrElse {
        CarLogger.i(TAG, "android.car.Car not found; using MockBinderPropertyProvider")
        false
    }

    private var nativeBridge: AaosCarPropertyBridge? = null
    private var usingFallback = false
    private var connectJob: Job? = null
    private var fallbackJob: Job? = null

    init {
        if (isAaosAvailable) {
            CarLogger.i(
                TAG,
                "android.car.Car found; automotiveFeature=" +
                    appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
            )
        }
    }

    override fun connect() {

        if (nativeBridge != null || usingFallback || connectJob?.isActive == true) return

        if (!isAaosAvailable) {
            startFallback()
            return
        }

        // 原生连接会同步跨 Binder 读取属性，放到后台线程，避免卡住 MainActivity.onCreate。
        connectJob = scope.launch {
            CarLogger.i(TAG, "trying native AAOS CarPropertyManager (read-only speed/gear)")
            val bridge = AaosCarPropertyBridge(
                context = appContext,
                onSpeed = { signal ->
                    VehiclePropertyAdapter.updateSpeed(
                        speedKmh = signal.value,
                        status = signal.status,
                        timestampMs = signal.timestamp,
                        areaId = signal.areaId
                    )
                },
                onGear = { signal ->
                    VehiclePropertyAdapter.updateGear(
                        gearCode = signal.value.code,
                        status = signal.status,
                        timestampMs = signal.timestamp,
                        areaId = signal.areaId
                    )
                }
            )
            val connected = runCatching { bridge.connect() }.getOrElse {
                CarLogger.e(TAG, "native bridge threw during connect", it)
                false
            }

            // disconnect() 已经取消了这个任务时，不要再把桥接对象挂到字段上。
            if (!isActive) {
                bridge.disconnect()
                return@launch
            }

            if (connected) {
                nativeBridge = bridge
                _isConnected.value = true
                CarLogger.i(TAG, "using native AAOS CarPropertyManager (speed + gear)")
            } else {
                CarLogger.w(TAG, "native AAOS bridge unavailable; falling back to Mock Binder")
                bridge.disconnect()
                startFallback()
            }
        }
    }

    private fun startFallback() {
        if (usingFallback) return
        usingFallback = true
        fallbackJob = scope.launch {
            fallbackProvider.isConnected.collect { _isConnected.value = it }
        }
        fallbackProvider.connect()
    }

    override fun disconnect() {
        connectJob?.cancel()
        connectJob = null
        fallbackJob?.cancel()
        fallbackJob = null

        nativeBridge?.disconnect()
        nativeBridge = null

        if (usingFallback) {
            fallbackProvider.disconnect()
            usingFallback = false
        }
        _isConnected.value = false
    }

    override fun injectSpeed(speed: Float) {
        if (nativeBridge != null) {
            CarLogger.w(TAG, "injectSpeed($speed) ignored: AAOS speed property is read-only")
            return
        }
        fallbackProvider.injectSpeed(speed)
    }

    override fun injectGear(gear: VehicleGear) {
        if (nativeBridge != null) {
            CarLogger.w(TAG, "injectGear(${gear.code}) ignored: AAOS gear property is read-only")
            return
        }
        fallbackProvider.injectGear(gear)
    }

    override fun injectPower(powerState: PowerState) {
        if (nativeBridge != null) {
            CarLogger.w(TAG, "injectPower(${powerState.code}) ignored: native mode does not bridge power")
            return
        }
        fallbackProvider.injectPower(powerState)
    }

    override fun triggerPtt() {
        // PTT 是主进程内的语音状态；原生模式下没有 Mock Binder 可调用，直接跳过。
        if (nativeBridge == null) {
            fallbackProvider.triggerPtt()
        }
    }

    override fun getProviderName(): String = when {
        nativeBridge != null -> "AAOS CarPropertyManager (speed/gear, read-only)"
        usingFallback -> "MockBinderProvider (:vehicle daemon)"
        else -> "Not connected"
    }

    companion object {
        private const val TAG = "AaosCarPropertyProvider"
    }
}

/**
 * 提供者工厂类。
 */
object VehiclePropertyProviderFactory {
    fun create(context: Context): VehiclePropertyProvider {
        return AaosCarPropertyProvider(context)
    }
}
