package com.example.acoustictxrx.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transmission_logs")
data class TransmissionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "direction")
    val direction: String, // "SENT" or "RECEIVED"

    @ColumnInfo(name = "status")
    val status: String, // "SUCCESS" or "FAILURE"

    @ColumnInfo(name = "data_content")
    val dataContent: String,

    @ColumnInfo(name = "details")
    val details: String, // e.g., "CRC Mismatch" or "" for success

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)