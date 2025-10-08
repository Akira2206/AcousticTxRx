package com.example.acoustictxrx.ui.screens.receiver

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.acoustictxrx.AcousticModem
import com.example.acoustictxrx.database.TransmissionLog
import com.example.acoustictxrx.database.TransmissionLogDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Represents all the dynamic data on our Receiver screen
data class ReceiverUiState(
    val timeoutSecs: String = "60",
    val receivedText: String = "",
    val isRecording: Boolean = false,
    val statusMessage: String = "STANDBY",
    val logs: List<TransmissionLog> = emptyList()
)

// Represents actions the user can take on this screen
sealed interface ReceiverEvent {
    data class OnTimeoutChange(val text: String) : ReceiverEvent
    object OnRecordClick : ReceiverEvent
}

class ReceiverViewModel(
    private val logDao: TransmissionLogDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiverUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Automatically listen for changes in the database's log table.
        // Whenever a new log is added, it will update our UI state's log list.
        logDao.getAllLogs()
            .onEach { logs ->
                _uiState.update { it.copy(logs = logs.reversed()) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ReceiverEvent) {
        when (event) {
            is ReceiverEvent.OnTimeoutChange -> {
                // Allow only digits for the timeout
                if (event.text.all { it.isDigit() }) {
                    _uiState.update { it.copy(timeoutSecs = event.text) }
                }
            }
            is ReceiverEvent.OnRecordClick -> {
                startRecording()
            }
        }
    }
    @SuppressLint("MissingPermission")
    private fun startRecording() {
        if (_uiState.value.isRecording) return

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update {
                it.copy(
                    isRecording = true,
                    statusMessage = "LISTENING...",
                    receivedText = "" // Clear previous text
                )
            }

            val timeout = _uiState.value.timeoutSecs.toIntOrNull() ?: 60
            // This is a blocking call, so it's perfect for Dispatchers.Default
            val receivedMessage = AcousticModem.receiveOneMessageBlocking(timeout)

            val log: TransmissionLog
            if (receivedMessage != null) {
                // SUCCESS
                log = TransmissionLog(
                    direction = "RECEIVED",
                    status = "SUCCESS",
                    dataContent = receivedMessage,
                    details = "Received ${receivedMessage.length} chars"
                )
                _uiState.update {
                    it.copy(
                        receivedText = receivedMessage,
                        statusMessage = "SUCCESS"
                    )
                }
            } else {
                // FAILURE
                log = TransmissionLog(
                    direction = "RECEIVED",
                    status = "FAILURE",
                    dataContent = "",
                    details = "Failed to decode (Timeout or CRC error)"
                )
                _uiState.update {
                    it.copy(
                        receivedText = "",
                        statusMessage = "FAILURE: ${log.details}"
                    )
                }
            }
            logDao.insert(log)
            _uiState.update { it.copy(isRecording = false) }
        }
    }
}

class ReceiverViewModelFactory(
    private val logDao: TransmissionLogDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReceiverViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReceiverViewModel(logDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}