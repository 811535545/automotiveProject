package com.oem.car.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * 车载屏幕宽度类型。
 */
enum class CarWindowWidthClass {
    /** 紧凑屏 (如 800x480、1024x600 小尺寸中控/仪表) */
    COMPACT,
    /** 标准屏 (如 1280x800、1920x1080 15.6寸横屏) */
    MEDIUM,
    /** 超宽屏 (如 1920x720、2560x960 21:9 或 8:3 贯穿屏) */
    EXPANDED
}

/**
 * 车载屏幕高度类型。
 */
enum class CarWindowHeightClass {
    /** 矮宽屏 (如 1920x720 超宽带状屏，高度小于 500dp) */
    COMPACT,
    /** 标准高度 (高度大等于 500dp) */
    MEDIUM
}

/**
 * 车载窗口尺寸分类。
 */
data class CarWindowSizeClass(
    val widthClass: CarWindowWidthClass,
    val heightClass: CarWindowHeightClass,
    val isUltraWide: Boolean,
    val screenWidthDp: Int,
    val screenHeightDp: Int
)

/**
 * 动态获取当前车载屏幕的尺寸规格与宽高比。
 */
@Composable
fun rememberCarWindowSizeClass(): CarWindowSizeClass {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val aspectRatio = screenWidth.toFloat() / screenHeight.coerceAtLeast(1).toFloat()

    val widthClass = when {
        screenWidth < 850 -> CarWindowWidthClass.COMPACT
        screenWidth < 1100 -> CarWindowWidthClass.MEDIUM
        else -> CarWindowWidthClass.EXPANDED
    }

    val heightClass = when {
        screenHeight < 500 -> CarWindowHeightClass.COMPACT
        else -> CarWindowHeightClass.MEDIUM
    }

    return CarWindowSizeClass(
        widthClass = widthClass,
        heightClass = heightClass,
        isUltraWide = aspectRatio >= 2.1f,
        screenWidthDp = screenWidth,
        screenHeightDp = screenHeight
    )
}
