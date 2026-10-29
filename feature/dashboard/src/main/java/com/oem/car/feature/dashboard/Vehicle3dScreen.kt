package com.oem.car.feature.dashboard

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oem.car.core.designsystem.*
import com.oem.car.core.model.VehicleGear

/**
 * 3D 车身数字孪生专属交互独立页面 (Vehicle 3D Digital Twin Screen)。
 * 智能响应式高度填充：在车机大屏/高屏上打底自动拉伸填充全屏，在手机/小屏上智能切换为可滚动布局。
 */
@Composable
fun Vehicle3dScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val speedSignal by viewModel.speedSignal.collectAsState()
    val gearSignal by viewModel.gearSignal.collectAsState()

    CarTheme {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val needScroll = this.maxHeight < 620.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (needScroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🚘 3D 车身数字孪生与实时姿态 (Digital Twin Console)",
                    fontSize = 20.sp,
                    color = Color(0xFF00E5FF),
                    style = MaterialTheme.typography.titleLarge
                )

                // 3D Visualizer Canvas & Controls - 智能填满高屏幕剩余高度！
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (!needScroll) Modifier.weight(1f) else Modifier)
                ) {
                    Vehicle3dVisualizer(
                        speed = speedSignal.value,
                        isDoorLocked = gearSignal.value == VehicleGear.DRIVE || speedSignal.value > 0f,
                        isDriving = gearSignal.value == VehicleGear.DRIVE && speedSignal.value > 0.5f,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 3D Controls Panel
                CarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "🛠️ 车身姿态与模拟控制台",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CarButton(
                                text = "驻车 P 档 (0 km/h)",
                                onClick = {
                                    viewModel.injectGear(VehicleGear.PARK)
                                    viewModel.injectSpeed(0f)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            CarButton(
                                text = "城市 D 档 (60 km/h)",
                                onClick = {
                                    viewModel.injectGear(VehicleGear.DRIVE)
                                    viewModel.injectSpeed(60f)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            CarButton(
                                text = "巡航 D 档 (120 km/h)",
                                onClick = {
                                    viewModel.injectGear(VehicleGear.DRIVE)
                                    viewModel.injectSpeed(120f)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
