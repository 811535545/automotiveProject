package com.oem.car.core.vehicleservice

import android.content.Context
import com.oem.car.core.common.CarLogger
import com.oem.car.core.model.PowerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 核心座舱中间件总协调器 (Cockpit Middleware Core Coordinator)。
 *
 * 封装座舱中间件 5 大核心服务：
 * 1. 开机自启与生命周期拉起 (Boot & Lifecycle Management)
 * 2. 车辆信号路由分发与 VHAL 桥接 (Vehicle Signal Dispatcher)
 * 3. 信号超时降级看门狗 (Signal Timeout Watchdog)
 * 4. 多音区音频焦点与优先级仲裁 (Multi-Zone Audio Focus Policy)
 * 5. 电源状态机与 STR 休眠/唤醒挂管 (Power State Machine)
 */
class CockpitMiddlewareCoordinator private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isMiddlewareReady = MutableStateFlow(false)
    val isMiddlewareReady = _isMiddlewareReady.asStateFlow()

    private val _currentPowerState = MutableStateFlow(PowerState.RUNNING)
    val currentPowerState = _currentPowerState.asStateFlow()

    val propertyProvider: VehiclePropertyProvider = VehiclePropertyProviderFactory.create(appContext)

    fun startMiddlewareServices() {
        CarLogger.i(TAG, "Starting Core Cockpit Middleware Services...")

        // 1. 启动信号超时降级看门狗
        VehiclePropertyAdapter.startSignalTimeoutWatchdog(scope)

        // 2. 连接 VHAL / Mock Binder 车身服务
        scope.launch {
            propertyProvider.isConnected.collect { connected ->
                _isMiddlewareReady.value = connected
                CarLogger.i(TAG, "Middleware VHAL connection status changed: $connected (${propertyProvider.getProviderName()})")
            }
        }
        propertyProvider.connect()
    }

    fun onPowerStateTransition(newState: PowerState) {
        CarLogger.i(TAG, "Power state transition: ${_currentPowerState.value} -> $newState")
        _currentPowerState.value = newState

        when (newState) {
            PowerState.STR_SLEEP -> {
                CarLogger.w(TAG, "Entering STR (Suspend-to-RAM) sleep! Flushing middleware cache & suspending sockets...")
                // 休眠处理：挂起非必要连接、保存持久化快照
            }
            PowerState.SHUTDOWN_PREPARE -> {
                CarLogger.w(TAG, "Shutdown prepare! Releasing middleware resources...")
                propertyProvider.disconnect()
            }
            PowerState.RUNNING -> {
                CarLogger.i(TAG, "System wake up! Re-establishing VHAL connections...")
                propertyProvider.connect()
            }
            else -> {}
        }
    }

    companion object {
        private const val TAG = "CockpitMiddleware"

        @Volatile
        private var instance: CockpitMiddlewareCoordinator? = null

        fun getInstance(context: Context): CockpitMiddlewareCoordinator {
            return instance ?: synchronized(this) {
                instance ?: CockpitMiddlewareCoordinator(context).also { instance = it }
            }
        }
    }
}
