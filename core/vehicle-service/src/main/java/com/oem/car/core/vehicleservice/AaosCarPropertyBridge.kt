package com.oem.car.core.vehicleservice

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.oem.car.core.common.CarLogger
import com.oem.car.core.model.AaosSignalMapper
import com.oem.car.core.model.CarSignal
import com.oem.car.core.model.VehicleGear
import com.oem.car.core.model.VehiclePropertyIds
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * AAOS 原生 CarPropertyManager 的最小反射桥接。
 *
 * 为什么用反射而不是直接 import：
 * 项目默认编译/运行在普通 Android 设备上，那里没有 android.car 类；
 * 如果类里直接出现 android.car.Car 类型，类加载阶段就可能抛 NoClassDefFoundError。
 * 把所有 android.car 引用都收敛到这个类，并且只在 Class.forName 探测成功后才实例化，
 * 普通设备会走 MockBinderPropertyProvider，不会触碰这里的反射代码。
 *
 * 当前只桥接两个只读属性：
 * - PERF_VEHICLE_SPEED：AAOS 单位是 m/s，转成项目 UI 使用的 km/h；
 * - GEAR_SELECTION：AAOS 档位码与项目内部 VehicleGear 编码不同，需要映射。
 *
 * 真正的调用链：
 * App -> CarPropertyManager (car-lib) -> ICarProperty (Binder)
 *     -> CarService CarPropertyService -> PropertyHalService -> VehicleHal
 *     -> IVehicle (Binder) -> VHAL -> CAN/以太网。
 * 本类只负责 App 侧这一小段：连接、注册回调、把 CarPropertyValue 解包成 CarSignal。
 */
internal class AaosCarPropertyBridge(
    private val context: Context,
    private val onSpeed: (CarSignal<Float>) -> Unit,
    private val onGear: (CarSignal<VehicleGear>) -> Unit
) {
    private var carClass: Class<*>? = null
    private var car: Any? = null
    private var managerClass: Class<*>? = null
    private var propertyManager: Any? = null
    private var callbackInterface: Class<*>? = null
    private var callback: Any? = null

    /**
     * 连接 CarService，读取一次初始值，再订阅车速/档位。
     *
     * 两步都成功才返回 true；只要有一个属性订阅失败（最常见原因是
     * CAR_SPEED/CAR_POWERTRAIN 权限没授予），调用方就会整体回退到 Mock，
     * 避免出现“车速来自 AAOS、档位来自 Mock”的混合数据源。
     */
    fun connect(): Boolean {
        return try {
            val localCarClass = Class.forName(CAR_CLASS)
            // connect() 在后台线程执行；显式传入主 Looper 的 Handler，
            // 避免 Car 内部回调依赖当前线程的 Looper（后台线程默认没有 Looper）。
            val mainHandler = Handler(Looper.getMainLooper())
            val localCar = localCarClass
                .getMethod("createCar", Context::class.java, Handler::class.java)
                .invoke(null, context, mainHandler)
                ?: return false
            val localManager = localCarClass
                .getMethod("getCarManager", String::class.java)
                .invoke(localCar, CAR_PROPERTY_SERVICE)
                ?: return false

            val loader = localManager.javaClass.classLoader
            val localCallbackInterface = if (loader != null) {
                Class.forName(CALLBACK_CLASS, false, loader)
            } else {
                Class.forName(CALLBACK_CLASS)
            }
            val localCallback = Proxy.newProxyInstance(
                localCallbackInterface.classLoader,
                arrayOf(localCallbackInterface),
                PropertyEventInvocationHandler()
            )

            carClass = localCarClass
            car = localCar
            managerClass = localManager.javaClass
            propertyManager = localManager
            callbackInterface = localCallbackInterface
            callback = localCallback

            // 先读一次快照，避免只等 on-change 属性时 UI 一直停在默认值。
            readInitialValue(VehiclePropertyIds.PERF_VEHICLE_SPEED, java.lang.Float::class.java)
            readInitialValue(VehiclePropertyIds.GEAR_SELECTION, java.lang.Integer::class.java)

            // 车速是连续属性，UI 用 5Hz 足够；档位是 on-change，rate=0。
            val speedSubscribed = subscribe(VehiclePropertyIds.PERF_VEHICLE_SPEED, SPEED_RATE_HZ)
            val gearSubscribed = subscribe(VehiclePropertyIds.GEAR_SELECTION, GEAR_RATE_HZ)

            if (!speedSubscribed || !gearSubscribed) {
                CarLogger.w(
                    TAG,
                    "native subscribe incomplete: speed=$speedSubscribed gear=$gearSubscribed; " +
                        "check CAR_SPEED / CAR_POWERTRAIN permissions"
                )
                disconnect()
                return false
            }

            CarLogger.i(TAG, "native CarPropertyManager bridge connected (speed + gear)")
            true
        } catch (e: InvocationTargetException) {
            // 反射调用的真实异常在 cause 里，比如 SecurityException。
            val cause = e.targetException
            CarLogger.e(
                TAG,
                "native connect failed: ${cause.javaClass.simpleName}: ${cause.message}",
                cause
            )
            disconnect()
            false
        } catch (t: Throwable) {
            CarLogger.e(TAG, "native connect failed: ${t.javaClass.simpleName}: ${t.message}", t)
            disconnect()
            false
        }
    }

    fun disconnect() {
        val manager = propertyManager
        val managerCls = managerClass
        val cb = callback
        val cbInterface = callbackInterface
        if (manager != null && managerCls != null && cb != null && cbInterface != null) {
            try {
                managerCls.getMethod("unregisterCallback", cbInterface).invoke(manager, cb)
            } catch (t: Throwable) {
                CarLogger.w(TAG, "unregisterCallback failed: ${t.javaClass.simpleName}")
            }
        }
        try {
            carClass?.getMethod("disconnect")?.invoke(car)
        } catch (t: Throwable) {
            CarLogger.w(TAG, "Car.disconnect failed: ${t.javaClass.simpleName}")
        }

        carClass = null
        car = null
        managerClass = null
        propertyManager = null
        callbackInterface = null
        callback = null
    }

    /**
     * 读取一次属性。getProperty(Class, propId, areaId) 返回 CarPropertyValue，
     * 里面同时带有 value/status/timestamp/areaId，比 getFloatProperty 的裸值更适合
     * 做信号有效性判断。
     */
    private fun readInitialValue(propId: Int, valueClass: Class<*>) {
        val manager = propertyManager ?: return
        val managerCls = managerClass ?: return
        try {
            val method = managerCls.getMethod(
                "getProperty",
                Class::class.java,
                INT_PRIMITIVE,
                INT_PRIMITIVE
            )
            val result = method.invoke(manager, valueClass, propId, GLOBAL_AREA_ID)
            if (result == null) {
                CarLogger.w(TAG, "initial getProperty returned null prop=$propId")
            } else {
                handlePropertyValue(result)
            }
        } catch (e: InvocationTargetException) {
            val cause = e.targetException
            CarLogger.w(
                TAG,
                "initial getProperty failed prop=$propId: ${cause.javaClass.simpleName}: ${cause.message}"
            )
        } catch (t: Throwable) {
            CarLogger.w(TAG, "initial getProperty failed prop=$propId: ${t.javaClass.simpleName}")
        }
    }

    private fun subscribe(propId: Int, rateHz: Float): Boolean {
        val manager = propertyManager ?: return false
        val managerCls = managerClass ?: return false
        val cb = callback ?: return false
        val cbInterface = callbackInterface ?: return false
        return try {
            val method = managerCls.getMethod(
                "registerCallback",
                cbInterface,
                INT_PRIMITIVE,
                FLOAT_PRIMITIVE
            )
            val subscribed = method.invoke(manager, cb, propId, rateHz) as? Boolean ?: false
            if (!subscribed) {
                CarLogger.w(TAG, "registerCallback returned false prop=$propId")
            }
            subscribed
        } catch (e: InvocationTargetException) {
            val cause = e.targetException
            CarLogger.w(
                TAG,
                "registerCallback failed prop=$propId: ${cause.javaClass.simpleName}: ${cause.message}"
            )
            false
        } catch (t: Throwable) {
            CarLogger.w(TAG, "registerCallback failed prop=$propId: ${t.javaClass.simpleName}")
            false
        }
    }

    /**
     * 把反射拿到的 CarPropertyValue 解包，再转换成项目内部的 CarSignal。
     *
     * CarPropertyValue 是 AAOS 的“读数信封”：
     * - getValue()：真正的值
     * - getStatus()：AVAILABLE/UNAVAILABLE/ERROR
     * - getTimestamp()：elapsedRealtimeNanos，不是墙钟毫秒
     * - getAreaId()：分区 ID
     */
    private fun handlePropertyValue(value: Any) {
        try {
            val cls = value.javaClass
            val propId = cls.getMethod("getPropertyId").invoke(value) as? Int ?: return
            val areaId = cls.getMethod("getAreaId").invoke(value) as? Int ?: GLOBAL_AREA_ID
            val status = cls.getMethod("getStatus").invoke(value) as? Int
                ?: AAOS_STATUS_ERROR
            val timestampNanos = cls.getMethod("getTimestamp").invoke(value) as? Long ?: 0L
            val rawValue = cls.getMethod("getValue").invoke(value)

            val signalStatus = AaosSignalMapper.statusFromAaos(status)
            val timestampMs = if (timestampNanos > 0L) {
                timestampNanos / NANOS_PER_MILLI
            } else {
                System.currentTimeMillis()
            }

            when (propId) {
                VehiclePropertyIds.PERF_VEHICLE_SPEED -> {
                    val speedMps = (rawValue as? Number)?.toFloat() ?: return
                    onSpeed(
                        CarSignal(
                            value = AaosSignalMapper.speedMpsToKmh(speedMps),
                            status = signalStatus,
                            timestamp = timestampMs,
                            areaId = areaId
                        )
                    )
                }

                VehiclePropertyIds.GEAR_SELECTION -> {
                    val gearCode = (rawValue as? Number)?.toInt() ?: return
                    onGear(
                        CarSignal(
                            value = AaosSignalMapper.gearFromAaosCode(gearCode),
                            status = signalStatus,
                            timestamp = timestampMs,
                            areaId = areaId
                        )
                    )
                }
            }
        } catch (t: Throwable) {
            CarLogger.w(TAG, "handlePropertyValue failed: ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    /**
     * CarPropertyManager 的回调接口是普通 Java interface，不是 AIDL Stub；
     * 用动态代理实现它，避免在普通设备上加载 android.car 类型。
     */
    private inner class PropertyEventInvocationHandler : InvocationHandler {
        override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>?): Any? {
            return when (method.name) {
                "onChangeEvent" -> {
                    args?.firstOrNull()?.let { handlePropertyValue(it) }
                    null
                }

                "onErrorEvent" -> {
                    val propId = args?.getOrNull(0) as? Int
                    val areaId = args?.getOrNull(1) as? Int
                    val errorCode = args?.getOrNull(2) as? Int
                    CarLogger.w(TAG, "onErrorEvent prop=$propId area=$areaId code=$errorCode")
                    null
                }

                "toString" -> "AaosCarPropertyBridgeCallback"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.getOrNull(0)
                else -> null
            }
        }
    }

    companion object {
        private const val TAG = "AaosCarPropertyBridge"
        private const val CAR_CLASS = "android.car.Car"
        private const val CALLBACK_CLASS =
            "android.car.hardware.property.CarPropertyManager\$CarPropertyEventCallback"

        /** Car.PROPERTY_SERVICE 的字符串值 */
        private const val CAR_PROPERTY_SERVICE = "property"

        /** 全局属性的 areaId 固定为 0 */
        private const val GLOBAL_AREA_ID = 0

        /** CarPropertyManager.SENSOR_RATE_UI */
        private const val SPEED_RATE_HZ = 5.0f

        /** CarPropertyManager.SENSOR_RATE_ONCHANGE */
        private const val GEAR_RATE_HZ = 0.0f

        /** CarPropertyValue.STATUS_ERROR */
        private const val AAOS_STATUS_ERROR = 2
        private const val NANOS_PER_MILLI = 1_000_000L

        private val INT_PRIMITIVE: Class<*> = java.lang.Integer.TYPE
        private val FLOAT_PRIMITIVE: Class<*> = java.lang.Float.TYPE
    }
}
