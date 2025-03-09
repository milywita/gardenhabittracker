package com.mily.gardenhabittracker.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.icu.util.Calendar
import android.util.Log
import java.util.Date
import java.util.concurrent.TimeUnit

// Utility class for tracking app usage using Android's UsageStatsManager

class UsageStatsTracker(private val context: Context) {
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    // Check if permission to access usage stats has been granted
    fun hasUsageStatsPermission(): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        // For Android Q (API 29) and above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            ) == AppOpsManager.MODE_ALLOWED
        }

        // Older versions
        else {
            try {
                // Using reflection as a fallback for older devices
                val method = appOpsManager.javaClass.getMethod(
                    "checkOpNoThrow",
                    Int::class.java,
                    Int::class.java,
                    String::class.java
                )

                val result = method.invoke(
                    appOpsManager,
                    43,
                    android.os.Process.myUid(),
                    context.packageName
                ) as Int

                return result == AppOpsManager.MODE_ALLOWED
            } catch (e: Exception) {
                // If reflection fails, try the permission-based approach
                return context.checkCallingOrSelfPermission(
                    android.Manifest.permission.PACKAGE_USAGE_STATS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        }
    }

    /**
     * Get usage time for a specific app on a given day
     * @param packageName The package name of the app to query
     * @param date The date for which to get usage statistics
     * @return Usage time in minutes, or 0 if no data available
     */

    fun getAppUsageForDay(packageName: String, date: Date): Int {
        if (usageStatsManager == null || !hasUsageStatsPermission()) return 0

        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        val startTime = calendar.timeInMillis

        calendar[Calendar.HOUR_OF_DAY] = 23
        calendar[Calendar.MINUTE] = 59
        calendar[Calendar.SECOND] = 59
        val endTime = calendar.timeInMillis

        try {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            ).toList()

            return stats
                .filter { it.packageName == packageName }
                .sumOf { it.totalTimeInForeground }
                .let { TimeUnit.MILLISECONDS.toMinutes(it).toInt() }
        } catch (e: Exception) {
            Log.e("UsageStatsTracker", "Error getting usage stats", e)
            return 0
        }
    }

    /**
     * Get real-time usage stats for today
     * @param packageName The package name to query
     * @return Usage time in minutes for today
     */

    fun getTodayUsage(packageName: String): Int{

        if (usageStatsManager == null || !hasUsageStatsPermission()) return 0

        val calendar = Calendar.getInstance()
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        try {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            ).toList() // Convert to immutable list

            return stats
                .filter { it.packageName == packageName }
                .sumOf { it.totalTimeInForeground }
                .let { TimeUnit.MILLISECONDS.toMinutes(it).toInt() }
        } catch (e: Exception) {
            Log.e("UsageStatsTracker", "Error getting today's usage stats", e)
            return 0
        }
    }
}