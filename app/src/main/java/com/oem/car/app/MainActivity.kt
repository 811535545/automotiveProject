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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oem.car.core.designsystem.CarButton
import com.oem.car.core.designsystem.CarTheme
import com.oem.car.feature.climate.ClimateScreen
import com.oem.car.feature.cluster.ClusterScreen
import com.oem.car.feature.connected.ConnectedScreen
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
    DASHBOARD("中控"),
    CLUSTER("仪表"),
    CLIMATE("空调"),
    MEDIA("影音"),
    CONNECTED("互联"),
    SETTINGS("设置")
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
                Text("🚘 AAOS Cockpit", color = Color(0xFF00E5FF), fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))

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
                    CockpitTab.CONNECTED -> ConnectedScreen()
                    CockpitTab.SETTINGS -> SettingsScreen()
                }
            }
        }

        // Voice Assistant PTT Overlay Layer
        VoiceAssistantOverlay()
    }
}
