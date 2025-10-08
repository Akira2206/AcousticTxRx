package com.example.acoustictxrx.ui.screens.transmitter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.acoustictxrx.AcousticModem
import com.example.acoustictxrx.database.TransmissionLog
import com.example.acoustictxrx.database.TransmissionLogDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 1. Define the data that your UI will show
// Represents all the dynamic data on our screen
data class TransmitterUiState(
    val dataPayload: String = "",
    val status: String = "READY",
    val isTransmitting: Boolean = false
)

// Represents actions the user can take
sealed interface TransmitterEvent {
    data class OnDataPayloadChange(val text: String) : TransmitterEvent
    object OnTransmitClick : TransmitterEvent
}

class TransmitterViewModel(
    private val logDao: TransmissionLogDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransmitterUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: TransmitterEvent) {
        when (event) {
            is TransmitterEvent.OnDataPayloadChange -> {
                _uiState.update { it.copy(dataPayload = event.text) }
            }
            is TransmitterEvent.OnTransmitClick -> {
                transmitData()
            }
        }
    }

    private fun transmitData() {
        // Prevent multiple transmissions at once
        if (_uiState.value.isTransmitting) return

        val dataToSend = _uiState.value.dataPayload
        if (dataToSend.isBlank()) {
            _uiState.update { it.copy(status = "ERROR: PAYLOAD EMPTY") }
            return
        }

        viewModelScope.launch {
            // Update UI to show we're working
            _uiState.update { it.copy(isTransmitting = true, status = "GENERATING TONE...") }
            var log: TransmissionLog

            try {
                // Generate the audio signal (can be slow, so we use Dispatchers.Default)
                val pcm = kotlinx.coroutines.withContext(Dispatchers.Default) {
                    AcousticModem.buildFramePcmFromText(dataToSend)
                }

                // Update status before playing
                _uiState.update { it.copy(status = "TRANSMITTING...") }

                // Play the audio
                AcousticModem.playPcm(pcm)

                // Create a success log
                log = TransmissionLog(
                    direction = "SENT",
                    status = "SUCCESS",
                    dataContent = dataToSend,
                    details = "Sent ${dataToSend.length} chars"
                )
                // Update UI with success message
                _uiState.update { it.copy(isTransmitting = false, status = "SUCCESS") }

            } catch (e: Exception) {
                // Create a failure log
                log = TransmissionLog(
                    direction = "SENT",
                    status = "FAILURE",
                    dataContent = dataToSend,
                    details = e.message ?: "Unknown transmission error"
                )
                // Update UI with error message
                _uiState.update { it.copy(isTransmitting = false, status = "ERROR: ${log.details}") }
            }
            // Save the log to the database
            logDao.insert(log)
        }
    }
}
// This class teaches the app how to create our TransmitterViewModel,
// since it has a special requirement (the logDao).
class TransmitterViewModelFactory(
    private val logDao: TransmissionLogDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransmitterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransmitterViewModel(logDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}