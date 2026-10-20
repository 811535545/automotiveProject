package com.oem.car.core.telematics

import com.oem.car.core.model.OtaState
import com.oem.car.core.model.TelematicsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TelematicsManager(
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(TelematicsState())
    val state: StateFlow<TelematicsState> = _state.asStateFlow()

    private var otaJob: Job? = null

    fun toggleSentryMode() {
        _state.value = _state.value.copy(isSentryModeActive = !_state.value.isSentryModeActive)
    }

    fun sendRemoteCommand(command: String) {
        _state.value = _state.value.copy(lastRemoteCommand = command)
    }

    fun startOtaUpdate() {
        if (otaJob?.isActive == true) return
        otaJob = scope.launch {
            updateOta(OtaState.CHECKING, 0)
            delay(500)
            updateOta(OtaState.DOWNLOADING, 5)
            for (progress in 10..70 step 10) {
                delay(300)
                updateOta(OtaState.DOWNLOADING, progress)
            }
            delay(300)
            updateOta(OtaState.VERIFYING, 80)
            delay(500)
            updateOta(OtaState.APPLYING, 95)
            delay(500)
            updateOta(OtaState.SUCCESS, 100)
        }
    }

    private fun updateOta(otaState: OtaState, progress: Int) {
        _state.value = _state.value.copy(
            otaState = otaState,
            otaProgressPercent = progress
        )
    }
}
