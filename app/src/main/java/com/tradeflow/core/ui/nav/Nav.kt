package com.tradeflow.core.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.tradeflow.core.R
import com.tradeflow.core.ui.screens.CalendarScreen
import com.tradeflow.core.ui.screens.CustomersScreen
import com.tradeflow.core.ui.screens.DiagScreen
import com.tradeflow.core.ui.screens.JobsScreen
import com.tradeflow.core.ui.screens.MessagesScreen
import com.tradeflow.core.ui.screens.MoreScreen
import com.tradeflow.core.ui.screens.SettingsScreen

private data class Tab(val title: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("Customers", Icons.Filled.Person),
    Tab("Calendar", Icons.Filled.DateRange),
    Tab("Jobs", Icons.Filled.Work),
    Tab("Messages", Icons.Filled.Message),
    Tab("More", Icons.Filled.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeFlowApp(initialPhone: String? = null) {
    var tab by remember { mutableStateOf(if (initialPhone != null) 3 else 0) }
    var route by remember { mutableStateOf("tabs") } // tabs | settings | diag
    var deepPhone by remember { mutableStateOf(initialPhone) }
    val appName = LocalContext.current.getString(R.string.app_name)

    when (route) {
        "settings" -> SettingsScreen(onBack = { route = "tabs" })
        "diag" -> DiagScreen(onBack = { route = "tabs" })
        else -> Scaffold(
            topBar = { TopAppBar(title = { Text(appName) }) },
            bottomBar = {
                NavigationBar {
                    TABS.forEachIndexed { index, t ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Icon(t.icon, contentDescription = t.title) },
                            label = { Text(t.title) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    0 -> CustomersScreen()
                    1 -> CalendarScreen()
                    2 -> JobsScreen()
                    3 -> MessagesScreen(
                        initialPhone = deepPhone,
                        onOpened = { deepPhone = null }
                    )
                    4 -> MoreScreen(
                        onSettings = { route = "settings" },
                        onDiag = { route = "diag" }
                    )
                }
            }
        }
    }
}
