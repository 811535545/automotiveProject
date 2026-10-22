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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oem.car.core.model.AdasPerceptionState

/**
 * 车规级 ADAS 智驾 SR (Surrounding Reality) 3D 感知渲染画卷。
 *
 * 功能点：
 * 1. 透视投影网格与流动车道线 (3D Perspective Road Lanes)
 * 2. NOA 领航规划发光轨迹廊道 (NOA Planned Path Ribbon)
 * 3. 自车 (Ego Vehicle) 矢量模型与状态光环
 * 4. 周边感知目标 3D 渲染 (前车 Sedan、左/右侧卡车 Truck、行人 Pedestrian、锥桶 Cone)
 */
@Composable
fun AdasSrVisualizer(
    adasState: AdasPerceptionState,
    speed: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SrMotion")
    val laneOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (speed > 0) (8000 / speed.coerceAtLeast(10f)).toInt().coerceIn(150, 1500) else 4000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "RoadGridMotion"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color(0xFF03070E), shape = RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val horizonY = height * 0.25f // 远方消失点高度
            val egoX = width / 2f
            val egoY = height * 0.82f   // 自车画面中心

            // 1. 背景星空与地平线渐变 (Horizon Sky Gradient)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1324), Color(0xFF03070E)),
                    startY = 0f,
                    endY = horizonY
                ),
                size = Size(width, horizonY)
            )

            // 地平线发光细线条
            drawLine(
                color = Color(0x3300E5FF),
                start = Offset(0f, horizonY),
                end = Offset(width, horizonY),
                strokeWidth = 2f
            )

            // 2. 3D 车道线与透视收拢效果 (Perspective Lanes)
            val leftLaneStart = Offset(egoX - width * 0.15f, horizonY)
            val leftLaneEnd = Offset(egoX - width * 0.38f, height)
            val rightLaneStart = Offset(egoX + width * 0.15f, horizonY)
            val rightLaneEnd = Offset(egoX + width * 0.38f, height)

            // 左右主车道线
            drawLine(
                color = if (adasState.laneKeepActive) Color(0xFF00E5FF) else Color(0xFF546E7A),
                start = leftLaneStart,
                end = leftLaneEnd,
                strokeWidth = 4f
            )
            drawLine(
                color = if (adasState.laneKeepActive) Color(0xFF00E5FF) else Color(0xFF546E7A),
                start = rightLaneStart,
                end = rightLaneEnd,
                strokeWidth = 4f
            )

            // 3. NOA 领航规划发光轨迹廊道 (NOA Planned Ribbon)
            if (adasState.isNoaActive) {
                val path = Path().apply {
                    moveTo(leftLaneStart.x + 20f, leftLaneStart.y)
                    lineTo(rightLaneStart.x - 20f, rightLaneStart.y)
                    lineTo(rightLaneEnd.x - 40f, rightLaneEnd.y)
                    lineTo(leftLaneEnd.x + 40f, leftLaneEnd.y)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x0000E5FF), Color(0x3300E5FF), Color(0x6600E5FF)),
                        startY = horizonY,
                        endY = height
                    )
                )
            }

            // 4. 行驶后退横向动感栅格线 (Moving Road Mesh)
            for (i in 0..5) {
                val progress = ((laneOffset + i * 20f) % 100f) / 100f
                val lineY = horizonY + (height - horizonY) * progress
                val scale = progress * progress
                val currentWidth = width * (0.3f + 0.5f * scale)

                drawLine(
                    color = Color(0x2200E5FF),
                    start = Offset(egoX - currentWidth / 2f, lineY),
                    end = Offset(egoX + currentWidth / 2f, lineY),
                    strokeWidth = 2f
                )
            }

            // 5. 周边感知目标 3D 渲染 (Perception Targets)
            adasState.targets.forEach { target ->
                // 透视坐标转换 (Perspective Projection 3D -> 2D)
                val distanceRatio = (target.distanceX / 60.0f).coerceIn(0.1f, 1.0f)
                val targetY = horizonY + (egoY - horizonY) * (1.0f - distanceRatio)
                val lateralOffset = (target.distanceY / 10.0f) * (width * 0.35f)
                val targetX = egoX + lateralOffset

                val targetScale = (1.0f - distanceRatio * 0.6f).coerceIn(0.3f, 1.0f)
                val boxWidth = 50f * targetScale
                val boxHeight = 35f * targetScale

                when {
                    target.type.contains("Sedan") || target.type.contains("前车") -> {
                        // 轿车：青蓝色 3D 框
                        drawRoundRect(
                            color = Color(0xFF00E5FF),
                            topLeft = Offset(targetX - boxWidth / 2f, targetY - boxHeight / 2f),
                            size = Size(boxWidth, boxHeight),
                            cornerRadius = CornerRadius(6f, 6f),
                            style = Stroke(width = 3f)
                        )
                        drawCircle(
                            color = Color(0xFFFF5252),
                            radius = 3f * targetScale,
                            center = Offset(targetX - boxWidth * 0.3f, targetY + boxHeight * 0.3f)
                        )
                        drawCircle(
                            color = Color(0xFFFF5252),
                            radius = 3f * targetScale,
                            center = Offset(targetX + boxWidth * 0.3f, targetY + boxHeight * 0.3f)
                        )
                    }
                    target.type.contains("Truck") || target.type.contains("卡车") -> {
                        // 卡车：较宽橙色 3D 框
                        val truckWidth = boxWidth * 1.4f
                        val truckHeight = boxHeight * 1.6f
                        drawRoundRect(
                            color = Color(0xFFFF9100),
                            topLeft = Offset(targetX - truckWidth / 2f, targetY - truckHeight / 2f),
                            size = Size(truckWidth, truckHeight),
                            cornerRadius = CornerRadius(8f, 8f),
                            style = Stroke(width = 3f)
                        )
                    }
                    target.type.contains("Pedestrian") || target.type.contains("行人") -> {
                        // 行人：黄色预警圆圈
                        drawCircle(
                            color = Color(0xFFFFEA00),
                            radius = 12f * targetScale,
                            center = Offset(targetX, targetY),
                            style = Stroke(width = 3f)
                        )
                    }
                    else -> {
                        // 锥桶/障碍物：红色警示圆点
                        drawCircle(
                            color = Color(0xFFFF1744),
                            radius = 8f * targetScale,
                            center = Offset(targetX, targetY)
                        )
                    }
                }
            }

            // 6. 自车 (Ego Vehicle) 矢量图形
            val egoWidth = 54f
            val egoHeight = 72f

            // 自车车底发光光环
            drawCircle(
                color = Color(0x6600E5FF),
                radius = 45f,
                center = Offset(egoX, egoY)
            )

            // 自车轮廓
            drawRoundRect(
                color = Color(0xFF00E5FF),
                topLeft = Offset(egoX - egoWidth / 2f, egoY - egoHeight / 2f),
                size = Size(egoWidth, egoHeight),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // 自车车顶车窗
            drawRoundRect(
                color = Color(0xFF0A1324),
                topLeft = Offset(egoX - egoWidth * 0.35f, egoY - egoHeight * 0.3f),
                size = Size(egoWidth * 0.7f, egoHeight * 0.5f),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // 尾灯
            drawCircle(color = Color(0xFFFF1744), radius = 4f, center = Offset(egoX - 18f, egoY + 30f))
            drawCircle(color = Color(0xFFFF1744), radius = 4f, center = Offset(egoX + 18f, egoY + 30f))
        }

        // Overlay ADAS Status Bar
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (adasState.isNoaActive) "🌐 NOA 领航已激活" else "🛣️ 手动驾驶中",
                color = if (adasState.isNoaActive) Color(0xFF00E5FF) else Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "设定巡航: ${adasState.cruiseSpeed.toInt()} km/h",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "SR 3D 感知目标: ${adasState.targets.size} 个",
                color = Color(0xFF80D8FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
