package com.oem.car.feature.cluster

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oem.car.core.data.VehicleRepository
import com.oem.car.core.data.VehicleRepositoryImpl
import com.oem.car.core.designsystem.AdasSrVisualizer
import com.oem.car.core.designsystem.CarCard
import com.oem.car.core.designsystem.CarTheme
import com.oem.car.core.designsystem.SpeedGaugeComponent
import com.oem.car.core.model.RouteStatus

@Composable
fun ClusterScreen(
    repository: VehicleRepository = VehicleRepositoryImpl
) {
    val speedSignal by repository.speedSignal.collectAsState()
    val gearSignal by repository.gearSignal.collectAsState()
    val navigation by repository.navigationState.collectAsState()
    val adas by repository.adasState.collectAsState()

    CarTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050B14))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🖥️ 液晶仪表 (Instrument Cluster SR Display)", color = Color.Gray, fontSize = 14.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. 左侧速度表
                    SpeedGaugeComponent(
                        speedSignal = speedSignal,
                        gearDisplay = gearSignal.value.display
                    )

                    // 2. 中间 ADAS 3D SR 感知画卷 (Surrounding Reality Canvas)
                    AdasSrVisualizer(
                        adasState = adas,
                        speed = speedSignal.value,
                        modifier = Modifier
                            .weight(1f)
                            .height(260.dp)
                    )

                    // 3. 右侧导航 TBT 卡片
                    CarCard(
                        modifier = Modifier.width(240.dp),
                        containerColor = Color(0xFF101B2B)
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("📍 仪表 TBT 导航卡片", color = Color(0xFF00E5FF), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (navigation.status == RouteStatus.NAVIGATING) {
                                    navigation.nextInstruction
                                } else {
                                    "未开始导航，请在「导航」页设置目的地"
                                },
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                if (navigation.status == RouteStatus.NAVIGATING) {
                                    "剩余 %.1f km · ETA %d 分钟".format(
                                        navigation.remainingKm,
                                        navigation.etaMinutes
                                    )
                                } else {
                                    "辅助驾驶: ${if (adas.isNoaActive) "NOA 开启" else "NOA 关闭"}"
                                },
                                color = if (navigation.status == RouteStatus.NAVIGATING) Color(0xFF00E676) else Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
