package com.oem.car.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oem.car.core.designsystem.*
import com.oem.car.core.model.PowerState
import com.oem.car.core.model.VehicleGear

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val speedSignal by viewModel.speedSignal.collectAsState()
    val gearSignal by viewModel.gearSignal.collectAsState()
    val powerSignal by viewModel.powerSignal.collectAsState()
    val uxRestriction by viewModel.uxRestrictionState.collectAsState()
    val audioState by viewModel.audioZoneState.collectAsState()

    CarTheme {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // 左侧：座舱核心数据与总线注入模拟器（加入垂直滚动支持）
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(end = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("AAOS 智能座舱中间件 (Enterprise)", fontSize = 18.sp, color = Color.Gray)
                    SafetyBadge(isRestricted = uxRestriction.isRestricted)
                }

                Spacer(modifier = Modifier.height(12.dp))

                CarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SpeedGaugeComponent(
                                speed = speedSignal.value,
                                gearDisplay = gearSignal.value.display
                            )
                            Column {
                                Text("Service 挂载: ${if (isConnected) "🟢 已连接 (DeathRecipient 监控中)" else "🔴 未连接"}", fontSize = 13.sp)
                                Text("电源状态: ${powerSignal.value.display}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                if (audioState.isPttActive) {
                                    Text("🎙️ PTT 语音触发: 已抢占音频焦点 (Duck Music)", color = Color.Magenta, fontSize = 13.sp)
                                }
                            }
                        }

                        // 3D 数字孪生渲染器
                        Vehicle3dVisualizer(
                            speed = speedSignal.value,
                            isDoorLocked = true,
                            isDriving = gearSignal.value == VehicleGear.DRIVE && speedSignal.value > 0.5f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("【总线仿真 Injector】", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CarButton("P档 (0 km/h)", onClick = {
                        viewModel.injectGear(VehicleGear.PARK)
                        viewModel.injectSpeed(0f)
                    })
                    CarButton("D档 (80 km/h)", onClick = {
                        viewModel.injectGear(VehicleGear.DRIVE)
                        viewModel.injectSpeed(80f)
                    })
                    CarButton("方向盘 PTT", onClick = { viewModel.triggerPtt() })
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CarButton("STR 休眠", onClick = { viewModel.injectPower(PowerState.STR_SLEEP) })
                    CarButton("唤醒运行", onClick = { viewModel.injectPower(PowerState.RUNNING) })
                }
            }

            // 右侧：车规防分心视频与防盲打面板
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                CarCard(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = if (uxRestriction.noVideo) Color(0xFF211111) else MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (uxRestriction.noVideo) {
                            Text("🛑 行车视频安全拦截生效", color = Color.Red, fontSize = 22.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("行车安全策略：移动状态禁止播放影音", color = Color.Gray, fontSize = 14.sp)
                        } else {
                            Text("🎬 乘客影音系统 (P档独享)", color = Color.Green, fontSize = 22.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("允许操作复杂多媒体与设置", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
