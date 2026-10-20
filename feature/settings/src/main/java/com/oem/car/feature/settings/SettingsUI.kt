package com.oem.car.feature.settings

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
import com.oem.car.core.designsystem.CarButton
import com.oem.car.core.designsystem.CarCard
import com.oem.car.core.designsystem.CarTheme
import com.oem.car.core.model.DriveMode

@Composable
fun SettingsScreen(
    repository: VehicleRepository = VehicleRepositoryImpl
) {
    val driveMode by repository.driveMode.collectAsState()

    CarTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚙️ 车辆控制与个性化设置", fontSize = 20.sp, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))

            CarCard(modifier = Modifier.fillMaxWidth(0.9f)) {
                Text("驾驶模式切换 (Drive Mode)", fontSize = 16.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DriveMode.entries.forEach { mode ->
                        val isSelected = driveMode == mode
                        CarButton(
                            text = mode.display,
                            onClick = { repository.setDriveMode(mode) },
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CarCard(modifier = Modifier.fillMaxWidth(0.9f)) {
                Column {
                    Text("智能驾驶与安全辅助 (ADAS)", fontSize = 16.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("✓ 车道偏离预警 (LDW): 开启", fontSize = 14.sp)
                    Text("✓ 自动紧急制动 (AEB): 开启 (ASIL-D 级别)", fontSize = 14.sp, color = Color.Green)
                    Text("✓ 盲区监测 (BSD): 开启", fontSize = 14.sp)
                }
            }
        }
    }
}
