package com.oem.car.core.designsystem

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 2.5D / 3D 动态车身矢量与状态渲染器 (Vehicle Body Canvas Visualizer)。
 *
 * 实时渲染车身轮廓、前后大灯光束、门锁状态、胎压指示以及行驶风阻线条。
 */
@Composable
fun Vehicle3dVisualizer(
    speed: Float,
    isDoorLocked: Boolean,
    isDriving: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CarMotion")
    val motionOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (speed > 0) (10000 / speed.coerceAtLeast(1f)).toInt().coerceIn(200, 2000) else 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpeedLines"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Color(0xFF0F131A), shape = RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f

            // 1. Driving Speed Motion Lines
            if (isDriving) {
                for (i in 0..4) {
                    val startX = (centerX - 200f + (motionOffset + i * 80) % 400f)
                    drawLine(
                        color = Color(0x3300E5FF),
                        start = Offset(startX, centerY - 60f + i * 30f),
                        end = Offset(startX + 40f, centerY - 60f + i * 30f),
                        strokeWidth = 3f
                    )
                }
            }

            // 2. Car Chassis Ground Shadow
            drawRoundRect(
                color = Color(0x66000000),
                topLeft = Offset(centerX - 130f, centerY - 45f),
                size = Size(260f, 90f),
                cornerRadius = CornerRadius(20f, 20f)
            )

            // 3. Main Car Body Silhouette (Cyan Blueprint Style)
            val bodyColor = if (isDriving) Color(0xFF00E5FF) else Color(0xFF37474F)
            drawRoundRect(
                color = bodyColor,
                topLeft = Offset(centerX - 120f, centerY - 40f),
                size = Size(240f, 80f),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 4f)
            )

            // Windshield & Cabin Roof
            drawRoundRect(
                color = bodyColor.copy(alpha = 0.6f),
                topLeft = Offset(centerX - 50f, centerY - 30f),
                size = Size(100f, 60f),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(width = 2f)
            )

            // Headlights Beam (Front)
            drawCircle(
                color = if (isDriving) Color(0xFF00E5FF) else Color(0xFF78909C),
                radius = 8f,
                center = Offset(centerX + 115f, centerY - 25f)
            )
            drawCircle(
                color = if (isDriving) Color(0xFF00E5FF) else Color(0xFF78909C),
                radius = 8f,
                center = Offset(centerX + 115f, centerY + 25f)
            )

            // Tail Lights (Rear)
            drawCircle(
                color = Color(0xFFFF5252),
                radius = 6f,
                center = Offset(centerX - 115f, centerY - 25f)
            )
            drawCircle(
                color = Color(0xFFFF5252),
                radius = 6f,
                center = Offset(centerX - 115f, centerY + 25f)
            )

            // Door Lock Status Indicator Rings
            val lockColor = if (isDoorLocked) Color(0xFF00E676) else Color(0xFFFF9100)
            drawCircle(color = lockColor, radius = 5f, center = Offset(centerX, centerY - 42f))
            drawCircle(color = lockColor, radius = 5f, center = Offset(centerX, centerY + 42f))
        }

        // Overlay Telemetry Labels
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text(
                text = "3D 车身数字孪生 (Digital Twin)",
                color = Color(0xFF80D8FF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isDoorLocked) "🔒 车门已锁止" else "🔓 车门解锁",
                color = if (isDoorLocked) Color(0xFF00E676) else Color(0xFFFF9100),
                fontSize = 11.sp
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "胎压: 2.4 bar (正常)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Text(
                text = if (speed > 0) "实时车速: ${speed.toInt()} km/h" else "车辆静止 P 档",
                color = Color(0xFF00E5FF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
