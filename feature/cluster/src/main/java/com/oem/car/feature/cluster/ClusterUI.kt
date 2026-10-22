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
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🖥️ 液晶仪表 (Instrument Cluster Display)", color = Color.Gray, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(48.dp)
                ) {
                SpeedGaugeComponent(
                    speedSignal = speedSignal,
                    gearDisplay = gearSignal.value.display
                )

                CarCard(
                    modifier = Modifier.width(360.dp),
                    containerColor = Color(0xFF101B2B)
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("📍 仪表 TBT 导航卡片", color = Color(0xFF00E5FF), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (navigation.status == RouteStatus.NAVIGATING) {
                                navigation.nextInstruction
                            } else {
                                "未开始导航，请在「导航」页设置目的地"
                            },
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Text(
                            if (navigation.status == RouteStatus.NAVIGATING) {
                                "剩余 %.1f km · ETA %d 分钟 · 限速 %d km/h".format(
                                    navigation.remainingKm,
                                    navigation.etaMinutes,
                                    navigation.speedLimitKph
                                )
                            } else {
                                "辅助驾驶: ${if (adas.isNoaActive) "NOA 开启" else "NOA 关闭"} · " +
                                    "${if (adas.laneKeepActive) "车道保持" else "车道关闭"} · ${adas.cruiseSpeed.toInt()} km/h"
                            },
                            color = if (navigation.status == RouteStatus.NAVIGATING) Color(0xFF00E676) else Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
                }
            }
        }
    }
}
