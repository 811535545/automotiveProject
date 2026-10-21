package com.oem.car.feature.voice

import com.oem.car.core.common.CarLogger
import com.oem.car.core.data.VehicleRepository
import com.oem.car.core.model.DriveMode

/**
 * AppFunction 执行结果。
 */
data class AppFunctionResult(
    val isSuccess: Boolean,
    val feedbackText: String
)

/**
 * Android AppFunctions 智能语音/AI Agent 控车意图处理器。
 *
 * 允许 AI 语音助手无需拉起 UI 界面，直接以系统 AppFunctions 规范触发车辆控制能力。
 */
class CarAppFunctionsHandler(
    private val repository: VehicleRepository
) {
    fun executeFunction(functionName: String, parameters: Map<String, Any>): AppFunctionResult {
        CarLogger.i(TAG, "Executing AppFunction: name=$functionName params=$parameters")
        return when (functionName) {
            "set_driver_temperature" -> {
                val temp = (parameters["temp"] as? Number)?.toFloat() ?: 22f
                repository.updateClimate { it.copy(driverTemp = temp) }
                AppFunctionResult(true, "已将主驾空调温度设置为 ${temp}°C")
            }
            "toggle_door_lock" -> {
                repository.toggleDoorLock()
                val unlocked = repository.digitalKeyState.value.isDoorUnlocked
                AppFunctionResult(true, if (unlocked) "已解锁全车车门" else "已锁止全车车门")
            }
            "set_drive_mode" -> {
                val modeName = parameters["mode"] as? String ?: "SPORT"
                val mode = try { DriveMode.valueOf(modeName) } catch (e: Exception) { DriveMode.COMFORT }
                repository.setDriveMode(mode)
                AppFunctionResult(true, "已将驾驶模式切换为 ${mode.display}")
            }
            "adjust_cruise_speed" -> {
                val delta = (parameters["delta"] as? Number)?.toFloat() ?: 5f
                repository.adjustCruiseSpeed(delta)
                val targetSpeed = repository.adasState.value.cruiseSpeed
                AppFunctionResult(true, "巡航设定车速已调整为 ${targetSpeed.toInt()} km/h")
            }
            "toggle_sentry_mode" -> {
                repository.toggleSentryMode()
                val active = repository.telematicsState.value.isSentryModeActive
                AppFunctionResult(true, if (active) "已开启哨兵模式" else "已关闭哨兵模式")
            }
            else -> {
                AppFunctionResult(false, "未知的控车指令: $functionName")
            }
        }
    }

    companion object {
        private const val TAG = "AppFunctionsHandler"
    }
}
