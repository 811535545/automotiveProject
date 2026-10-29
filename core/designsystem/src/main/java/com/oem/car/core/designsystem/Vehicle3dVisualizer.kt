package com.oem.car.core.designsystem

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * 3D 车身数字孪生状态模型 (Vehicle 3D Digital Twin State)。
 * 支持前后左右 4 大灯、4 车门开关状态控制。
 */
data class Vehicle3dState(
    val speed: Float = 0f,
    val isDriving: Boolean = false,
    val tirePressure: Float = 2.5f,
    // 4 大灯 / 尾灯
    val isFrontLeftLightOn: Boolean = true,
    val isFrontRightLightOn: Boolean = true,
    val isRearLeftLightOn: Boolean = true,
    val isRearRightLightOn: Boolean = true,
    // 4 车门 (FL: Front Left, FR: Front Right, RL: Rear Left, RR: Rear Right)
    val isFrontLeftDoorOpen: Boolean = false,
    val isFrontRightDoorOpen: Boolean = false,
    val isRearLeftDoorOpen: Boolean = false,
    val isRearRightDoorOpen: Boolean = false
) {
    val isAnyDoorOpen: Boolean
        get() = isFrontLeftDoorOpen || isFrontRightDoorOpen || isRearLeftDoorOpen || isRearRightDoorOpen

    val isAllLightsOn: Boolean
        get() = isFrontLeftLightOn && isFrontRightLightOn && isRearLeftLightOn && isRearRightLightOn

    val isDoorLocked: Boolean
        get() = !isAnyDoorOpen && !isDriving
}

/**
 * 2.5D / 3D 动态车身矢量与状态渲染器 (Vehicle Body Canvas Visualizer)。
 *
 * 实时渲染车身轮廓、前后大灯光束、4 车门开合状态、胎压指示以及行驶风阻线条。
 * 包含底部前后左右大灯控制、4车门控制与 StateFlow 测试按钮组件。
 */
@Composable
fun Vehicle3dVisualizer(
    speed: Float,
    isDoorLocked: Boolean,
    isDriving: Boolean,
    modifier: Modifier = Modifier,
    tirePressure: Float = 2.5f,
    titleText: String = "3D 车身数字孪生 (Digital Twin)",
    gearText: String? = null,
    showControls: Boolean = true,
    onStateChange: ((Vehicle3dState) -> Unit)? = null
) {
    var state by remember(speed, isDriving, isDoorLocked, tirePressure) {
        mutableStateOf(
            Vehicle3dState(
                speed = speed,
                isDriving = isDriving,
                tirePressure = tirePressure,
                isFrontLeftDoorOpen = !isDoorLocked,
                isFrontRightDoorOpen = false,
                isRearLeftDoorOpen = false,
                isRearRightDoorOpen = false
            )
        )
    }

    Vehicle3dVisualizerContent(
        state = state,
        modifier = modifier,
        titleText = titleText,
        gearText = gearText,
        showControls = showControls,
        onStateUpdated = { newState ->
            state = newState
            onStateChange?.invoke(newState)
        }
    )
}

/**
 * 接受完整 [Vehicle3dState] 状态模型的 Composable 重载接口。
 */
@Composable
fun Vehicle3dVisualizerContent(
    state: Vehicle3dState,
    modifier: Modifier = Modifier,
    titleText: String = "3D 车身数字孪生 (Digital Twin)",
    gearText: String? = null,
    showControls: Boolean = true,
    onStateUpdated: (Vehicle3dState) -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CarMotion")
    val motionOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state.speed > 0) (10000 / state.speed.coerceAtLeast(1f)).toInt()
                    .coerceIn(200, 2000) else 5000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpeedLines"
    )

    Column(
        modifier = modifier
            .testTag("vehicle_3d_container")
            .fillMaxWidth()
    ) {
        // --- 1. 3D Canvas 渲染视口 ---
        Box(
            modifier = Modifier
                .testTag("vehicle_3d_root")
                .fillMaxWidth()
                .height(220.dp)
                .background(Color(0xFF0F131A), shape = RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .testTag("vehicle_3d_canvas")
                    .fillMaxSize()
            ) {
                val width = size.width
                val height = size.height
                val centerX = width / 2f
                val centerY = height / 2f

                // 1. 行驶风阻线条 (Driving Motion Lines)
                if (state.isDriving) {
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

                // 2. 底盘地面阴影 (Chassis Shadow)
                drawRoundRect(
                    color = Color(0x66000000),
                    topLeft = Offset(centerX - 130f, centerY - 45f),
                    size = Size(260f, 90f),
                    cornerRadius = CornerRadius(20f, 20f)
                )

                // 3. 车身主体轮廓 (Car Silhouette Blueprint)
                val bodyColor = if (state.isDriving) Color(0xFF00E5FF) else Color(0xFF37474F)
                drawRoundRect(
                    color = bodyColor,
                    topLeft = Offset(centerX - 120f, centerY - 40f),
                    size = Size(240f, 80f),
                    cornerRadius = CornerRadius(24f, 24f),
                    style = Stroke(width = 4f)
                )

                // 驾驶舱顶棚/挡风玻璃 (Cabin Roof)
                drawRoundRect(
                    color = bodyColor.copy(alpha = 0.6f),
                    topLeft = Offset(centerX - 50f, centerY - 30f),
                    size = Size(100f, 60f),
                    cornerRadius = CornerRadius(12f, 12f),
                    style = Stroke(width = 2f)
                )

                // --- 4. 前大灯光束 (Front Headlights Beam) ---
                val frontLeftLightColor = if (state.isFrontLeftLightOn) Color(0xFF00E5FF) else Color(0xFF455A64)
                val frontRightLightColor = if (state.isFrontRightLightOn) Color(0xFF00E5FF) else Color(0xFF455A64)

                // 前左大灯 + 光束锥体
                if (state.isFrontLeftLightOn) {
                    val path = Path().apply {
                        moveTo(centerX + 115f, centerY - 25f)
                        lineTo(centerX + 180f, centerY - 50f)
                        lineTo(centerX + 180f, centerY - 10f)
                        close()
                    }
                    drawPath(path = path, color = Color(0x3300E5FF))
                }
                drawCircle(color = frontLeftLightColor, radius = 8f, center = Offset(centerX + 115f, centerY - 25f))

                // 前右大灯 + 光束锥体
                if (state.isFrontRightLightOn) {
                    val path = Path().apply {
                        moveTo(centerX + 115f, centerY + 25f)
                        lineTo(centerX + 180f, centerY + 10f)
                        lineTo(centerX + 180f, centerY + 50f)
                        close()
                    }
                    drawPath(path = path, color = Color(0x3300E5FF))
                }
                drawCircle(color = frontRightLightColor, radius = 8f, center = Offset(centerX + 115f, centerY + 25f))

                // --- 5. 后尾灯 (Rear Tail Lights) ---
                val rearLeftLightColor = if (state.isRearLeftLightOn) Color(0xFFFF5252) else Color(0xFF455A64)
                val rearRightLightColor = if (state.isRearRightLightOn) Color(0xFFFF5252) else Color(0xFF455A64)

                drawCircle(color = rearLeftLightColor, radius = 7f, center = Offset(centerX - 115f, centerY - 25f))
                if (state.isRearLeftLightOn) {
                    drawCircle(color = Color(0x66FF5252), radius = 12f, center = Offset(centerX - 115f, centerY - 25f))
                }

                drawCircle(color = rearRightLightColor, radius = 7f, center = Offset(centerX - 115f, centerY + 25f))
                if (state.isRearRightLightOn) {
                    drawCircle(color = Color(0x66FF5252), radius = 12f, center = Offset(centerX - 115f, centerY + 25f))
                }

                // --- 6. 4 车门开合状态渲染 (4 Doors Status) ---
                // 左前门 (FL)
                if (state.isFrontLeftDoorOpen) {
                    drawLine(
                        color = Color(0xFFFF9100),
                        start = Offset(centerX + 20f, centerY - 40f),
                        end = Offset(centerX + 35f, centerY - 62f),
                        strokeWidth = 5f
                    )
                }
                // 右前门 (FR)
                if (state.isFrontRightDoorOpen) {
                    drawLine(
                        color = Color(0xFFFF9100),
                        start = Offset(centerX + 20f, centerY + 40f),
                        end = Offset(centerX + 35f, centerY + 62f),
                        strokeWidth = 5f
                    )
                }
                // 左后门 (RL)
                if (state.isRearLeftDoorOpen) {
                    drawLine(
                        color = Color(0xFFFF9100),
                        start = Offset(centerX - 40f, centerY - 40f),
                        end = Offset(centerX - 25f, centerY - 62f),
                        strokeWidth = 5f
                    )
                }
                // 右后门 (RR)
                if (state.isRearRightDoorOpen) {
                    drawLine(
                        color = Color(0xFFFF9100),
                        start = Offset(centerX - 40f, centerY + 40f),
                        end = Offset(centerX - 25f, centerY + 62f),
                        strokeWidth = 5f
                    )
                }
            }

            // 上方信息标签 (Top Panel)
            Column(
                modifier = Modifier
                    .testTag("vehicle_3d_top_panel")
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = titleText,
                    color = Color(0xFF80D8FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("vehicle_3d_title")
                )
                Text(
                    text = if (state.isAnyDoorOpen) "⚠️ 车门开启中" else if (state.isDoorLocked) "🔒 车门全锁止" else "🔓 车门解锁",
                    color = if (state.isAnyDoorOpen) Color(0xFFFF9100) else if (state.isDoorLocked) Color(0xFF00E676) else Color(0xFFFFB74D),
                    fontSize = 11.sp,
                    modifier = Modifier.testTag("vehicle_3d_lock_status")
                )
            }

            // 底部胎压/车速信息 (Bottom Telemetry Panel)
            Column(
                modifier = Modifier
                    .testTag("vehicle_3d_bottom_panel")
                    .align(Alignment.BottomCenter)
                    .background(
                        color = CommonColors.Black60,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "胎压: ${String.format(Locale.US, "%.1f", state.tirePressure)} bar (正常)",
                    color = CommonColors.White,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .testTag("vehicle_3d_tire_pressure")
                        .padding(bottom = 2.dp)
                )

                val displayGearSpeed = gearText
                    ?: if (state.speed > 0) "实时车速: ${state.speed.toInt()} km/h" else "车辆静止 P 档"
                Text(
                    text = displayGearSpeed,
                    color = Color(0xFF00E5FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("vehicle_3d_speed_gear")
                )
            }
        }

        // --- 2. 底部控制控制台 (Headlights, 4 Doors & StateFlow Controls Panel) ---
        if (showControls) {
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vehicle_3d_controls_card"),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF141820),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Text(
                        text = "🎛️ 车身灯光与4车门 StateFlow 测试控制台",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 1: 前后左右 4 大灯开关
                    Text("💡 车灯控制 (前左 / 前右 / 后左 / 后右):", fontSize = 11.sp, color = Color.Gray)


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ControlChip(
                            label = "前左灯",
                            isOn = state.isFrontLeftLightOn,
                            onToggle = { onStateUpdated(state.copy(isFrontLeftLightOn = !state.isFrontLeftLightOn)) },
                            modifier = Modifier.weight(1f)
                        )
                        ControlChip(
                            label = "前右灯",
                            isOn = state.isFrontRightLightOn,
                            onToggle = { onStateUpdated(state.copy(isFrontRightLightOn = !state.isFrontRightLightOn)) },
                            modifier = Modifier.weight(1f)
                        )
                        ControlChip(
                            label = "后左尾灯",
                            isOn = state.isRearLeftLightOn,
                            onToggle = { onStateUpdated(state.copy(isRearLeftLightOn = !state.isRearLeftLightOn)) },
                            modifier = Modifier.weight(1f)
                        )
                        ControlChip(
                            label = "后右尾灯",
                            isOn = state.isRearRightLightOn,
                            onToggle = { onStateUpdated(state.copy(isRearRightLightOn = !state.isRearRightLightOn)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: 前排/后排 4 车门开关
                    Text("🚪 车门控制 (左前 / 右前 / 左后 / 右后):", fontSize = 11.sp, color = Color.Gray)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ControlChip(
                            label = "左前门",
                            isOn = state.isFrontLeftDoorOpen,
                            activeColor = Color(0xFFFF9100),
                            onToggle = { onStateUpdated(state.copy(isFrontLeftDoorOpen = !state.isFrontLeftDoorOpen)) },
                            modifier = Modifier.weight(1f)
                        )
                        ControlChip(
                            label = "右前门",
                            isOn = state.isFrontRightDoorOpen,
                            activeColor = Color(0xFFFF9100),
                            onToggle = { onStateUpdated(state.copy(isFrontRightDoorOpen = !state.isFrontRightDoorOpen)) },
                            modifier = Modifier.weight(1f)
                        )
                        ControlChip(
                            label = "左后门",
                            isOn = state.isRearLeftDoorOpen,
                            activeColor = Color(0xFFFF9100),
                            onToggle = { onStateUpdated(state.copy(isRearLeftDoorOpen = !state.isRearLeftDoorOpen)) },
                            modifier = Modifier.weight(1f)
                        )
                        ControlChip(
                            label = "右后门",
                            isOn = state.isRearRightDoorOpen,
                            activeColor = Color(0xFFFF9100),
                            onToggle = { onStateUpdated(state.copy(isRearRightDoorOpen = !state.isRearRightDoorOpen)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 3: StateFlow 一键控制测试按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val allLightsOn = !state.isAllLightsOn
                                onStateUpdated(
                                    state.copy(
                                        isFrontLeftLightOn = allLightsOn,
                                        isFrontRightLightOn = allLightsOn,
                                        isRearLeftLightOn = allLightsOn,
                                        isRearRightLightOn = allLightsOn
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(36.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B8D4)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (state.isAllLightsOn) "💡 关全车灯" else "💡 开全车灯", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val anyOpen = state.isAnyDoorOpen
                                onStateUpdated(
                                    state.copy(
                                        isFrontLeftDoorOpen = !anyOpen,
                                        isFrontRightDoorOpen = !anyOpen,
                                        isRearLeftDoorOpen = !anyOpen,
                                        isRearRightDoorOpen = !anyOpen
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(36.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (state.isAnyDoorOpen) "🔒 关全车门" else "🔓 开全车门", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                onStateUpdated(
                                    state.copy(
                                        isFrontLeftDoorOpen = false,
                                        isFrontRightDoorOpen = false,
                                        isRearLeftDoorOpen = false,
                                        isRearRightDoorOpen = false,
                                        isFrontLeftLightOn = false,
                                        isFrontRightLightOn = false,
                                        isRearLeftLightOn = false,
                                        isRearRightLightOn = false
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(36.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("⚡ 一键复位", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 仪表控制台可点按切换按钮 Chip。
 */
@Composable
private fun ControlChip(
    label: String,
    isOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF00E5FF)
) {
    val bgColor = if (isOn) activeColor.copy(alpha = 0.25f) else Color(0xFF1F2633)
    val borderColor = if (isOn) activeColor else Color(0xFF37474F)
    val textColor = if (isOn) activeColor else Color(0xFF90A4AE)
    val statusIcon = if (isOn) "ON" else "OFF"

    Box(
        modifier = modifier
            .height(34.dp)
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$label:$statusIcon",
            fontSize = 10.sp,
            fontWeight = if (isOn) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}
