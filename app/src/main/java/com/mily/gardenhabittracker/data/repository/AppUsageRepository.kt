package com.mily.gardenhabittracker.data.repository

import com.mily.gardenhabittracker.data.dao.AppUsageDao
import com.mily.gardenhabittracker.data.dao.TrackedApps
import com.mily.gardenhabittracker.data.entity.AppUsage
import com.mily.gardenhabittracker.data.usage.UsageStatsTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date


// Repo for app usage data, provides API to the rest of the app
class AppUsageRepository(
    private val appUsageDao: AppUsageDao,
    private val usageStatsTracker: UsageStatsTracker
) {
    // Check if the app has permission to access usage stats
    fun hasUsageStatsPermission(): Boolean {
        return usageStatsTracker.hasUsageStatsPermission()
    }

    // Get app usage for a specific habit, app and date
    suspend fun getAppUsageForDate(habitId: Long, packageName: String, date: Date): AppUsage? {
        return appUsageDao.getAppUsageForDate(habitId, packageName, date)
    }

    // Get all usage history for a specific habit
    fun getUsageHistoryForHabit(habitId: Long): Flow<List<AppUsage>> {
        return appUsageDao.getUsageHistoryForHabit(habitId)
    }

    // Get usage data within a date range for a specific habit
    fun getUsageInDateRange(habitId: Long, startDate: Date, endDate: Date): Flow<List<AppUsage>> {
        return appUsageDao.getUsageInDateRange(habitId, startDate, endDate)
    }

    // Update today's usage record for a specific app
    suspend fun updateTodayUsage(
        habitId: Long,
        packageName: String,
        appName: String,
        dailyLimitMinutes: Int
    ) {
        val today = Calendar.getInstance().time
        val usage = usageStatsTracker.getTodayUsage(packageName)

        // Check if we already have a record for today
        val existingRecord = appUsageDao.getAppUsageForDate(habitId, packageName, today)

        if (existingRecord != null) {
            val updateRecord = existingRecord.copy(usageDurationMinutes = usage)
            appUsageDao.updateAppUsage(updateRecord)
        } else {
            // Create new record
            val newRecord = AppUsage(
                packageName = packageName,
                appName = appName,
                usageDate = today,
                usageDurationMinutes = usage,
                dailyLimitMinutes = dailyLimitMinutes,
                habitId = habitId
            )
            appUsageDao.insertAppUsage(newRecord)
        }
    }

    // Check if usage is within limit for today
    suspend fun isUsageWithinLimit(habitId: Long, packageName: String):
            Boolean {
        val today = Calendar.getInstance().time
        val record = appUsageDao.getAppUsageForDate(habitId, packageName, today) ?: return true
        return record.usageDurationMinutes <= record.dailyLimitMinutes
    }

    // Get all tracked apps
    suspend fun getAllTrackedApps(): List<TrackedApps> {
        return appUsageDao.getAllTrackedApps()
    }

    // Enable tracking for a specific app
    suspend fun enableTracking(packageName: String, habitId: Long) {
        appUsageDao.enableTracking(packageName, habitId)
    }

    // Disable tracking for a specific app
    suspend fun disableTracking(packageName: String, habitId: Long) {
        appUsageDao.disableTracking(packageName, habitId)
    }

    // Get the current streak (days within limit) for a specific habit
    suspend fun calculateCurrentStreak(habitId: Long, packageName: String): Int {
        // Get all usage records for this habit
        val records = appUsageDao.getUsageHistoryForHabit(habitId).first()

        // Filter records for the specific package and sort by date (newest first)
        val filteredRecords = records
            .filter { it.packageName == packageName }
            .sortedByDescending { it.usageDate }

        if (filteredRecords.isEmpty()) return 0

        var streak = 0

        // Start from most recent record and count backward until we find a day
        // where usage was not within the limit

        for (record in filteredRecords) {
            if (record.usageDurationMinutes <= record.dailyLimitMinutes) {
                streak++
            } else {
                break
            }
        }
        return streak
    }
}