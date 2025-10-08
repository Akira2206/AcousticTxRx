//package com.example.acoustictxrx
//
//import android.Manifest
//import android.content.pm.PackageManager
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.unit.dp
//import androidx.core.content.ContextCompat
//import com.example.acoustictxrx.database.AppDatabase
//import com.example.acoustictxrx.database.TransmissionLog
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//
//class MainActivity : ComponentActivity() {
//
//    private val requestMicPermission = registerForActivityResult(
//        ActivityResultContracts.RequestPermission()
//    ) { isGranted ->
//        // Optional: Handle permission grant/denial
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            MaterialTheme {
//                AcousticModemScreen(
//                    onRequestPermission = {
//                        requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
//                    }
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun AcousticModemScreen(onRequestPermission: () -> Unit) {
//    var inputText by remember { mutableStateOf("") }
//    var statusText by remember { mutableStateOf("Ready") }
//    var timeoutSecs by remember { mutableStateOf("60") }
//    val coroutineScope = rememberCoroutineScope()
//    val context = LocalContext.current
//    val logDao = remember { AppDatabase.getDatabase(context).transmissionLogDao() }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.spacedBy(8.dp)
//    ) {
//
//        OutlinedTextField(
//            value = inputText,
//            onValueChange = { inputText = it },
//            label = { Text("Text to transmit") },
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Button(
//            onClick = {
//                if (inputText.isBlank()) {
//                    statusText = "Enter text to send"
//                    return@Button
//                }
//                coroutineScope.launch(Dispatchers.Default) {
//                    var log: TransmissionLog
//                    try {
//                        val pcm = AcousticModem.buildFramePcmFromText(inputText)
//                        withContext(Dispatchers.Main) {
//                            statusText = "Sending..."
//                        }
//                        AcousticModem.playPcm(pcm)
//                        // Create a success log
//                        log = TransmissionLog(
//                            direction = "SENT",
//                            status = "SUCCESS",
//                            dataContent = inputText,
//                            details = "Sent ${inputText.length} chars"
//                        )
//                        withContext(Dispatchers.Main) { statusText = log.details }
////                        withContext(Dispatchers.Main) {
////                            statusText = "Sent ${inputText.length} chars"
////                        }
//                    } catch (e: Exception) {
//                        // Create a failure log
//                        log = TransmissionLog(
//                            direction = "SENT",
//                            status = "FAILURE",
//                            dataContent = inputText,
//                            details = e.message ?: "Unknown send error"
//                        )
//                        withContext(Dispatchers.Main) { statusText = "Send error: ${log.details}" }
////                        withContext(Dispatchers.Main) {
////                            statusText = "Send error: ${e.message}"
////                        }
//                    }
//                    logDao.insert(log)
//                }
//            },
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text("Send (Audio)")
//        }
//
//        Divider(modifier = Modifier.padding(vertical = 8.dp))
//
//        OutlinedTextField(
//            value = timeoutSecs,
//            onValueChange = { timeoutSecs = it },
//            label = { Text("Receive Timeout (seconds)") },
//            // This line now works because of the new imports
//            keyboardOptions = KeyboardOptions(
//                keyboardType = KeyboardType.Number
//            ),
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Button(
//            onClick = {
//                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
//                    statusText = "Listening..."
//                    coroutineScope.launch(Dispatchers.Default) {
//                        val timeout = timeoutSecs.toIntOrNull() ?: 60
//                        val msg = AcousticModem.receiveOneMessageBlocking(maxSeconds = timeout)
//
//                        // Create a log based on the result
//                        val log = if (msg != null) {
//                            TransmissionLog(
//                                direction = "RECEIVED",
//                                status = "SUCCESS",
//                                dataContent = msg,
//                                details = "Received ${msg.length} chars"
//                            )
//                        } else {
//                            TransmissionLog(
//                                direction = "RECEIVED",
//                                status = "FAILURE",
//                                dataContent = "",
//                                details = "Failed to decode (Timeout or CRC error)"
//                            )
//                        }
//                        logDao.insert(log)
//                        withContext(Dispatchers.Main) {
//                            statusText = if (msg != null) "Received: $msg" else "Failed to decode"
//                        }
//                    }
//
//                } else {
//                    onRequestPermission()
//                    statusText = "Microphone permission needed. Please grant it and tap Receive again."
//                }
//            },
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text("Receive")
//        }
//
//        Text(
//            text = statusText,
//            style = MaterialTheme.typography.bodyLarge,
//            modifier = Modifier
//                .padding(top = 16.dp)
//                .fillMaxWidth()
//        )
//    }
//}
package com.example.acoustictxrx // Change to your package name

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.acoustictxrx.database.AppDatabase
import com.example.acoustictxrx.ui.screens.receiver.ReceiverViewModelFactory
import com.example.acoustictxrx.ui.screens.transmitter.TransmitterViewModelFactory
import com.example.acoustictxrx.ui.theme.AcousticTxRxTheme
import com.example.acoustictxrx.MainScreen
import com.example.acoustictxrx.ui.screens.history.HistoryViewModelFactory

class MainActivity : ComponentActivity() {

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Optional: handle grant/denial */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val logDao = AppDatabase.getDatabase(applicationContext).transmissionLogDao()
        val transmitterFactory = TransmitterViewModelFactory(logDao)
        val receiverFactory = ReceiverViewModelFactory(logDao)
        val historyFactory = HistoryViewModelFactory(logDao)

        setContent {
            AcousticTxRxTheme {
                MainScreen(
                    transmitterFactory = transmitterFactory,
                    receiverFactory = receiverFactory,
                    historyFactory = historyFactory,
                    onRequestPermission = {
                        requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
            }
        }
    }
}