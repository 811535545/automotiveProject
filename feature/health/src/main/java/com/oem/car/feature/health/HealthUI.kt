package com.oem.car.feature.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.oem.car.core.model.HealthSeverity
import com.oem.car.core.model.TirePressure
import com.oem.car.core.model.VehicleHealthState

@Composable
fun HealthScreen(
    repository: VehicleRepository = VehicleRepositoryImpl
) {
    val health by repository.vehicleHealthState.collectAsState()

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
                Text("🩺 车辆健康与诊断", fontSize = 18.sp, color = Color.Gray)
                Text("仿真模式：无真实 UDS / DoIP 链路", fontSize = 12.sp, color = Color(0xFFFFB74D))
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1.05f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CarCard(modifier = Modifier.fillMaxWidth()) {
                        Text("胎压监测 (bar)", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TireItem(health.tires[0], Modifier.weight(1f))
                            TireItem(health.tires[1], Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TireItem(health.tires[2], Modifier.weight(1f))
                            TireItem(health.tires[3], Modifier.weight(1f))
                        }
                    }

                    CarCard(modifier = Modifier.fillMaxWidth()) {
                        Text("低压电源与保养", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            HealthStat("12V 蓄电池", "%.1f V".format(health.battery12vVoltage))
                            HealthStat("总里程", "${health.odometerKm} km")
                            HealthStat(
                                "距保养",
                                "${(health.serviceDueKm - health.odometerKm).coerceAtLeast(0)} km"
                            )
                        }
                    }
                }

                CarCard(modifier = Modifier.weight(1f)) {
                    Text("系统自检", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    health.systems.forEach { system ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(system.name, fontSize = 14.sp)
                            Text(
                                "${system.severity.display} · ${system.detail}",
                                fontSize = 13.sp,
                                color = severityColor(system.severity)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CarButton(
                            text = if (health.isScanning) "扫描中..." else "扫描故障码",
                            onClick = repository::scanVehicleHealth,
                            enabled = !health.isScanning,
                            modifier = Modifier.weight(1f)
                        )
                        CarButton(
                            text = "清除故障码",
                            onClick = repository::clearDtcs,
                            enabled = health.dtcs.isNotEmpty() && !health.isScanning,
                            modifier = Modifier.weight(1f),
                            containerColor = Color(0xFF37474F)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(health.lastScanResult, fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (health.dtcs.isEmpty()) {
                        Text("当前无存储故障码", fontSize = 13.sp, color = Color(0xFF00E676))
                    } else {
                        health.dtcs.forEach { dtc ->
                            Text(
                                "${dtc.code} · ${dtc.description}",
                                fontSize = 13.sp,
                                color = severityColor(dtc.severity)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "演示说明：扫描 1.5 秒后返回模拟 DTC，可手动清除。",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun TireItem(tire: TirePressure, modifier: Modifier = Modifier) {
    val color = when {
        tire.bar < 1.8f -> Color(0xFFFF5252)
        tire.bar < 2.2f -> Color(0xFFFFB74D)
        else -> Color(0xFF00E676)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF111C2A)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(tire.position, fontSize = 13.sp, color = Color.Gray)
            Text("%.1f bar".format(tire.bar), fontSize = 22.sp, color = color)
            Text(if (tire.bar < 2.2f) "偏低" else "正常", fontSize = 12.sp, color = color)
        }
    }
}

@Composable
private fun HealthStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 16.sp, color = Color.White)
    }
}

private fun severityColor(severity: HealthSeverity): Color = when (severity) {
    HealthSeverity.NORMAL -> Color(0xFF00E676)
    HealthSeverity.WARNING -> Color(0xFFFFB74D)
    HealthSeverity.FAULT -> Color(0xFFFF5252)
}
