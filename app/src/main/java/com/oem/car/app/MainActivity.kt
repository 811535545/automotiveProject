package com.oem.car.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oem.car.core.data.VehicleRepositoryImpl
import com.oem.car.core.designsystem.CarButton
import com.oem.car.core.designsystem.CarTheme
import com.oem.car.feature.climate.ClimateScreen
import com.oem.car.feature.cluster.ClusterScreen
import com.oem.car.feature.connected.ConnectedScreen
import com.oem.car.feature.dashboard.DashboardScreen
import com.oem.car.feature.energy.EnergyScreen
import com.oem.car.feature.health.HealthScreen
import com.oem.car.feature.media.MediaScreen
import com.oem.car.feature.navigation.NavigationScreen
import com.oem.car.feature.phone.PhoneScreen
import com.oem.car.feature.settings.SettingsScreen
import com.oem.car.feature.voice.VoiceAssistantOverlay

/**
 * ============================================================================
 * AAOS 智能座舱系统主入口 Activity (Main Cockpit Activity)
 * ============================================================================
 *
 * 【核心职责】
 * 1. 挂载与初始化座舱核心数据仓库与中间件连接 (`VehicleRepositoryImpl.initialize`)；
 * 2. 初始化 Jetpack Compose 车规级黑暗主题 (`CarTheme`)；
 * 3. 渲染顶部车规 Dock 导航栏、11 大座舱功能 Tab 页面容器及全局 Voice PTT 浮层 (`VoiceAssistantOverlay`)；
 * 4. 可在 AndroidManifest.xml 中配置为系统桌面 (`android.intent.category.HOME`)，实现开机自动全屏呈现。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 在主界面创建时，初始化座舱核心数据门面与 Binder 服务连接通道
        VehicleRepositoryImpl.initialize(applicationContext)

        // 2. 加载声明式 Compose 座舱 HMI 界面
        setContent {
            CarTheme {
                MainCockpitNavigationHost()
            }
        }
    }
}

/**
 * 座舱 11 大核心功能 Tab 枚举定义 (Automotive Cockpit Tab Modules)。
 */
enum class CockpitTab(val title: String) {
    /** 中控大屏：实时仪表数据、总线模拟 Injector 与视频防分心保护卡片 */
    DASHBOARD("中控"),

    /** 液晶仪表屏：三栏式排版，包含 3D SR 感知画卷与 TBT 导航卡片 */
    CLUSTER("仪表"),

    /** 3D 车模：基于 Compose Canvas 的 2.5D/3D 车身姿态数字孪生控制台 */
    CAR_3D("3D车模"),

    /** 导航系统：路线规划、逐段指引与剩余 ETA 显示 */
    NAVIGATION("导航"),

    /** 空调系统 (HVAC)：主驾/副驾双区温度与风速调节 */
    CLIMATE("空调"),

    /** 影音媒体：行车状态自动防分心安全拦截保护 */
    MEDIA("影音"),

    /** 蓝牙电话：拨号、来电接听、通话计时与静音控制 */
    PHONE("电话"),

    /** 智能互联：数字钥匙 (CCC 3.0)、T-BOX/OTA 升级、ADAS 智驾与 DMS 多账户 */
    CONNECTED("互联"),

    /** 能量管理：SOC 环形仪表、充电限值设置、预约充电与电池预热 */
    ENERGY("能量"),

    /** 车辆健康：四轮胎压监控、全车系统自检与 UDS/DTC 故障码扫描清除 */
    HEALTH("健康"),

    /** 车辆设置：驾驶模式切换 (ECO/COMFORT/SPORT) 与辅助驾驶配置 */
    SETTINGS("设置")
}

/**
 * 智能座舱主导航宿主组件 (Main Cockpit Navigation Host)。
 *
 * 界面结构：
 * - 顶部：64dp 高度的车规 Dock 导航栏（支持横向滑动，适合各类宽屏/窄屏）；
 * - 主体：按 selectedTab 动态重组（Recompose）渲染对应的功能 Screen；
 * - 浮层：顶层挂载 VoiceAssistantOverlay，响应方向盘 PTT 语音按键唤醒。
 */
@Composable
fun MainCockpitNavigationHost() {
    var selectedTab by remember { mutableStateOf(CockpitTab.DASHBOARD) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. 顶部车规级 Dock 导航栏 (Automotive Top Dock Bar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color(0xFF141820))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 车机 Branding 图标与标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.mipmap.ic_car),
                        contentDescription = "Car Icon",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = "AAOS Cockpit",
                        color = Color(0xFF00E5FF),
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 渲染 11 个 Dock Tab 切换按钮
                CockpitTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    CarButton(
                        text = tab.title,
                        onClick = { selectedTab = tab },
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color(0xFF263238)
                        }
                    )
                }
            }

            // 2. 中央核心视角渲染区域 (Main View Content Area)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    CockpitTab.DASHBOARD -> DashboardScreen()
                    CockpitTab.CLUSTER -> ClusterScreen()
                    CockpitTab.CAR_3D -> com.oem.car.feature.dashboard.Vehicle3dScreen()
                    CockpitTab.NAVIGATION -> NavigationScreen()
                    CockpitTab.CLIMATE -> ClimateScreen()
                    CockpitTab.MEDIA -> MediaScreen()
                    CockpitTab.PHONE -> PhoneScreen()
                    CockpitTab.CONNECTED -> ConnectedScreen()
                    CockpitTab.ENERGY -> EnergyScreen()
                    CockpitTab.HEALTH -> HealthScreen()
                    CockpitTab.SETTINGS -> SettingsScreen()
                }
            }
        }

        // 3. 全局 Voice PTT 语音助手 Overlay 悬浮层
        VoiceAssistantOverlay()
    }
}
