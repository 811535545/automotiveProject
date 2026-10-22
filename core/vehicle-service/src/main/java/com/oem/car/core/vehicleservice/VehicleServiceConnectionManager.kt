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

class VehicleServiceConnectionManager(
    context: Context,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val appContext = context.applicationContext

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var vehicleCoreManager: IVehicleCoreManager? = null
    private var linkedBinder: IBinder? = null
    private var linkedDeathRecipient: IBinder.DeathRecipient? = null
    private var isBinding = false
    private var reconnectJob: Job? = null
    private var bindStartedAtMs = 0L

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

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            isBinding = false
            if (binder == null) {
                CarLogger.e(TAG, "onServiceConnected returned null binder")
                scheduleReconnect()
                return
            }
            if (!linkToDeath(binder)) {
                CarLogger.e(TAG, "linkToDeath failed, binder already dead")
                scheduleReconnect()
                return
            }
            val manager = IVehicleCoreManager.Stub.asInterface(binder)
            try {
                manager.registerListener(vehicleEventListener)
            } catch (e: RemoteException) {
                CarLogger.e(TAG, "registerListener failed", e)
                clearConnection()
                unbindQuietly()
                scheduleReconnect()
                return
            }
            vehicleCoreManager = manager
            reconnectAttempt = 0
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

    fun injectSpeed(speed: Float) = withService { it.injectMockSpeed(speed) }

    fun injectGear(gear: VehicleGear) = withService { it.injectMockGear(gear.code) }

    fun injectPower(powerState: PowerState) = withService { it.injectMockPower(powerState.code) }

    fun triggerPtt() = withService { it.triggerPttVoice() }

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

    private fun linkToDeath(binder: IBinder): Boolean {
        unlinkFromDeath()
        val recipient = IBinder.DeathRecipient {
            if (linkedBinder === binder) {
                CarLogger.e(TAG, "Vehicle service binder died")
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

    private fun clearConnection() {
        _isConnected.value = false
        vehicleCoreManager = null
        unlinkFromDeath()
    }

    private fun unbindQuietly() {
        isBinding = false
        try {
            appContext.unbindService(connection)
        } catch (e: IllegalArgumentException) {
        }
    }

    private var reconnectAttempt = 0

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectAttempt++
        // 指数退避算法: 1s -> 2s -> 4s -> max 10s
        val delayMs = (INITIAL_RECONNECT_DELAY_MS * (1 shl (reconnectAttempt - 1).coerceAtMost(3)))
            .coerceAtMost(MAX_RECONNECT_DELAY_MS)
        CarLogger.w(TAG, "scheduleReconnect attempt=$reconnectAttempt delay=${delayMs}ms")
        reconnectJob = externalScope.launch {
            delay(delayMs)
            connect()
        }
    }

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
        private const val INITIAL_RECONNECT_DELAY_MS = 1000L
        private const val MAX_RECONNECT_DELAY_MS = 10000L
    }
}
