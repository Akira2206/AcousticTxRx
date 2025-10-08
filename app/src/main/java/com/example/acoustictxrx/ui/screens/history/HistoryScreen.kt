package com.example.acoustictxrx.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.acoustictxrx.R
import com.example.acoustictxrx.ui.screens.LogItemRow

// --- Constants ---
private val orbitron = FontFamily(Font(R.font.orbitron, FontWeight.Normal))
private val greenGlow = Color(0xff22c55e)
private val backgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0A0A0A), Color(0xFF111827))
)

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            HistoryHeader()
            Spacer(Modifier.height(24.dp))

            // A scrollable list that shows all the logs
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.logs) { log ->
                    LogItemRow(log = log)
                }
            }
        }
    }
}

@Composable
fun HistoryHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "TRANSMISSION LOGS",
            color = greenGlow,
            lineHeight = 1.33.em,
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = orbitron)
        )
    }
}