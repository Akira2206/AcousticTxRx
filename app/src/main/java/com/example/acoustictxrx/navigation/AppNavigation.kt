package com.example.acoustictxrx.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.acoustictxrx.ui.screens.history.HistoryScreen
import com.example.acoustictxrx.ui.screens.history.HistoryViewModel
import com.example.acoustictxrx.ui.screens.history.HistoryViewModelFactory
import com.example.acoustictxrx.ui.screens.receiver.ReceiverScreen
import com.example.acoustictxrx.ui.screens.receiver.ReceiverViewModel
import com.example.acoustictxrx.ui.screens.receiver.ReceiverViewModelFactory
import com.example.acoustictxrx.ui.screens.transmitter.TransmitterScreen
import com.example.acoustictxrx.ui.screens.transmitter.TransmitterViewModel
import com.example.acoustictxrx.ui.screens.transmitter.TransmitterViewModelFactory

// Defines the unique routes for our screens
object AppRoutes {
    const val TRANSMITTER = "transmitter"
    const val RECEIVER = "receiver"
    const val HISTORY = "history"
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    transmitterFactory: TransmitterViewModelFactory,
    receiverFactory: ReceiverViewModelFactory,
    historyFactory: HistoryViewModelFactory,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.TRANSMITTER,
        modifier = modifier
    ) {
        composable(AppRoutes.TRANSMITTER) {
            val viewModel: TransmitterViewModel = viewModel(factory = transmitterFactory)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            TransmitterScreen(
                uiState = uiState,
                onEvent = viewModel::onEvent
            )
        }

        composable(AppRoutes.RECEIVER) {
            val viewModel: ReceiverViewModel = viewModel(factory = receiverFactory)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ReceiverScreen(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onRequestPermission = onRequestPermission
            )
        }

        composable(AppRoutes.HISTORY) {
            val viewModel: HistoryViewModel = viewModel(factory = historyFactory)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            HistoryScreen(uiState = uiState)
        }
    }
}