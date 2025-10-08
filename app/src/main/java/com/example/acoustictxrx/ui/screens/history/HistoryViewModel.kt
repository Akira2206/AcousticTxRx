package com.example.acoustictxrx.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.acoustictxrx.database.TransmissionLog
import com.example.acoustictxrx.database.TransmissionLogDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

// Represents the dynamic data for the History screen
data class HistoryUiState(
    val logs: List<TransmissionLog> = emptyList()
)

class HistoryViewModel(
    private val logDao: TransmissionLogDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // When the ViewModel is created, start listening to the database.
        // The list will automatically update whenever a new log is added.
        logDao.getAllLogs()
            .onEach { logs ->
                _uiState.update { it.copy(logs = logs.reversed()) }
            }
            .launchIn(viewModelScope)
    }
}

// Factory to help us create the HistoryViewModel
class HistoryViewModelFactory(
    private val logDao: TransmissionLogDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(logDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}