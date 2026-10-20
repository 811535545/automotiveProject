package com.oem.car.core.vehicleservice

import android.os.IBinder
import com.oem.car.core.common.CarLogger
import com.oem.car.core.model.CarSignal
import com.oem.car.core.model.PowerState
import com.oem.car.core.model.VehicleGear
import com.oem.car.middleware.IVehicleCoreManager
import com.oem.car.middleware.IVehicleEventListener
import com.oem.car.middleware.vehicle.CarCoreDaemon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VehicleServiceConnectionManager(
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var vehicleCoreManager: IVehicleCoreManager? = null

    private val deathRecipient = IBinder.DeathRecipient {
        CarLogger.e("VehicleServiceConn", "Vehicle Service Binder Died! Triggering auto-reconnect...")
        _isConnected.value = false
        vehicleCoreManager = null
        scheduleReconnect()
    }

    private val vehicleEventListener = object : IVehicleEventListener.Stub() {
        override fun onSpeedChanged(speed: Float) {
            VehiclePropertyAdapter.updateSpeed(speed)
        }

        override fun onGearChanged(gear: Int) {
            VehiclePropertyAdapter.updateGear(gear)
        }

        override fun onPowerStateChanged(state: Int) {
            VehiclePropertyAdapter.updatePowerState(state)
        }

        override fun onDistractionStateChanged(isRestricted: Boolean) {
            VehiclePropertyAdapter.updateDistractionState(isRestricted)
        }
    }

    fun connect() {
        CarLogger.i("VehicleServiceConn", "Connecting to CarCoreDaemon Vehicle Service...")
        externalScope.launch {
            try {
                // In production AAOS, this binds via bindService(Intent("com.oem.car.VEHICLE_SERVICE"))
                // Here we connect to the CarCoreDaemon Binder interface with DeathRecipient monitoring.
                val binder = CarCoreDaemon.asBinder()
                binder.linkToDeath(deathRecipient, 0)

                vehicleCoreManager = CarCoreDaemon
                vehicleCoreManager?.registerListener(vehicleEventListener)

                _isConnected.value = true
                CarLogger.i("VehicleServiceConn", "Successfully connected to Vehicle Service with Binder DeathRecipient protection!")
            } catch (e: Exception) {
                CarLogger.e("VehicleServiceConn", "Connection failed", e)
                scheduleReconnect()
            }
        }
    }

    fun injectSpeed(speed: Float) {
        vehicleCoreManager?.injectMockSpeed(speed)
    }

    fun injectGear(gear: VehicleGear) {
        vehicleCoreManager?.injectMockGear(gear.code)
    }

    fun injectPower(powerState: PowerState) {
        vehicleCoreManager?.injectMockPower(powerState.code)
    }

    fun triggerPtt() {
        vehicleCoreManager?.triggerPttVoice()
    }

    private fun scheduleReconnect() {
        externalScope.launch {
            delay(3000)
            CarLogger.i("VehicleServiceConn", "Attempting reconnection...")
            connect()
        }
    }

    fun disconnect() {
        try {
            vehicleCoreManager?.unregisterListener(vehicleEventListener)
            CarCoreDaemon.asBinder().unlinkToDeath(deathRecipient, 0)
        } catch (e: Exception) {
            CarLogger.w("VehicleServiceConn", "Error during disconnect: ${e.message}")
        } finally {
            _isConnected.value = false
        }
    }
}
