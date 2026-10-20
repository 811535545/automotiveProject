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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VehicleRepositoryImpl.initialize(applicationContext)
        setContent {
            CarTheme {
                MainCockpitNavigationHost()
            }
            title = "aaa"
        }
    }
}

enum class CockpitTab(val title: String) {
    DASHBOARD("中控"), CLUSTER("仪表"), NAVIGATION("导航"), CLIMATE("空调"), MEDIA("影音"), PHONE("电话"), CONNECTED(
        "互联"
    ),
    ENERGY("能量"), HEALTH("健康"), SETTINGS("设置")
}

@Composable
fun MainCockpitNavigationHost() {

    var selectedTab by remember { mutableStateOf(CockpitTab.DASHBOARD) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Navigation Bar (Automotive Dock Bar)
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

                //Text("🚘 AAOS Cockpit", color = Color(0xFF00E5FF), fontSize = 16.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp) // 控制图标与文字的间距
                ) {

                    Icon(
                        painter = painterResource(id = R.mipmap.ic_car), // 你的 Vector Drawable 图标
                        contentDescription = "Car Icon",
                        tint = Color(0xFF00E5FF), // 统一的青蓝色调
                        modifier = Modifier.size(20.dp) // 控制图标大小
                    )

                    Text(
                        text = "AAOS Cockpit",
                        color = Color(0xFF00E5FF),
                        fontSize = 16.sp
                    )

                }

                Spacer(modifier = Modifier.width(8.dp))

                CockpitTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    CarButton(
                        text = tab.title,
                        onClick = { selectedTab = tab },
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(
                            0xFF263238
                        )
                    )
                }

            }

            //Main Screen View Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    CockpitTab.DASHBOARD -> DashboardScreen()
                    CockpitTab.CLUSTER -> ClusterScreen()
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

        // Voice Assistant PTT Overlay Layer
        VoiceAssistantOverlay()
    }
}
