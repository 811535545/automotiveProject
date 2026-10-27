package com.oem.car.core.vehicleservice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Process
import android.os.RemoteException
import android.os.SystemClock
import com.oem.car.core.common.CarLogger
import com.oem.car.core.model.PowerState
import com.oem.car.core.model.VehicleGear
import com.oem.car.middleware.IVehicleCoreManager
import com.oem.car.middleware.IVehicleEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ============================================================================
 * 车辆服务 Binder 通信与断线自愈连接管理器 (Vehicle Service Connection Manager)
 * ============================================================================
 *
 * 【核心职责】
 * 1. 客户端 Binder 管道建立：通过 `bindService` 绑定运行在 `:vehicle` 进程的 `VehicleCoreService`；
 * 2. 死亡看门狗 (DeathRecipient)：挂载 `IBinder.DeathRecipient` 监听器，时刻监控 `:vehicle` 服务进程生命状态；
 * 3. 车规级指数退避自愈 (Exponential Backoff Reconnect)：当服务崩溃被杀时，按 `1s -> 2s -> 4s -> max 10s` 退避算法重连；
 * 4. 事件监听与快照推送：通过 AIDL 注册 `IVehicleEventListener`，并将跨进程回调转给 `VehiclePropertyAdapter`；
 * 5. 安全 IPC 调用封装：使用 `withService` 捕获 `RemoteException` 与 `DeadObjectException`，确保调用安全。
 */
class VehicleServiceConnectionManager(
    context: Context,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val appContext = context.applicationContext

    /** 当前与 :vehicle 服务进程的连接状态通道 */
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    /** 跨进程 BinderProxy 句柄 (IVehicleCoreManager) */
    private var vehicleCoreManager: IVehicleCoreManager? = null

    /** 已绑定连接的底座 IBinder 对象及其死亡监听器句柄 */
    private var linkedBinder: IBinder? = null
    private var linkedDeathRecipient: IBinder.DeathRecipient? = null

    /** 正在尝试 bindService 的标志位，防止并发重复绑定 */
    private var isBinding = false

    /** 退避重连的协程 Job 句柄 */
    private var reconnectJob: Job? = null

    /** 重连尝试次数计数器（用于计算指数退避延时） */
    private var reconnectAttempt = 0

    /** 记录开始绑定的时间戳，用于统计耗时 */
    private var bindStartedAtMs = 0L

    /**
     * AIDL 回调监听器实现 (IVehicleEventListener.Stub)。
     *
     * 运行线程：主进程的 Binder 线程池（由 Binder 驱动分发）。
     * 接收来自 :vehicle 进程回调的数据更新，并适配给 VehiclePropertyAdapter。
     */
    private val vehicleEventListener = object : IVehicleEventListener.Stub() {
        override fun onSpeedChanged(speed: Float) {
            CarLogger.d(TAG, "snapshot onSpeedChanged speed=$speed")
            VehiclePropertyAdapter.updateSpeed(speed)
        }

        override fun onGearChanged(gear: Int) {
            CarLogger.d(TAG, "snapshot onGearChanged gear=$gear")
            VehiclePropertyAdapter.updateGear(gear)
        }

        override fun onPowerStateChanged(state: Int) {
            CarLogger.d(TAG, "snapshot onPowerStateChanged state=$state")
            VehiclePropertyAdapter.updatePowerState(state)
        }

        override fun onDistractionStateChanged(isRestricted: Boolean) {
            CarLogger.d(TAG, "snapshot onDistractionStateChanged restricted=$isRestricted")
            VehiclePropertyAdapter.updateDistractionState(isRestricted)
        }
    }

    /**
     * Android 原生 ServiceConnection 服务绑定生命周期回调。
     */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            isBinding = false
            if (binder == null) {
                CarLogger.e(TAG, "onServiceConnected returned null binder")
                scheduleReconnect()
                return
            }

            // 1. 挂载 DeathRecipient 死亡监听器
            if (!linkToDeath(binder)) {
                CarLogger.e(TAG, "linkToDeath failed, binder already dead")
                scheduleReconnect()
                return
            }

            // 2. 转换 Binder 为 AIDL IVehicleCoreManager 接口代理
            val manager = IVehicleCoreManager.Stub.asInterface(binder)
            try {
                // 3. 跨进程注册客户端监听器，服务端会立即主动回推一份当前最新状态快照
                manager.registerListener(vehicleEventListener)
            } catch (e: RemoteException) {
                CarLogger.e(TAG, "registerListener failed", e)
                clearConnection()
                unbindQuietly()
                scheduleReconnect()
                return
            }

            vehicleCoreManager = manager
            reconnectAttempt = 0 // 重置指数退避计数器
            _isConnected.value = true

            val now = SystemClock.uptimeMillis()
            CarLogger.i(
                TAG,
                "Connected pid=${Process.myPid()} binder=${binder.javaClass.name} " +
                    "localInterface=${binder.queryLocalInterface(AIDL_DESCRIPTOR)} " +
                    "bindCostMs=${now - bindStartedAtMs} " +
                    "processAgeMs=${now - Process.getStartUptimeMillis()}"
            )
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            CarLogger.w(TAG, "Service disconnected, waiting for system rebind")
            clearConnection()
        }

        override fun onBindingDied(name: ComponentName?) {
            CarLogger.e(TAG, "Binding died, rebinding")
            clearConnection()
            unbindQuietly()
            scheduleReconnect()
        }

        override fun onNullBinding(name: ComponentName?) {
            isBinding = false
            CarLogger.e(TAG, "Service returned null binding")
            unbindQuietly()
            scheduleReconnect()
        }
    }

    /**
     * 发起 bindService 绑定请求，跨进程拉起 :vehicle 车辆服务进程。
     */
    fun connect() {
        if (isBinding || _isConnected.value || vehicleCoreManager != null) return
        isBinding = true
        bindStartedAtMs = SystemClock.uptimeMillis()

        val intent = Intent().setComponent(
            ComponentName(appContext.packageName, VEHICLE_SERVICE_CLASS)
        )
        CarLogger.i(
            TAG,
            "bindService requested component=${intent.component?.flattenToShortString()} " +
                "pid=${Process.myPid()} processAgeMs=${bindStartedAtMs - Process.getStartUptimeMillis()}"
        )

        if (!appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
            isBinding = false
            CarLogger.e(TAG, "bindService returned false")
            scheduleReconnect()
        }
    }

    /** AIDL 远程方法：注入模拟车速 */
    fun injectSpeed(speed: Float) = withService { it.injectMockSpeed(speed) }

    /** AIDL 远程方法：注入模拟档位 */
    fun injectGear(gear: VehicleGear) = withService { it.injectMockGear(gear.code) }

    /** AIDL 远程方法：注入模拟电源状态 */
    fun injectPower(powerState: PowerState) = withService { it.injectMockPower(powerState.code) }

    /** AIDL 远程方法：触发方向盘 PTT 语音按键 */
    fun triggerPtt() = withService { it.triggerPttVoice() }

    /**
     * 线程安全的 AIDL 远程方法执行高阶函数。
     *
     * 捕获 RemoteException/DeadObjectException 异常，在底层通信断开时自动触发断线自愈重连。
     */
    private inline fun withService(action: (IVehicleCoreManager) -> Unit) {
        val manager = vehicleCoreManager ?: return
        try {
            action(manager)
        } catch (e: RemoteException) {
            CarLogger.e(TAG, "Remote call failed", e)
            clearConnection()
            scheduleReconnect()
        }
    }

    /**
     * 挂载 Binder 死亡看门狗 (IBinder.DeathRecipient)。
     *
     * 当 :vehicle 进程意外崩溃或被系统 kill 时，Binder 驱动会自动回调 DeathRecipient，
     * 触发主进程清理断开并启动自愈重连流程。
     */
    private fun linkToDeath(binder: IBinder): Boolean {
        unlinkFromDeath()
        val recipient = IBinder.DeathRecipient {
            if (linkedBinder === binder) {
                CarLogger.e(TAG, "Vehicle service binder died! Triggering auto-reconnect...")
                clearConnection()
                unbindQuietly()
                scheduleReconnect()
            }
        }
        return try {
            binder.linkToDeath(recipient, 0)
            linkedBinder = binder
            linkedDeathRecipient = recipient
            true
        } catch (e: RemoteException) {
            false
        }
    }

    /**
     * 解绑死亡监听器。
     */
    private fun unlinkFromDeath() {
        val binder = linkedBinder
        val recipient = linkedDeathRecipient
        linkedBinder = null
        linkedDeathRecipient = null
        if (binder != null && recipient != null) {
            try {
                binder.unlinkToDeath(recipient, 0)
            } catch (e: Exception) {
            }
        }
    }

    /**
     * 清理连接状态与死句柄。
     */
    private fun clearConnection() {
        _isConnected.value = false
        vehicleCoreManager = null
        unlinkFromDeath()
    }

    /**
     * 安全解绑 Service，忽略重复 unbind 异常。
     */
    private fun unbindQuietly() {
        isBinding = false
        try {
            appContext.unbindService(connection)
        } catch (e: IllegalArgumentException) {
        }
    }

    /**
     * 指数退避重连算法调度器 (Exponential Backoff Reconnect Scheduler)。
     *
     * 延时序列：1s -> 2s -> 4s -> max 10s，防止频繁尝试崩溃打爆系统资源。
     */
    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectAttempt++

        val delayMs = (INITIAL_RECONNECT_DELAY_MS * (1 shl (reconnectAttempt - 1).coerceAtMost(3)))
            .coerceAtMost(MAX_RECONNECT_DELAY_MS)

        CarLogger.w(TAG, "scheduleReconnect attempt=$reconnectAttempt delay=${delayMs}ms")
        reconnectJob = externalScope.launch {
            delay(delayMs)
            connect()
        }
    }

    /**
     * 断开连接并释放所有重连与监听资源。
     */
    fun disconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        try {
            vehicleCoreManager?.unregisterListener(vehicleEventListener)
        } catch (e: RemoteException) {
            CarLogger.w(TAG, "unregisterListener failed: ${e.message}")
        }
        clearConnection()
        unbindQuietly()
    }

    companion object {
        private const val TAG = "VehicleServiceConn"
        private const val AIDL_DESCRIPTOR = "com.oem.car.middleware.IVehicleCoreManager"
        private const val VEHICLE_SERVICE_CLASS = "com.oem.car.middleware.vehicle.VehicleCoreService"

        /** 指数退避算法初始延时：1000 毫秒 */
        private const val INITIAL_RECONNECT_DELAY_MS = 1000L

        /** 指数退避算法最大延时：10000 毫秒 */
        private const val MAX_RECONNECT_DELAY_MS = 10000L
    }
}
