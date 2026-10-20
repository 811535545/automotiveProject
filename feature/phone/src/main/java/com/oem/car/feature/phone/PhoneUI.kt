package com.oem.car.feature.phone

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
import com.oem.car.core.model.CallStatus
import com.oem.car.core.model.PhoneState

@Composable
fun PhoneScreen(
    repository: VehicleRepository = VehicleRepositoryImpl
) {
    val phone by repository.phoneState.collectAsState()

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
                Text("📞 蓝牙电话与通讯", fontSize = 18.sp, color = Color.Gray)
                Text("仿真模式：无真实 HFP 链路", fontSize = 12.sp, color = Color(0xFFFFB74D))
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CarCard(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("蓝牙连接", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                        CarButton(
                            text = if (phone.isBluetoothConnected) "断开蓝牙" else "连接蓝牙",
                            onClick = repository::togglePhoneBluetooth
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "设备：${if (phone.isBluetoothConnected) phone.deviceName else "未连接"}",
                        fontSize = 16.sp
                    )
                    Text("音频路由：${phone.audioRoute}", fontSize = 13.sp, color = Color.Gray)
                    Text("未接来电：${phone.missedCalls} 条", fontSize = 13.sp, color = Color(0xFFFFB74D))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("联系人", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    phone.contacts.forEach { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(contact.name, fontSize = 15.sp)
                                Text(contact.phone, fontSize = 12.sp, color = Color.Gray)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CarButton(
                                    text = "拨打",
                                    enabled = phone.isBluetoothConnected && phone.callStatus == CallStatus.IDLE,
                                    onClick = { repository.dialContact(contact) }
                                )
                                CarButton(
                                    text = "来电",
                                    enabled = phone.isBluetoothConnected && phone.callStatus == CallStatus.IDLE,
                                    onClick = { repository.simulateIncomingCall(contact) },
                                    containerColor = Color(0xFF455A64)
                                )
                            }
                        }
                    }
                }

                CarCard(modifier = Modifier.weight(1f)) {
                    Text("当前通话", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        phone.callStatus.display,
                        fontSize = 28.sp,
                        color = when (phone.callStatus) {
                            CallStatus.ACTIVE -> Color(0xFF00E676)
                            CallStatus.RINGING -> Color(0xFFFFB74D)
                            CallStatus.DIALING -> Color(0xFF00E5FF)
                            CallStatus.IDLE -> Color.Gray
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        phone.currentContact?.name ?: "暂无通话",
                        fontSize = 20.sp
                    )
                    Text(
                        phone.currentContact?.phone ?: "选择联系人发起通话",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "通话时长：${formatDuration(phone.callDurationSec)}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        "静音：${if (phone.isMuted) "已开启" else "关闭"}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (phone.callStatus == CallStatus.RINGING) {
                            CarButton(
                                text = "接听",
                                onClick = repository::answerCall,
                                modifier = Modifier.weight(1f),
                                containerColor = Color(0xFF2E7D32)
                            )
                        }
                        CarButton(
                            text = "挂断",
                            enabled = phone.callStatus != CallStatus.IDLE,
                            onClick = repository::hangUpCall,
                            modifier = Modifier.weight(1f),
                            containerColor = Color(0xFFB71C1C)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    CarButton(
                        text = if (phone.isMuted) "取消静音" else "静音",
                        enabled = phone.callStatus == CallStatus.ACTIVE,
                        onClick = repository::toggleCallMute,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color(0xFF455A64)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "演示说明：拨号后 1.2 秒自动接通并计时，断开蓝牙会结束通话。",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
