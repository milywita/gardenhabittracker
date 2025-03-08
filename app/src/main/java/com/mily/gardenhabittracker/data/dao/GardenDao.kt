package com.mily.gardenhabittracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mily.gardenhabittracker.data.entity.Garden
import kotlinx.coroutines.flow.Flow

@Dao
interface GardenDao{
    // Get users garden
    @Query("SELECT * FROM garden WHERE id = 1")
    fun getGarden(): Flow<Garden?>

    // Insert garden
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGarden(garden: Garden)

    // Update garden
    @Update
    suspend fun updateGarden(garden: Garden)

    // Increase garden capacity by the given amount
    @Query("UPDATE garden SET capacity = capacity + :amount WHERE id = 1")
    suspend fun increaseCapacity(amount: Int)

    // Update unlock progress
    @Query("UPDATE garden SET unlockProgress = :progress WHERE id = 1")
    suspend fun updateUnlockProgress(progress: Int)

    // Initialize garden if it doesn't exist
    @Query("INSERT OR IGNORE INTO garden (id, capacity, name, unlockProgress) VALUES (1, 5, 'My Garden', 0)")
    suspend fun initializeGardenIfNeeded()

}