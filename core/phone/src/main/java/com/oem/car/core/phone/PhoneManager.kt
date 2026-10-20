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

class PhoneManager(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(PhoneState())
    val state: StateFlow<PhoneState> = _state.asStateFlow()

    private var callJob: Job? = null

    fun toggleBluetooth() {
        if (_state.value.isBluetoothConnected) {
            endCall()
            _state.update { it.copy(isBluetoothConnected = false, audioRoute = "未连接") }
        } else {
            _state.update { it.copy(isBluetoothConnected = true, audioRoute = "车机扬声器") }
        }
    }

    fun dial(contact: Contact) {
        if (!_state.value.isBluetoothConnected) return
        startCall(contact, CallStatus.DIALING)
    }

    fun simulateIncomingCall(contact: Contact) {
        if (!_state.value.isBluetoothConnected) return
        startCall(contact, CallStatus.RINGING)
    }

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

    fun answerCall() {
        if (_state.value.callStatus != CallStatus.RINGING) return
        callJob?.cancel()
        _state.update { it.copy(callStatus = CallStatus.ACTIVE, callDurationSec = 0) }
        callJob = scope.launch { runTimer() }
    }

    private suspend fun runTimer() {
        while (scope.isActive && _state.value.callStatus == CallStatus.ACTIVE) {
            delay(1000)
            _state.update { it.copy(callDurationSec = it.callDurationSec + 1) }
        }
    }

    fun hangUp() = endCall()

    fun toggleMute() {
        if (_state.value.callStatus == CallStatus.ACTIVE) {
            _state.update { it.copy(isMuted = !it.isMuted) }
        }
    }

    private fun endCall() {
        callJob?.cancel()
        callJob = null
        _state.update {
            it.copy(callStatus = CallStatus.IDLE, currentContact = null, callDurationSec = 0, isMuted = false)
        }
    }
}
