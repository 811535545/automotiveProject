package com.oem.car.core.audio

import com.oem.car.core.common.CarLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 车载多音区定义 (符合 AAOS CarAudioService 多音区标准)。
 */
enum class AudioZone(val zoneId: Int, val description: String) {
    DRIVER_ZONE_0(0, "主驾音区 (Driver Zone 0)"),
    PASSENGER_ZONE_1(1, "副驾音区 (Passenger Zone 1)"),
    REAR_ZONE_2(2, "后排音区 (Rear Zone 2)")
}

/**
 * 音频使用类型。
 */
enum class CarAudioUsage {
    MEDIA,
    NAVIGATION,
    VOICE_COMMAND,
    ALARM
}

/**
 * 各音区的状态快照。
 */
data class ZoneAudioStatus(
    val zone: AudioZone,
    val isMediaPlaying: Boolean = false,
    val isNavGuiding: Boolean = false,
    val isVoiceActive: Boolean = false,
    val volumeMultiplier: Float = 1.0f
)

/**
 * 多音区音频焦点仲裁引擎 (Multi-Zone Audio Focus Arbitrator)。
 *
 * 核心车载音频策略：
 * 1. 导航播报 (NAVIGATION) 只作用于 DRIVER_ZONE_0：主驾媒体自动 Ducking 降低至 30% 音量，而 PASSENGER_ZONE_1 与 REAR_ZONE_2 保持 100% 音量不受干扰（物理音区隔离）。
 * 2. 语音助手 (VOICE_COMMAND/PTT) 属于最高优先级全车事件：触发时全车媒体暂停/降音。
 */
class MultiZoneAudioManager {

    private val _zoneStatuses = MutableStateFlow(
        mapOf(
            AudioZone.DRIVER_ZONE_0 to ZoneAudioStatus(AudioZone.DRIVER_ZONE_0),
            AudioZone.PASSENGER_ZONE_1 to ZoneAudioStatus(AudioZone.PASSENGER_ZONE_1),
            AudioZone.REAR_ZONE_2 to ZoneAudioStatus(AudioZone.REAR_ZONE_2)
        )
    )
    val zoneStatuses: StateFlow<Map<AudioZone, ZoneAudioStatus>> = _zoneStatuses.asStateFlow()

    fun requestAudioFocus(zone: AudioZone, usage: CarAudioUsage): Boolean {
        CarLogger.i(TAG, "AudioFocus requested: zone=${zone.name}, usage=${usage.name}")
        val currentMap = _zoneStatuses.value.toMutableMap()
        val currentStatus = currentMap[zone] ?: ZoneAudioStatus(zone)

        val updatedStatus = when (usage) {
            CarAudioUsage.MEDIA -> currentStatus.copy(isMediaPlaying = true)
            CarAudioUsage.NAVIGATION -> currentStatus.copy(isNavGuiding = true)
            CarAudioUsage.VOICE_COMMAND -> currentStatus.copy(isVoiceActive = true)
            CarAudioUsage.ALARM -> currentStatus.copy(isNavGuiding = true)
        }
        currentMap[zone] = updatedStatus
        _zoneStatuses.value = recalculateFocus(currentMap)
        return true
    }

    fun abandonAudioFocus(zone: AudioZone, usage: CarAudioUsage) {
        CarLogger.i(TAG, "AudioFocus abandoned: zone=${zone.name}, usage=${usage.name}")
        val currentMap = _zoneStatuses.value.toMutableMap()
        val currentStatus = currentMap[zone] ?: return

        val updatedStatus = when (usage) {
            CarAudioUsage.MEDIA -> currentStatus.copy(isMediaPlaying = false)
            CarAudioUsage.NAVIGATION -> currentStatus.copy(isNavGuiding = false)
            CarAudioUsage.VOICE_COMMAND -> currentStatus.copy(isVoiceActive = false)
            CarAudioUsage.ALARM -> currentStatus.copy(isNavGuiding = false)
        }
        currentMap[zone] = updatedStatus
        _zoneStatuses.value = recalculateFocus(currentMap)
    }

    private fun recalculateFocus(
        statuses: MutableMap<AudioZone, ZoneAudioStatus>
    ): Map<AudioZone, ZoneAudioStatus> {
        val isGlobalVoiceActive = statuses.values.any { it.isVoiceActive }

        statuses.keys.forEach { zone ->
            val status = statuses[zone] ?: return@forEach
            val newMultiplier = when {
                isGlobalVoiceActive -> 0.1f // 全车 PTT 时大降音
                zone == AudioZone.DRIVER_ZONE_0 && status.isNavGuiding -> 0.3f // 主驾导航 Ducking
                else -> 1.0f // 副驾/后排不受主驾导航影响
            }
            statuses[zone] = status.copy(volumeMultiplier = newMultiplier)
        }
        return statuses
    }

    companion object {
        private const val TAG = "MultiZoneAudio"
    }

}
