package com.oem.car.feature.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

@Composable
fun VoiceAssistantOverlay(
    repository: VehicleRepository = VehicleRepositoryImpl
) {
    val audioState by repository.audioZoneState.collectAsState()

    AnimatedVisibility(
        visible = audioState.isPttActive,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Color(0xFF1E1035),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFF7C4DFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎙️", fontSize = 32.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("智能语音助手正在倾听...", color = Color.White, fontSize = 20.sp)
                    Text("已成功挂起背景音乐 (Duck Music Focus)", color = Color.Magenta, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    CarButton("完成对话", onClick = { repository.releasePtt() })
                }
            }
        }
    }
}
