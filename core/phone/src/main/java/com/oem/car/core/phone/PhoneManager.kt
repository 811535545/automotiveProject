package com.oem.car.core.phone

import com.oem.car.core.model.CallStatus
import com.oem.car.core.model.Contact
import com.oem.car.core.model.PhoneState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ============================================================================
 * 蓝牙免提电话与通话状态机管理器 (Bluetooth Phone & Call Manager)
 * ============================================================================
 *
 * 【核心职责】
 * 管理蓝牙连接状态、通讯录联系人、呼出拨号状态机 (`DIALING ➔ ACTIVE`)、模拟来电 (`RINGING`)、
 * 每秒通话计时、麦克风静音以及挂断控制。
 */
class PhoneManager(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(PhoneState())
    val state: StateFlow<PhoneState> = _state.asStateFlow()

    private var callJob: Job? = null

    /** 切换蓝牙连接/断开状态 */
    fun toggleBluetooth() {
        if (_state.value.isBluetoothConnected) {
            endCall()
            _state.update { it.copy(isBluetoothConnected = false, audioRoute = "未连接") }
        } else {
            _state.update { it.copy(isBluetoothConnected = true, audioRoute = "车机扬声器") }
        }
    }

    /** 向指定联系人拨打电话 */
    fun dial(contact: Contact) {
        if (!_state.value.isBluetoothConnected) return
        startCall(contact, CallStatus.DIALING)
    }

    /** 模拟收到外部来电 */
    fun simulateIncomingCall(contact: Contact) {
        if (!_state.value.isBluetoothConnected) return
        startCall(contact, CallStatus.RINGING)
    }

    /** 内部逻辑：启动通话状态机 */
    private fun startCall(contact: Contact, status: CallStatus) {
        callJob?.cancel()
        _state.update {
            it.copy(callStatus = status, currentContact = contact, callDurationSec = 0, isMuted = false)
        }
        if (status == CallStatus.DIALING) {
            callJob = scope.launch {
                delay(1200)
                if (_state.value.callStatus != CallStatus.DIALING) return@launch
                _state.update { it.copy(callStatus = CallStatus.ACTIVE) }
                runTimer()
            }
        }
    }

    /** 接听外部来电 */
    fun answerCall() {
        if (_state.value.callStatus != CallStatus.RINGING) return
        callJob?.cancel()
        _state.update { it.copy(callStatus = CallStatus.ACTIVE, callDurationSec = 0) }
        callJob = scope.launch { runTimer() }
    }

    /** 通话计时器协程（每秒自增 1 秒） */
    private suspend fun runTimer() {
        while (scope.isActive && _state.value.callStatus == CallStatus.ACTIVE) {
            delay(1000)
            _state.update { it.copy(callDurationSec = it.callDurationSec + 1) }
        }
    }

    /** 挂断电话 */
    fun hangUp() = endCall()

    /** 切换通话麦克风静音状态 */
    fun toggleMute() {
        if (_state.value.callStatus == CallStatus.ACTIVE) {
            _state.update { it.copy(isMuted = !it.isMuted) }
        }
    }

    /** 内部逻辑：结束通话并复位状态 */
    private fun endCall() {
        callJob?.cancel()
        callJob = null
        _state.update {
            it.copy(callStatus = CallStatus.IDLE, currentContact = null, callDurationSec = 0, isMuted = false)
        }
    }
}
