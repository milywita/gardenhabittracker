package com.mily.gardenhabittracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mily.gardenhabittracker.data.entity.AppUsage
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface AppUsageDao {
    // Insert a new app usage record into database
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsage(appUsage: AppUsage): Long

    // Update existing app usage record
    @Update
    suspend fun updateAppUsage(appUsage: AppUsage)

    // Get app usage for specific date, package name, and habit
    @Query("SELECT * FROM app_usage WHERE usageDate = :date AND packageName = :packageName AND habitId = :habitId")
    suspend fun getAppUsageForDate(habitId: Long, packageName: String, date: Date): AppUsage?

    // Get total usage time from specific app on specific day
    @Query("SELECT SUM(usageDurationMinutes) FROM app_usage WHERE packageName = :packageName AND usageDate = :date")
    suspend fun getTotalUsageForDate(packageName: String, date: Date): Int?

    // Get all usage history for specific habit
    @Query("SELECT * FROM app_usage WHERE habitId = :habitId ORDER BY usageDate DESC")
    fun getUsageHistoryForHabit(habitId: Long): Flow<List<AppUsage>>

    // Get usage data within a date range
    @Query("SELECT * FROM app_usage WHERE usageDate BETWEEN :startDate AND :endDate AND habitId = :habitId")
    fun getUsageInDateRange(habitId: Long, startDate: Date, endDate: Date): Flow<List<AppUsage>>

    // Calculate average use within a date range
    @Query("SELECT AVG(usageDurationMinutes) FROM app_usage WHERE habitId = :habitId AND usageDate BETWEEN :startDate AND :endDate")
    suspend fun getAverageUsageInDateRange(habitId: Long, startDate: Date, endDate: Date): Float?

    // Find the most recent date when user was within limits
    @Query("SELECT usageDate FROM app_usage WHERE habitId = :habitId AND usageDurationMinutes <= dailyLimitMinutes ORDER BY usageDate DESC LIMIT 1")
    suspend fun getLastSuccessfulDate(habitId: Long): Date?

    // Get all tracked apps
    @Query("SELECT DISTINCT packageName, habitId FROM app_usage WHERE isTracker = 1")
    suspend fun getAllTrackedApps(): List<TrackedApps>

    // Enable tracking for a specific app
    @Query("UPDATE app_usage SET isTracker = 1 WHERE packageName = :packageName AND habitId = :habitId")
    suspend fun enableTracking(packageName: String, habitId: Long)

    // Disable tracking for a specific app
    @Query("UPDATE app_usage SET isTracker = 0 WHERE packageName = :packageName AND habitId = :habitId")
    suspend fun disableTracking(packageName: String, habitId: Long)
}

data class TrackedApps(
    val packageName: String,
    val habitId: Long
)