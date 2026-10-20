package com.oem.car.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CarCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderModifier = if (borderColor != null) {
        Modifier.border(1.dp, borderColor, RoundedCornerShape(16.dp))
    } else Modifier

    Surface(
        modifier = modifier.then(borderModifier),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun CarButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp), // Automotive large touch target
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text, fontSize = 16.sp)
    }
}

@Composable
fun SafetyBadge(isRestricted: Boolean, modifier: Modifier = Modifier) {
    val color = if (isRestricted) Color(0xFFFF5252) else Color(0xFF00E676)
    val text = if (isRestricted) "⚠️ 行车防分心策略已启用" else "✅ 安全允许"

    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .border(1.dp, color, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = color, fontSize = 13.sp)
    }
}

@Composable
fun SpeedGaugeComponent(speed: Float, gearDisplay: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${speed.toInt()}",
            fontSize = 64.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "KM/H",
            fontSize = 14.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "档位: $gearDisplay",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 18.sp
            )
        }
    }
}
