package com.oem.car.feature.energy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@Composable
fun EnergyScreen(
    repository: VehicleRepository = VehicleRepositoryImpl
) {
    val energy by repository.energyState.collectAsState()

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
                Text("🔋 能量管理与充电", fontSize = 18.sp, color = Color.Gray)
                Text("仿真模式：无真实 BMS / 充电桩", fontSize = 12.sp, color = Color(0xFFFFB74D))
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CarCard(modifier = Modifier.weight(1.1f)) {
                    Text("电池状态", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { energy.socPercent / 100f },
                                modifier = Modifier.size(168.dp),
                                strokeWidth = 14.dp,
                                trackColor = Color(0xFF1E2A38)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${energy.socPercent}%",
                                    fontSize = 40.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("SOC", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.width(24.dp))
                        Column {
                            Text("续航 ${energy.rangeKm} km", fontSize = 22.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (energy.isCharging) "充电中 · ${energy.chargingPowerKw.toInt()} kW" else "未充电",
                                fontSize = 14.sp,
                                color = if (energy.isCharging) Color(0xFF00E676) else Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("电池温度：${energy.batteryTempC.toInt()} °C", fontSize = 13.sp, color = Color.Gray)
                            Text("平均能耗：${energy.consumptionKwh100Km} kWh/100km", fontSize = 13.sp, color = Color.Gray)
                            Text("充电桩：${energy.chargerType}", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { energy.socPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("充电限值 ${energy.chargeLimitPercent}%", fontSize = 12.sp, color = Color.Gray)
                }

                CarCard(modifier = Modifier.weight(1f)) {
                    Text("充电控制", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("充电限值", fontSize = 14.sp)
                        Text("${energy.chargeLimitPercent}%", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CarButton(
                            text = "限值 -5%",
                            onClick = { repository.adjustChargeLimit(-5) },
                            modifier = Modifier.weight(1f)
                        )
                        CarButton(
                            text = "限值 +5%",
                            onClick = { repository.adjustChargeLimit(5) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    CarButton(
                        text = if (energy.isCharging) "停止充电" else "开始充电",
                        onClick = repository::toggleCharging,
                        enabled = energy.isCharging || energy.socPercent < energy.chargeLimitPercent,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = if (energy.isCharging) Color(0xFFB71C1C) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CarButton(
                        text = if (energy.isScheduledCharging) "关闭预约充电 (${energy.scheduledTime})" else "开启预约充电 (${energy.scheduledTime})",
                        onClick = repository::toggleScheduledCharging,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color(0xFF455A64)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CarButton(
                        text = if (energy.isPreconditioning) "关闭电池预热" else "开启电池预热",
                        onClick = repository::togglePreconditioning,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color(0xFF455A64)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "演示说明：充电时 SOC 每 0.8 秒 +1%，达到限值自动停止。",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
