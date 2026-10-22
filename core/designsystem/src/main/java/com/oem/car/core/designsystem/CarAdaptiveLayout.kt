package com.oem.car.core.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 车规级自适应座舱布局容器 (Automotive Responsive Cockpit Container)。
 *
 * 智能适配三种典型车机大屏比例：
 * 1. 贯穿式超宽屏 (Ultra-Wide, >2.1 宽高比，如 1920x720 / 2560x960): 三列并行布局 (左: 仪表核心 | 中: 3D/SR画卷 | 右: 交互控制)；
 * 2. 标准横屏屏 (Medium, 16:9/16:10，如 1920x1080 / 1280x800): 左右双列分屏布局；
 * 3. 紧凑/低分辨率屏 (Compact, <850dp 宽度，如 800x480 / 1024x600): 单列垂直可滚动布局。
 */
@Composable
fun CarAdaptiveLayout(
    primaryContent: @Composable () -> Unit,
    secondaryContent: @Composable () -> Unit,
    tertiaryContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    windowSizeClass: CarWindowSizeClass = rememberCarWindowSizeClass()
) {
    when {
        // 1. 超宽屏模式 (例如 1920x720 贯穿屏) -> 三列水平并排，零垂直滚动
        windowSizeClass.isUltraWide -> {
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) { primaryContent() }
                Box(modifier = Modifier.weight(1.2f).fillMaxHeight()) { secondaryContent() }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) { tertiaryContent() }
            }
        }

        // 2. 紧凑小屏模式 (例如 800x480 / 1024x600) -> 单列垂直流动布局
        windowSizeClass.widthClass == CarWindowWidthClass.COMPACT -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                primaryContent()
                secondaryContent()
                tertiaryContent()
            }
        }

        // 3. 标准横屏模式 (例如 1920x1080 15.6寸屏) -> 左右双列左右分栏布局
        else -> {
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    primaryContent()
                    secondaryContent()
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    tertiaryContent()
                }
            }
        }
    }
}
