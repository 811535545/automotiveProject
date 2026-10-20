package com.oem.car.feature.navigation

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oem.car.core.data.VehicleRepository
import com.oem.car.core.data.VehicleRepositoryImpl
import com.oem.car.core.designsystem.CarButton
import com.oem.car.core.designsystem.CarCard
import com.oem.car.core.designsystem.CarTheme
import com.oem.car.core.model.NavigationState
import com.oem.car.core.model.RouteStatus

@Composable
fun NavigationScreen(
    repository: VehicleRepository = VehicleRepositoryImpl
) {
    val navigation by repository.navigationState.collectAsState()

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
                Text("🧭 导航与路线规划", fontSize = 18.sp, color = Color.Gray)
                Text("仿真地图：未接入真实地图引擎", fontSize = 12.sp, color = Color(0xFFFFB74D))
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1.25f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MapPlaceholder(
                        navigation = navigation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    )
                    CarCard(modifier = Modifier.fillMaxWidth()) {
                        Text("路线信息", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(navigation.nextInstruction, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            InfoItem("转向距离", "${navigation.distanceToTurnMeters} m")
                            InfoItem("剩余里程", "%.1f km".format(navigation.remainingKm))
                            InfoItem("预计用时", "${navigation.etaMinutes} min")
                            InfoItem("道路限速", "${navigation.speedLimitKph} km/h")
                        }
                        if (navigation.laneHint.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("车道提示：${navigation.laneHint}", fontSize = 13.sp, color = Color(0xFF00E676))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { navigation.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )
                    }
                }

                CarCard(modifier = Modifier.weight(1f)) {
                    Text("目的地（预设 POI）", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    repository.navigationPresets.forEach { destination ->
                        CarButton(
                            text = "导航到 ${destination.name}",
                            onClick = { repository.startNavigation(destination) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "${destination.address} · ${destination.distanceKm} km · 约 ${destination.etaMinutes} 分钟",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CarButton(
                            text = "下一段",
                            onClick = repository::advanceNavigation,
                            enabled = navigation.status == RouteStatus.NAVIGATING,
                            modifier = Modifier.weight(1f)
                        )
                        CarButton(
                            text = "结束导航",
                            onClick = repository::stopNavigation,
                            enabled = navigation.status != RouteStatus.IDLE,
                            modifier = Modifier.weight(1f),
                            containerColor = Color(0xFF37474F)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("当前状态：${navigation.status.display}", fontSize = 13.sp, color = Color.Gray)
                    if (navigation.destination != null) {
                        Text(
                            "目的地：${navigation.destination!!.name}",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapPlaceholder(navigation: NavigationState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0B1726))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val road = Color(0xFF1E3A52)
            drawLine(
                color = road,
                start = Offset(0f, size.height * 0.75f),
                end = Offset(size.width, size.height * 0.35f),
                strokeWidth = 30f
            )
            drawLine(
                color = road,
                start = Offset(size.width * 0.32f, 0f),
                end = Offset(size.width * 0.55f, size.height),
                strokeWidth = 20f
            )
            drawLine(
                color = Color(0xFF142A3E),
                start = Offset(0f, size.height * 0.25f),
                end = Offset(size.width, size.height * 0.9f),
                strokeWidth = 14f
            )
            if (navigation.status != RouteStatus.IDLE) {
                drawLine(
                    color = Color(0xFF00E5FF),
                    start = Offset(size.width * 0.18f, size.height * 0.82f),
                    end = Offset(size.width * 0.82f, size.height * 0.22f),
                    strokeWidth = 8f
                )
                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = 10f,
                    center = Offset(size.width * 0.82f, size.height * 0.22f)
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text("仿真地图", fontSize = 13.sp, color = Color(0xFF00E5FF))
            Text(
                navigation.destination?.let { "目的地：${it.name}" } ?: "未设置目的地",
                fontSize = 16.sp,
                color = Color.White
            )
        }
        Text(
            "演示轨迹 · 无真实地图 SDK",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        )
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 16.sp, color = Color.White)
    }
}
