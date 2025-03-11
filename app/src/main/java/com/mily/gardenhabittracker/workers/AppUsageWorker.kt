package com.mily.gardenhabittracker.workers

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mily.gardenhabittracker.data.database.AppDatabase
import com.mily.gardenhabittracker.data.dao.TrackedApps
import com.mily.gardenhabittracker.data.entity.AppUsage
import com.mily.gardenhabittracker.data.usage.UsageStatsTracker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

/**
 * WorkManager Worker for collecting app usage statistics in the background
 * This worker runs periodically to update app usage data in the database
 */
class AppUsageWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : CoroutineWorker(appContext, workerParams) {

    private val usageStatsTracker = UsageStatsTracker(appContext)
    private val database = AppDatabase.getDatabase(appContext)
    private val appUsageDao = database.appUsageDao()
    private val habitDao = database.habitDao()
    private val packageManager = appContext.packageManager

    override suspend fun doWork(): Result = withContext(dispatcher) {
        try {
            Log.d(TAG, "Starting app usage data collection")

            // Check permission and tracked apps
            if (!checkPrerequisites()) {
                return@withContext Result.failure()
            }

            // Process tracked apps
            processTrackedApps()

            Log.d(TAG, "Completed app usage data collection")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in app usage worker", e)
            Result.failure()
        }
    }

    /**
     * Check if prerequisites are met (permissions and tracked apps)
     * @return true if prerequisites are met, false otherwise
     */
    private suspend fun checkPrerequisites(): Boolean {
        // Check for usage stats permission
        if (!usageStatsTracker.hasUsageStatsPermission()) {
            Log.e(TAG, "No usage stats permission, cannot collect data")
            return false
        }

        // Check if there are any apps to track
        val trackedApps = appUsageDao.getAllTrackedApps()
        if (trackedApps.isEmpty()) {
            Log.d(TAG, "No apps are being tracked")
            return false
        }

        return true
    }

    /**
     * Process all tracked apps to update their usage data
     */
    private suspend fun processTrackedApps() {
        val trackedApps = appUsageDao.getAllTrackedApps()
        val today = Calendar.getInstance().time

        for (trackedApp in trackedApps) {
            try {
                processApp(trackedApp, today)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating usage for ${trackedApp.packageName}", e)
                // Continue with other apps even if one fails
            }
        }
    }

    /**
     * Process a single app to update its usage data
     * @param trackedApp The app to process
     * @param today The current date
     */
    private suspend fun processApp(trackedApp: TrackedApps, today: Date) {
        // Get app info
        val appName = getAppName(trackedApp.packageName)

        // Get today's usage for this app
        val usageMinutes = usageStatsTracker.getTodayUsage(trackedApp.packageName)
        Log.d(TAG, "Usage for $appName: $usageMinutes minutes")

        // Update or create usage record
        updateAppUsageRecord(trackedApp, appName, usageMinutes, today)
    }

    /**
     * Get the app name from its package name
     * @param packageName The package name
     * @return The app name or package name if not found
     */
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName // Fallback to package name if app not found
        }
    }

    /**
     * Update or create usage record for an app
     * @param trackedApp The app to update
     * @param appName The app name
     * @param usageMinutes The usage minutes
     * @param date The current date
     */
    private suspend fun updateAppUsageRecord(
        trackedApp: TrackedApps,
        appName: String,
        usageMinutes: Int,
        date: Date
    ) {
        // Check if we already have a record for today
        val existingRecord = appUsageDao.getAppUsageForDate(
            trackedApp.habitId,
            trackedApp.packageName,
            date
        )

        if (existingRecord != null) {
            updateExistingRecord(existingRecord, usageMinutes, appName)
        } else {
            createNewRecord(trackedApp, appName, usageMinutes, date)
        }
    }

    /**
     * Update an existing app usage record
     * @param existingRecord The existing record
     * @param usageMinutes The new usage minutes
     * @param appName The app name for logging
     */
    private suspend fun updateExistingRecord(
        existingRecord: AppUsage,
        usageMinutes: Int,
        appName: String
    ) {
        val updatedRecord = existingRecord.copy(
            usageDurationMinutes = usageMinutes
        )
        appUsageDao.updateAppUsage(updatedRecord)
        Log.d(TAG, "Updated existing record for $appName")

        // Check if usage is within limits
        val withinLimit = updatedRecord.usageDurationMinutes <= updatedRecord.dailyLimitMinutes
        Log.d(TAG, "Usage within limit: $withinLimit")
    }

    /**
     * Create a new app usage record
     * @param trackedApp The app to create a record for
     * @param appName The app name
     * @param usageMinutes The usage minutes
     * @param date The current date
     */
    private suspend fun createNewRecord(
        trackedApp: TrackedApps,
        appName: String,
        usageMinutes: Int,
        date: Date
    ) {
        // Create new record - we need to get the habit to find out the daily limit
        val habit = habitDao.getHabitById(trackedApp.habitId)
        if (habit != null) {
            // Using a default limit of 60 minutes if not specified elsewhere
            val dailyLimit = 60

            val newRecord = AppUsage(
                packageName = trackedApp.packageName,
                appName = appName,
                usageDate = date,
                usageDurationMinutes = usageMinutes,
                dailyLimitMinutes = dailyLimit,
                habitId = trackedApp.habitId,
                isTracker = true
            )
            appUsageDao.insertAppUsage(newRecord)
            Log.d(TAG, "Created new record for $appName")
        } else {
            Log.e(TAG, "Could not find habit with ID ${trackedApp.habitId}")
        }
    }

    companion object {
        private const val TAG = "AppUsageWorker"
    }
}