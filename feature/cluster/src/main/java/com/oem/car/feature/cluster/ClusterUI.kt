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

@Composable
fun ClusterScreen(
    repository: VehicleRepository = VehicleRepositoryImpl
) {
    val speedSignal by repository.speedSignal.collectAsState()
    val gearSignal by repository.gearSignal.collectAsState()

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

                SpeedGaugeComponent(
                    speed = speedSignal.value,
                    gearDisplay = gearSignal.value.display
                )

                Spacer(modifier = Modifier.height(24.dp))

                CarCard(
                    modifier = Modifier.width(360.dp),
                    containerColor = Color(0xFF101B2B)
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("📍 仪表 TBT 导航卡片", color = Color(0xFF00E5FF), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("前方 200m 右转进入 科技大道", color = Color.White, fontSize = 18.sp)
                        Text("辅助驾驶 (ADAS): 保持车道中 (80km/h 巡航)", color = Color.Green, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
