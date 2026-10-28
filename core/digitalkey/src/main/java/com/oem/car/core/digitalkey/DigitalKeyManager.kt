package com.oem.car.core.digitalkey

import com.oem.car.core.model.DigitalKeyState
import com.oem.car.core.model.KeyTech
import com.oem.car.core.model.ProximityZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ============================================================================
 * CCC 3.0 数字钥匙与无感进出管理器 (Digital Key Manager)
 * ============================================================================
 *
 * 【核心职责】
 * 模拟车主携带数字钥匙靠近/远离车辆的过程，管理通信技术 (UWB/BLE/NFC)、距离测距、
 * 空间区域识别、车门自动解锁/落锁以及座舱内部启动授权。
 */
class DigitalKeyManager {
    private val _state = MutableStateFlow(
        DigitalKeyState(
            proximityZone = ProximityZone.FAR,
            distanceMeters = 12f,
            isDoorUnlocked = false,
            isEngineStartAllowed = false
        )
    )
    val state: StateFlow<DigitalKeyState> = _state.asStateFlow()

    /** 模拟钥匙靠近车辆一步 */
    fun moveCloser() = moveZone(1)

    /** 模拟钥匙远离车辆一步 */
    fun moveAway() = moveZone(-1)

    /** 循环切换数字钥匙底层通信技术 (UWB ➔ BLE ➔ NFC) */
    fun cycleTech() {
        val next = KeyTech.entries[(_state.value.activeTech.ordinal + 1) % KeyTech.entries.size]
        _state.value = _state.value.copy(activeTech = next)
    }

    /** 切换全车车门锁止/解锁状态 */
    fun toggleDoorLock() {
        _state.value = _state.value.copy(isDoorUnlocked = !_state.value.isDoorUnlocked)
    }

    /** 内部逻辑：按步长调整空间接近区域与门锁/启动授权状态 */
    private fun moveZone(delta: Int) {
        val current = _state.value
        val nextOrdinal = (current.proximityZone.ordinal + delta)
            .coerceIn(0, ProximityZone.entries.lastIndex)
        val nextZone = ProximityZone.entries[nextOrdinal]
        val doorUnlocked = when (nextZone) {
            ProximityZone.FAR, ProximityZone.APPROACHING -> false
            ProximityZone.WELCOME -> current.isDoorUnlocked
            ProximityZone.UNLOCK, ProximityZone.CABIN_INSIDE -> true
        }
        _state.value = current.copy(
            proximityZone = nextZone,
            distanceMeters = distanceFor(nextZone),
            isDoorUnlocked = doorUnlocked,
            isEngineStartAllowed = nextZone == ProximityZone.CABIN_INSIDE && current.isSeAuthenticated
        )
    }

    /** 获取对应空间区域的模拟距离 (米) */
    private fun distanceFor(zone: ProximityZone): Float = when (zone) {
        ProximityZone.FAR -> 12f
        ProximityZone.APPROACHING -> 8f
        ProximityZone.WELCOME -> 4f
        ProximityZone.UNLOCK -> 1.2f
        ProximityZone.CABIN_INSIDE -> 0.5f
    }
}
