package com.example.acoustictxrx

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.acoustictxrx.navigation.AppNavHost
import com.example.acoustictxrx.navigation.AppRoutes
import com.example.acoustictxrx.ui.screens.history.HistoryViewModelFactory
import com.example.acoustictxrx.ui.screens.receiver.ReceiverViewModelFactory
import com.example.acoustictxrx.ui.screens.transmitter.TransmitterViewModelFactory

private val orbitron = FontFamily(Font(R.font.orbitron, FontWeight.Normal))
private val greenGlow = Color(0xff22c55e)

@Composable
fun MainScreen(
    transmitterFactory: TransmitterViewModelFactory,
    receiverFactory: ReceiverViewModelFactory,
    historyFactory: HistoryViewModelFactory,
    onRequestPermission: () -> Unit
) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { AppBottomNavigation(navController = navController) }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            transmitterFactory = transmitterFactory,
            receiverFactory = receiverFactory,
            historyFactory = historyFactory,
            onRequestPermission = onRequestPermission,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun AppBottomNavigation(navController: NavController) {
    val navItems = listOf(
        BottomNavItem("Transmit", AppRoutes.TRANSMITTER, R.drawable.transmitter),
        BottomNavItem("Receive", AppRoutes.RECEIVER, R.drawable.receiver),
        BottomNavItem("History", AppRoutes.HISTORY, R.drawable.history)
    )

    NavigationBar(
        containerColor = Color(0xff1a1a1a)
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        navItems.forEach { item ->
            NavigationBarItem(
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(painterResource(id = item.iconRes), contentDescription = item.label) },
                label = { Text(text = item.label, style = TextStyle(fontSize = 12.sp, fontFamily = orbitron)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = greenGlow,
                    selectedTextColor = greenGlow,
                    unselectedIconColor = Color(0xff6b7280),
                    unselectedTextColor = Color(0xff6b7280),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

private data class BottomNavItem(val label: String, val route: String, val iconRes: Int)