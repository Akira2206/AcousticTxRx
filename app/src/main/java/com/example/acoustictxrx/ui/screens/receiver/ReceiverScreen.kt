package com.example.acoustictxrx.ui.screens.receiver

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.acoustictxrx.R
import com.example.acoustictxrx.database.TransmissionLog
import com.example.acoustictxrx.ui.screens.LogItemRow
import kotlinx.coroutines.delay

// --- Constants ---
private val orbitron = FontFamily(Font(R.font.orbitron, FontWeight.Normal))
private val greenGlow = Color(0xff22c55e)
private val redError = Color(0xffef4444)
private val backgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0A0A0A), Color(0xFF111827))
)


@Composable
fun ReceiverScreen(
    uiState: ReceiverUiState,
    onEvent: (ReceiverEvent) -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultWave = listOf(0.3f, 0.5f, 0.7f, 0.9f, 1.0f, 0.8f, 0.6f, 0.4f, 0.7f, 0.5f, 0.3f, 0.6f, 0.8f, 1.0f, 0.7f, 0.4f, 0.2f)
    var waveHeights by remember { mutableStateOf(defaultWave) }

    LaunchedEffect(uiState.isRecording) {
        if (uiState.isRecording) {
            // If we are recording, start an infinite loop
            while (true) {
                // Generate a new list of 17 random heights between 0.2 and 1.0
                waveHeights = List(17) { (Math.random() * 0.8f + 0.2f).toFloat() }
                // Wait for a short time to create an animation effect
                delay(120L)
            }
        } else {
            // When not recording, reset to the default static pattern
            waveHeights = defaultWave
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
    ) {
        // ✅ 1. Main layout is now a standard Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- FIXED CONTENT ---
            Spacer(Modifier.height(32.dp))
            ReceiverHeader()
            Spacer(Modifier.height(24.dp))
            WaveformDisplay(heights = waveHeights)
            Spacer(Modifier.height(24.dp))
            RecordButton(
                isRecording = uiState.isRecording,
                onClick = { onEvent(ReceiverEvent.OnRecordClick) },
                onRequestPermission = onRequestPermission
            )
            Spacer(Modifier.height(24.dp))
            TimeoutInput(
                value = uiState.timeoutSecs,
                onValueChange = { onEvent(ReceiverEvent.OnTimeoutChange(it)) }
            )
            Spacer(Modifier.height(32.dp))

            // --- SCROLLABLE CONTENT ---
            if (uiState.receivedText.isNotEmpty()) {
                ReceivedDataDisplay(text = uiState.receivedText)
                Spacer(Modifier.height(32.dp))
            }

            LogHistoryList(
                logs = uiState.logs,
                // ✅ 2. The weight modifier makes this component fill all remaining vertical space
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.height(16.dp)) // Padding at the very bottom
        }
    }
}

// --- UI Components ---
@Composable
fun LogHistoryList(logs: List<TransmissionLog>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = "RECENT ACTIVITY", color = Color(0xff9ca3af), style = TextStyle(fontSize = 14.sp, fontFamily = orbitron))
        Spacer(Modifier.height(8.dp))
        // ✅ 3. This LazyColumn now scrolls within the space given by the weight modifier
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xff1a1a1a), RoundedCornerShape(8.dp))
                .padding(vertical = 8.dp)
        ) {
            items(logs) { log ->
                LogItemRow(log = log)
            }
        }
    }
}


// --- Other composables remain the same ---

@Composable
fun ReceiverHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "DATA RECEIVER", color = greenGlow, lineHeight = 1.33.em, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = orbitron))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(shape = RoundedCornerShape(20.dp)).background(color = greenGlow.copy(alpha = 0.63f)))
            Spacer(Modifier.width(8.dp))
            Text(text = "TERMINAL ACTIVE", color = Color(0xff9ca3af), style = TextStyle(fontSize = 14.sp, fontFamily = orbitron))
        }
    }
}

@Composable
fun WaveformDisplay(heights: List<Float>) {
    Row(modifier = Modifier.fillMaxWidth().height(80.dp).background(Color(0x1AFFFFFF), RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        heights.forEach { height ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(height)
                    .background(greenGlow.copy(alpha = (height * 0.9f).coerceAtLeast(0.4f)), CircleShape)
            )
        }
    }
}

@Composable
fun RecordButton(isRecording: Boolean, onClick: () -> Unit, onRequestPermission: () -> Unit) {
    val context = LocalContext.current
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp).drawBehind { val brush = Brush.radialGradient(colors = listOf(greenGlow.copy(alpha = 0.3f), Color.Transparent), radius = (size.minDimension / 2.0f) + 4.dp.toPx()); drawCircle(brush) }) {
        Button(onClick = { val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED; if (hasPermission) { onClick() } else { onRequestPermission() } }, enabled = !isRecording, shape = CircleShape, modifier = Modifier.fillMaxSize(), colors = ButtonDefaults.buttonColors(containerColor = greenGlow, contentColor = Color.Black, disabledContainerColor = greenGlow.copy(alpha = 0.5f))) {
            Icon(painter = painterResource(id = R.drawable.mic), contentDescription = "Record", modifier = Modifier.size(48.dp), tint = Color.Black)
        }
    }
}

@Composable
fun TimeoutInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(200.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "RECEIVE TIMEOUT (SECONDS)", color = Color(0xff9ca3af), style = TextStyle(fontSize = 12.sp, fontFamily = orbitron))
        Spacer(Modifier.height(4.dp))
        BasicTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth().height(37.dp).background(Color(0xff1a1a1a), MaterialTheme.shapes.small).border(1.dp, Color(0xff374151), MaterialTheme.shapes.small), textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontFamily = orbitron, textAlign = TextAlign.Center), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, cursorBrush = SolidColor(greenGlow), decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                innerTextField()
            }
        })
    }
}

@Composable
fun ReceivedDataDisplay(text: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "RECEIVED DATA", color = Color(0xff9ca3af), style = TextStyle(fontSize = 14.sp, fontFamily = orbitron))
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 96.dp).background(Color(0xff1a1a1a), MaterialTheme.shapes.small).border(1.dp, Color(0xff374151), MaterialTheme.shapes.small).padding(12.dp)) {
            Text(text = text, color = Color(0xffadaebc), style = TextStyle(fontSize = 16.sp, fontFamily = orbitron, lineHeight = 1.5.em))
        }
    }
}