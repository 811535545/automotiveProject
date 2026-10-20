package com.oem.car.feature.climate

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
fun ClimateScreen(
    repository: VehicleRepository = VehicleRepositoryImpl
) {
    val climateState by repository.climateState.collectAsState()

    CarTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("❄️ 座舱舒适性与双区 HVAC 空调控制", fontSize = 20.sp, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // 主驾温度
                CarCard(modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("主驾温度", fontSize = 14.sp, color = Color.Gray)
                        Text("${climateState.driverTemp} °C", fontSize = 32.sp, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CarButton("-", onClick = {
                                repository.updateClimate { it.copy(driverTemp = it.driverTemp - 0.5f) }
                            })
                            CarButton("+", onClick = {
                                repository.updateClimate { it.copy(driverTemp = it.driverTemp + 0.5f) }
                            })
                        }
                    }
                }

                // 副驾温度
                CarCard(modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("副驾温度", fontSize = 14.sp, color = Color.Gray)
                        Text("${climateState.passengerTemp} °C", fontSize = 32.sp, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CarButton("-", onClick = {
                                repository.updateClimate { it.copy(passengerTemp = it.passengerTemp - 0.5f) }
                            })
                            CarButton("+", onClick = {
                                repository.updateClimate { it.copy(passengerTemp = it.passengerTemp + 0.5f) }
                            })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CarCard(modifier = Modifier.fillMaxWidth(0.8f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("风量: ${climateState.fanSpeed} 挡", fontSize = 16.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CarButton("降低", onClick = {
                            if (climateState.fanSpeed > 1) {
                                repository.updateClimate { it.copy(fanSpeed = it.fanSpeed - 1) }
                            }
                        })
                        CarButton("提高", onClick = {
                            if (climateState.fanSpeed < 7) {
                                repository.updateClimate { it.copy(fanSpeed = it.fanSpeed + 1) }
                            }
                        })
                    }
                }
            }
        }
    }
}
