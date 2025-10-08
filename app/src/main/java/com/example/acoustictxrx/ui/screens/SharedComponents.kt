package com.example.acoustictxrx.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.acoustictxrx.R
import com.example.acoustictxrx.database.TransmissionLog

// Shared constants for UI elements
private val orbitron = FontFamily(Font(R.font.orbitron, FontWeight.Normal))
private val greenGlow = Color(0xff22c55e)
private val redError = Color(0xffef4444)

@Composable
fun LogItemRow(log: TransmissionLog) {
    val isSuccess = log.status == "SUCCESS"
    val statusColor = if (isSuccess) greenGlow else redError
    val iconRes = if (log.direction == "SENT") R.drawable.arrow_up else R.drawable.arrow_down
    val iconTint = if (isSuccess) Color.White else redError.copy(alpha = 0.8f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = log.direction,
            modifier = Modifier.size(20.dp),
            tint = iconTint
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (log.dataContent.isNotBlank()) "\"${log.dataContent}\"" else "---",
                color = Color.White,
                style = TextStyle(fontSize = 14.sp, fontFamily = orbitron)
            )
            Text(
                text = log.details,
                color = Color(0xff9ca3af),
                style = TextStyle(fontSize = 12.sp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = log.status,
            color = statusColor,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
        )
    }
}