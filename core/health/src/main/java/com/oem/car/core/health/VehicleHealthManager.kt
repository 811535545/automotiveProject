package com.oem.car.core.health

import com.oem.car.core.model.DtcCode
import com.oem.car.core.model.HealthSeverity
import com.oem.car.core.model.VehicleHealthState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ============================================================================
 * 车辆健康诊断与 DTC 故障码管理器 (Vehicle Health & Diagnostics Manager)
 * ============================================================================
 *
 * 【核心职责】
 * 模拟全车系统诊断扫描（如制动、气囊、ESP、胎压）、UDS 诊断故障码 (DTC) 的扫描检索与清除。
 */
class VehicleHealthManager(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(VehicleHealthState())
    val state: StateFlow<VehicleHealthState> = _state.asStateFlow()

    private var scanJob: Job? = null

    /** 启动车辆健康诊断扫描（模拟 1.5 秒后返回故障码） */
    fun scan() {
        if (_state.value.isScanning) return
        _state.update { it.copy(isScanning = true, lastScanResult = "正在扫描车辆健康...") }
        scanJob = scope.launch {
            delay(1500)
            _state.update {
                it.copy(
                    isScanning = false,
                    lastScanResult = "扫描完成：发现 2 个历史故障码",
                    dtcs = listOf(
                        DtcCode("P0456", "蒸发排放系统微小泄漏", HealthSeverity.WARNING),
                        DtcCode("B1A00", "右前轮胎压偏低", HealthSeverity.WARNING)
                    )
                )
            }
        }
    }

    /** 清空所有已存储的 DTC 故障码 */
    fun clearDtcs() {
        scanJob?.cancel()
        scanJob = null
        _state.update { it.copy(isScanning = false, dtcs = emptyList(), lastScanResult = "故障码已清除") }
    }
}
