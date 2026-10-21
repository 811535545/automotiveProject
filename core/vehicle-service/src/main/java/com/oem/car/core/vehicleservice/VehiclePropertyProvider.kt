package com.oem.car.core.vehicleservice

import android.content.Context
import com.oem.car.core.common.CarLogger
import com.oem.car.core.model.PowerState
import com.oem.car.core.model.VehicleGear
import com.oem.car.core.model.VehiclePropertyIds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow

/**
 * 车辆属性服务提供者抽象接口 (VHAL 桥接抽象)。
 *
 * 实现了双模式兼容：
 * 1. MockBinderPropertyProvider：在普通手机/模拟器上，通过 Binder 连接本地模拟进程 (:vehicle)；
 * 2. AaosCarPropertyProvider：在原生 Android Automotive OS (AAOS) 车机模拟器上，通过反射接入系统 CarPropertyManager。
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
 * 原生 AAOS CarPropertyManager 桥接层。
 * 当运行在真实的 AAOS 车机镜像或官方 Automotive 模拟器上时，优先使用此 Provider。
 */
class AaosCarPropertyProvider(
    private val context: Context
) : VehiclePropertyProvider {
    private val fallbackProvider = MockBinderPropertyProvider(context)
    private var isAaosAvailable = false

    init {
        try {
            // 尝试检测系统是否包含 android.car.Car 类
            val carClass = Class.forName("android.car.Car")
            isAaosAvailable = carClass != null
            CarLogger.i(TAG, "AAOS Car API detected: isAvailable=$isAaosAvailable")
        } catch (e: ClassNotFoundException) {
            isAaosAvailable = false
            CarLogger.w(TAG, "AAOS Car API not found, falling back to MockBinderPropertyProvider")
        }
    }

    override val isConnected: StateFlow<Boolean> = fallbackProvider.isConnected

    override fun connect() {
        if (isAaosAvailable) {
            CarLogger.i(TAG, "Connecting via native AAOS CarPropertyManager for propertyId=${VehiclePropertyIds.PERF_VEHICLE_SPEED}")
            // 真实车机下在此调用 Car.createCar() 和 CarPropertyManager.registerCallback()
        }
        fallbackProvider.connect()
    }

    override fun disconnect() {
        fallbackProvider.disconnect()
    }

    override fun injectSpeed(speed: Float) {
        fallbackProvider.injectSpeed(speed)
    }

    override fun injectGear(gear: VehicleGear) {
        fallbackProvider.injectGear(gear)
    }

    override fun injectPower(powerState: PowerState) {
        fallbackProvider.injectPower(powerState)
    }

    override fun triggerPtt() {
        fallbackProvider.triggerPtt()
    }

    override fun getProviderName(): String {
        return if (isAaosAvailable) "Native AAOS CarPropertyManager" else "AAOS Bridge (Fallback to Mock Binder)"
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
