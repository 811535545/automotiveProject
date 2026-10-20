package com.oem.car.feature.connected

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oem.car.core.data.VehicleRepository
import com.oem.car.core.data.VehicleRepositoryImpl
import com.oem.car.core.designsystem.CarButton
import com.oem.car.core.designsystem.CarCard
import com.oem.car.core.designsystem.CarTheme
import com.oem.car.core.model.AdasPerceptionState
import com.oem.car.core.model.DigitalKeyState
import com.oem.car.core.model.OtaState
import com.oem.car.core.model.TelematicsState
import com.oem.car.core.model.UserProfile

@Composable
fun ConnectedScreen(
    repository: VehicleRepository = VehicleRepositoryImpl
) {
    val digitalKey by repository.digitalKeyState.collectAsState()
    val telematics by repository.telematicsState.collectAsState()
    val adas by repository.adasState.collectAsState()
    val profile by repository.userProfile.collectAsState()

    CarTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("智能互联与座舱扩展能力", fontSize = 18.sp, color = Color.Gray)
                Text("仿真模式：不连接真车与云端", fontSize = 12.sp, color = Color(0xFFFFB74D))
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DigitalKeyCard(
                    state = digitalKey,
                    modifier = Modifier.weight(1f),
                    onCloser = repository::moveKeyCloser,
                    onAway = repository::moveKeyAway,
                    onCycleTech = repository::cycleKeyTech,
                    onToggleLock = repository::toggleDoorLock
                )
                TelematicsCard(
                    state = telematics,
                    modifier = Modifier.weight(1f),
                    onRemoteAc = { repository.sendRemoteCommand("远程开启空调") },
                    onRemoteUnlock = { repository.sendRemoteCommand("远程解锁车门") },
                    onToggleSentry = repository::toggleSentryMode,
                    onStartOta = repository::startOtaUpdate
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdasCard(
                    state = adas,
                    modifier = Modifier.weight(1f),
                    onToggleNoa = repository::toggleNoa,
                    onToggleLaneKeep = repository::toggleLaneKeep,
                    onCruiseDown = { repository.adjustCruiseSpeed(-5f) },
                    onCruiseUp = { repository.adjustCruiseSpeed(5f) }
                )
                AccountCard(
                    profile = profile,
                    modifier = Modifier.weight(1f),
                    onVerifyFace = repository::verifyDriverFace,
                    onCycleSeat = repository::cycleSeatPosition,
                    onTempDown = { repository.adjustPreferredTemp(-0.5f) },
                    onTempUp = { repository.adjustPreferredTemp(0.5f) },
                    onApplyProfile = repository::applyUserProfile
                )
            }
        }
    }
}

@Composable
private fun DigitalKeyCard(
    state: DigitalKeyState,
    modifier: Modifier = Modifier,
    onCloser: () -> Unit,
    onAway: () -> Unit,
    onCycleTech: () -> Unit,
    onToggleLock: () -> Unit
) {
    CarCard(modifier = modifier) {
        Text("🔑 数字钥匙 (CCC 3.0 仿真)", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("技术: ${state.activeTech.display}", fontSize = 13.sp)
        Text("区域: ${state.proximityZone.display}", fontSize = 13.sp)
        Text("距离: ${"%.1f".format(state.distanceMeters)} m", fontSize = 13.sp)
        Text(
            "门锁: ${if (state.isDoorUnlocked) "已解锁" else "已上锁"} / " +
                "启动: ${if (state.isEngineStartAllowed) "允许" else "禁止"}",
            fontSize = 13.sp,
            color = if (state.isDoorUnlocked) Color(0xFF00E676) else Color(0xFFFF8A80)
        )
        Text("SE 鉴权: ${if (state.isSeAuthenticated) "通过" else "未通过"}", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CarButton("靠近", onClick = onCloser)
            CarButton("远离", onClick = onAway)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CarButton("切换技术", onClick = onCycleTech)
            CarButton(if (state.isDoorUnlocked) "上锁" else "解锁", onClick = onToggleLock)
        }
    }
}

@Composable
private fun TelematicsCard(
    state: TelematicsState,
    modifier: Modifier = Modifier,
    onRemoteAc: () -> Unit,
    onRemoteUnlock: () -> Unit,
    onToggleSentry: () -> Unit,
    onStartOta: () -> Unit
) {
    val otaRunning = state.otaState != OtaState.IDLE && state.otaState != OtaState.SUCCESS

    CarCard(modifier = modifier) {
        Text("📡 T-BOX / 车云 / OTA", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "云连接: ${if (state.isConnectedToCloud) "在线" else "离线"} / 信号 ${state.signalStrengthDbm} dBm",
            fontSize = 13.sp
        )
        Text("哨兵模式: ${if (state.isSentryModeActive) "已开启" else "已关闭"}", fontSize = 13.sp)
        Text("OTA: ${state.otaState.display} (${state.otaProgressPercent}%)", fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { state.otaProgressPercent / 100f },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text("最近远控: ${state.lastRemoteCommand}", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CarButton("远程空调", onClick = onRemoteAc)
            CarButton("远程解锁", onClick = onRemoteUnlock)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CarButton(if (state.isSentryModeActive) "关闭哨兵" else "开启哨兵", onClick = onToggleSentry)
            CarButton(
                text = if (otaRunning) "OTA 进行中" else "OTA 升级",
                enabled = !otaRunning,
                onClick = onStartOta
            )
        }
    }
}

@Composable
private fun AdasCard(
    state: AdasPerceptionState,
    modifier: Modifier = Modifier,
    onToggleNoa: () -> Unit,
    onToggleLaneKeep: () -> Unit,
    onCruiseDown: () -> Unit,
    onCruiseUp: () -> Unit
) {
    CarCard(modifier = modifier) {
        Text("🛣️ ADAS / SR 感知仿真", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("NOA 领航: ${if (state.isNoaActive) "开启" else "关闭"}", fontSize = 13.sp)
        Text("车道保持: ${if (state.laneKeepActive) "开启" else "关闭"}", fontSize = 13.sp)
        Text("巡航速度: ${state.cruiseSpeed.toInt()} km/h", fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text("感知目标 (${state.targets.size}):", fontSize = 12.sp, color = Color.Gray)
        state.targets.take(2).forEach { target ->
            Text(
                "• ${target.type}  ${target.distanceX.toInt()}m",
                fontSize = 12.sp,
                color = Color(0xFF80D8FF)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CarButton(if (state.isNoaActive) "关闭 NOA" else "开启 NOA", onClick = onToggleNoa)
            CarButton(if (state.laneKeepActive) "关闭车道" else "开启车道", onClick = onToggleLaneKeep)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CarButton("巡航 -5", onClick = onCruiseDown)
            CarButton("巡航 +5", onClick = onCruiseUp)
        }
    }
}

@Composable
private fun AccountCard(
    profile: UserProfile,
    modifier: Modifier = Modifier,
    onVerifyFace: () -> Unit,
    onCycleSeat: () -> Unit,
    onTempDown: () -> Unit,
    onTempUp: () -> Unit,
    onApplyProfile: () -> Unit
) {
    CarCard(modifier = modifier) {
        Text("👤 DMS 人脸 / 多账户", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("账户: ${profile.userName}", fontSize = 13.sp)
        Text(
            "人脸识别: ${if (profile.isFaceIdVerified) "已验证" else "待识别"}",
            fontSize = 13.sp,
            color = if (profile.isFaceIdVerified) Color(0xFF00E676) else Color(0xFFFFB74D)
        )
        Text("座椅记忆: ${profile.seatMemoryPosition} / 后视镜: ${profile.mirrorAngleDeg}°", fontSize = 13.sp)
        Text("偏好温度: ${"%.1f".format(profile.preferredTemp)} °C", fontSize = 13.sp)
        Text("氛围灯: ${profile.ambientColorHex}", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CarButton("人脸识别", onClick = onVerifyFace)
            CarButton("切换座椅", onClick = onCycleSeat)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CarButton("温度 -0.5", onClick = onTempDown)
            CarButton("温度 +0.5", onClick = onTempUp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        CarButton(
            text = "应用个性化到座舱",
            enabled = profile.isFaceIdVerified,
            modifier = Modifier.fillMaxWidth(),
            onClick = onApplyProfile
        )
    }
}
