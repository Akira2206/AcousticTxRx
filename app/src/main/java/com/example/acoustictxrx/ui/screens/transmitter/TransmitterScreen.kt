package com.example.acoustictxrx.ui.screens.transmitter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.acoustictxrx.R

// --- Constants ---
private val orbitron = FontFamily(Font(R.font.orbitron, FontWeight.Normal))
private val greenGlow = Color(0xff22c55e)
private val backgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0A0A0A), Color(0xFF111827))
)

@Composable
fun TransmitterScreen(
    uiState: TransmitterUiState,
    onEvent: (TransmitterEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
    ) {
        // ✅ 1. Main layout is now a non-scrolling Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // --- FIXED CONTENT ---
            Spacer(Modifier.height(32.dp))
            TransmitterHeader()
            Spacer(Modifier.height(32.dp))
            SignalStatusDisplay()
            Spacer(Modifier.height(32.dp))

            // --- SCROLLABLE CONTENT ---
            val scrollState = rememberScrollState()
            Column(
                // ✅ 2. This inner Column is scrollable and takes the remaining space
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                DataPayloadInput(
                    value = uiState.dataPayload,
                    onValueChange = { newText ->
                        onEvent(TransmitterEvent.OnDataPayloadChange(newText))
                    }
                )
                Spacer(Modifier.height(24.dp))
                TransmissionSettings()
                Spacer(Modifier.height(16.dp))
                TransmitButton(
                    enabled = !uiState.isTransmitting,
                    onClick = { onEvent(TransmitterEvent.OnTransmitClick) }
                )
                Spacer(Modifier.height(24.dp))
                StatusInfoPanel(
                    status = uiState.status
                )
                Spacer(Modifier.height(16.dp)) // Padding at the bottom
            }
        }
    }
}


// --- Other composables remain the same ---
@Composable
fun TransmitterHeader() {
    Column {
        Text(text = "DATA TRANSMITTER", color = greenGlow, lineHeight = 1.33.em, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = orbitron))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(shape = RoundedCornerShape(20.dp)).background(color = greenGlow.copy(alpha = 0.63f)))
            Spacer(Modifier.width(8.dp))
            Text(text = "TERMINAL ACTIVE", color = Color(0xff9ca3af), style = TextStyle(fontSize = 14.sp, fontFamily = orbitron))
        }
    }
}

@Composable
fun SignalStatusDisplay() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(160.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
            Box(modifier = Modifier.fillMaxSize().drawBehind { val brush = Brush.radialGradient(colors = listOf(greenGlow.copy(alpha = 0.4f), Color.Transparent), center = center, radius = (size.minDimension / 2.0f) + 4.dp.toPx()); drawCircle(brush) }.border(border = BorderStroke(0.5.dp, greenGlow), shape = CircleShape))
            Box(modifier = Modifier.size(128.dp).drawBehind { val brush = Brush.radialGradient(colors = listOf(greenGlow.copy(alpha = 0.5f), Color.Transparent), center = center, radius = (size.minDimension / 2.0f) + 6.dp.toPx()); drawCircle(brush) }.border(border = BorderStroke(2.dp, greenGlow), shape = CircleShape))
            Image(painter = painterResource(id = R.drawable.signal), contentDescription = "Signal", colorFilter = ColorFilter.tint(greenGlow), modifier = Modifier.size(45.dp, 36.dp))
        }
    }
}

@Composable
fun DataPayloadInput(value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(text = "DATA PAYLOAD", color = Color(0xff9ca3af), style = TextStyle(fontSize = 14.sp, fontFamily = orbitron))
        Spacer(Modifier.height(8.dp))
        BasicTextField(value = value, onValueChange = onValueChange, textStyle = TextStyle(color = Color(0xffadaebc), fontSize = 16.sp, fontFamily = orbitron, lineHeight = 1.5.em), cursorBrush = SolidColor(greenGlow), decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxWidth().height(96.dp).background(Color(0xff1a1a1a), MaterialTheme.shapes.small).border(1.dp, Color(0xff374151), MaterialTheme.shapes.small).padding(12.dp)) {
                if (value.isEmpty()) { Text(text = "Enter data to transmit...", color = Color(0xffadaebc), style = TextStyle(fontSize = 16.sp, fontFamily = orbitron)) }
                innerTextField()
            }
        })
    }
}

@Composable
fun TransmissionSettings() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SettingsDropdown(label = "MODULATION", value = "2-3 KHZ", modifier = Modifier.weight(1f))
        Spacer(Modifier.width(16.dp))
        SettingsDropdown(label = "POWER", value = "HIGH", modifier = Modifier.weight(1f))
    }
}

@Composable
fun SettingsDropdown(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, color = Color(0xff9ca3af), style = TextStyle(fontSize = 12.sp, fontFamily = orbitron))
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().height(37.dp).background(Color(0xff1a1a1a), MaterialTheme.shapes.small).border(1.dp, Color(0xff374151), MaterialTheme.shapes.small).padding(horizontal = 12.dp)) {
            Text(text = value, color = Color.White, style = TextStyle(fontSize = 14.sp, fontFamily = orbitron), modifier = Modifier.align(Alignment.CenterStart))
            Image(painter = painterResource(id = R.drawable.dropdown), contentDescription = "Dropdown", colorFilter = ColorFilter.tint(Color.White), modifier = Modifier.size(10.dp).align(Alignment.CenterEnd))
        }
    }
}

@Composable
fun TransmitButton(enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = greenGlow, contentColor = Color.Black, disabledContainerColor = greenGlow.copy(alpha = 0.5f), disabledContentColor = Color.Black.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Image(painter = painterResource(id = R.drawable.send), contentDescription = null, colorFilter = ColorFilter.tint(Color.Black))
        Spacer(Modifier.width(8.dp))
        Text(text = "TRANSMIT DATA", style = TextStyle(fontSize = 16.sp, fontFamily = orbitron, fontWeight = FontWeight.Bold))
    }
}

@Composable
fun StatusInfoPanel(status: String) {
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xff1a1a1a), RoundedCornerShape(8.dp)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "STATUS", color = Color(0xff9ca3af), style = TextStyle(fontSize = 12.sp, fontFamily = orbitron))
            Text(text = status, color = greenGlow, style = TextStyle(fontSize = 12.sp, fontFamily = orbitron))
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "OUTPUT LEVEL", color = Color(0xff9ca3af), style = TextStyle(fontSize = 12.sp, fontFamily = orbitron))
            Row {
                Box(modifier = Modifier.size(4.dp, 12.dp).background(greenGlow)); Spacer(Modifier.width(4.dp)); Box(modifier = Modifier.size(4.dp, 12.dp).background(greenGlow)); Spacer(Modifier.width(4.dp)); Box(modifier = Modifier.size(4.dp, 12.dp).background(greenGlow)); Spacer(Modifier.width(4.dp)); Box(modifier = Modifier.size(4.dp, 12.dp).background(Color(0xff4b5563)))
            }
        }
    }
}