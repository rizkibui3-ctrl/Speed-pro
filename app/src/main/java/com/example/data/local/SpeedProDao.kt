package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedProDao {
    @Query("SELECT * FROM trips ORDER BY endedAtMillis DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteTripById(id: Long)

    @Query("DELETE FROM trips")
    suspend fun clearAllTrips()

    @Query("SELECT * FROM acceleration_runs ORDER BY timestampMillis DESC")
    fun getAllAccelerationRuns(): Flow<List<AccelerationRunEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccelerationRun(run: AccelerationRunEntity): Long

    @Query("DELETE FROM acceleration_runs WHERE id = :id")
    suspend fun deleteAccelerationRunById(id: Long)

    @Query("DELETE FROM acceleration_runs")
    suspend fun clearAllAccelerationRuns()
}
