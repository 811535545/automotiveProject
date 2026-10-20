package com.oem.car.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oem.car.core.designsystem.CarButton
import com.oem.car.core.designsystem.CarTheme
import com.oem.car.feature.climate.ClimateScreen
import com.oem.car.feature.cluster.ClusterScreen
import com.oem.car.feature.dashboard.DashboardScreen
import com.oem.car.feature.media.MediaScreen
import com.oem.car.feature.settings.SettingsScreen
import com.oem.car.feature.voice.VoiceAssistantOverlay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CarTheme {
                MainCockpitNavigationHost()
            }
        }
    }
}


enum class CockpitTab(val title: String) {
    DASHBOARD("中控仪表"),
    CLUSTER("液晶仪表(2屏)"),
    CLIMATE("空调HVAC"),
    MEDIA("多媒体影音"),
    SETTINGS("车辆设置")
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
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🚘 AAOS Cockpit OS", color = Color(0xFF00E5FF), fontSize = 16.sp)
                Spacer(modifier = Modifier.width(16.dp))

                CockpitTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    CarButton(
                        text = tab.title,
                        onClick = { selectedTab = tab },
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF263238)
                    )
                }
            }

            // Main Screen View Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    CockpitTab.DASHBOARD -> DashboardScreen()
                    CockpitTab.CLUSTER -> ClusterScreen()
                    CockpitTab.CLIMATE -> ClimateScreen()
                    CockpitTab.MEDIA -> MediaScreen()
                    CockpitTab.SETTINGS -> SettingsScreen()
                }
            }
        }

        // Voice Assistant PTT Overlay Layer
        VoiceAssistantOverlay()
    }
}
