package com.oem.car.middleware.vehicle

import android.os.RemoteCallbackList
import com.oem.car.middleware.IVehicleCoreManager
import com.oem.car.middleware.IVehicleEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object CarCoreDaemon : IVehicleCoreManager.Stub() {
    private val callbacks = RemoteCallbackList<IVehicleEventListener>()
    
    private val _speed = MutableStateFlow(0f)
    val speedFlow = _speed.asStateFlow()

    private val _gear = MutableStateFlow(1) // 1=P, 8=D
    val gearFlow = _gear.asStateFlow()

    private val _isRestricted = MutableStateFlow(false)
    val isRestrictedFlow = _isRestricted.asStateFlow()

    private val _powerState = MutableStateFlow(0) // 0=RUNNING, 1=STR_SLEEP, 2=GARAGE_MODE
    val powerStateFlow = _powerState.asStateFlow()

    private val _voiceTriggerEvent = MutableStateFlow(0L)
    val voiceTriggerEvent = _voiceTriggerEvent.asStateFlow()

    override fun registerListener(listener: IVehicleEventListener) {
        callbacks.register(listener)
    }

    override fun unregisterListener(listener: IVehicleEventListener) {
        callbacks.unregister(listener)
    }

    override fun injectMockSpeed(speed: Float) {
        _speed.value = speed
        checkSafetyPolicy()
        dispatch { it.onSpeedChanged(speed) }
    }

    override fun injectMockGear(gear: Int) {
        _gear.value = gear
        checkSafetyPolicy()
        dispatch { it.onGearChanged(gear) }
    }

    override fun injectMockPower(state: Int) {
        _powerState.value = state
        dispatch { it.onPowerStateChanged(state) }
    }

    override fun triggerPttVoice() {
        _voiceTriggerEvent.value = System.currentTimeMillis()
    }

    private fun checkSafetyPolicy() {
        // 车规防分心：D档(8) 或 速度 > 0.5km/h 触发拦截
        val restricted = _gear.value == 8 || _speed.value > 0.5f
        if (_isRestricted.value != restricted) {
            _isRestricted.value = restricted
            dispatch { it.onDistractionStateChanged(restricted) }
        }
    }

    private fun dispatch(action: (IVehicleEventListener) -> Unit) {
        val count = callbacks.beginBroadcast()
        for (i in 0 until count) {
            try { action(callbacks.getBroadcastItem(i)) } catch (e: Exception) {}
        }
        callbacks.finishBroadcast()
    }
}
