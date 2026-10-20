package com.oem.car.feature.media

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

@Composable
fun MediaScreen(
    repository: VehicleRepository = VehicleRepositoryImpl
) {
    val uxRestriction by repository.uxRestrictionState.collectAsState()
    var isPlaying by remember { mutableStateOf(false) }

    CarTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            CarCard(modifier = Modifier.fillMaxWidth(0.85f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎵 多媒体与高精影音播放器", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (uxRestriction.noVideo) {
                        Surface(
                            color = Color(0xFF331010),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🚫 行车中已开启视频防分心保护", color = Color.Red, fontSize = 18.sp)
                                    Text("仅保留音频播放，恢复驻车 (P档) 后自动解封", color = Color.Gray, fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = Color(0xFF102A43),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(if (isPlaying) "▶️ 正在播放 4K 座舱演示视频" else "⏸️ 视频暂停中", color = Color.Cyan, fontSize = 18.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CarButton(if (isPlaying) "暂停" else "播放", onClick = { isPlaying = !isPlaying })
                        CarButton("下一首", onClick = {})
                    }
                }
            }
        }
    }
}
