package com.example.acoustictxrx.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransmissionLogDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: TransmissionLog)

    @Query("SELECT * FROM transmission_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<TransmissionLog>>
}