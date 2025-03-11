package com.mily.gardenhabittracker.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mily.gardenhabittracker.data.database.AppDatabase
import com.mily.gardenhabittracker.data.entity.AppUsage
import com.mily.gardenhabittracker.data.repository.AppUsageRepository
import com.mily.gardenhabittracker.data.usage.UsageStatsTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar


// ViewModel for app usage tracking functionality
class AppUsageViewModel(application: Application) : AndroidViewModel(application) {

    private val usageStatsTracker = UsageStatsTracker(application)
    private val database = AppDatabase.getDatabase(application)
    private val appUsageDao = database.appUsageDao()

    // Create repository for app usage data
    private val appUsageRepository = AppUsageRepository(appUsageDao, usageStatsTracker)

    // Permission check state
    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    // App usage state
    private val _appUsageState = MutableStateFlow(AppUsageState())
    val appUsageState: StateFlow<AppUsageState> = _appUsageState.asStateFlow()

    init {
        // Check permission initially
        checkPermission()

        // Load app usage data if permission is granted
        if (_hasPermission.value) {
            loadAppUsageData()
        }
    }


    // Check if the app has permission to access usage stats
    fun checkPermission() {
        _hasPermission.value = usageStatsTracker.hasUsageStatsPermission()
    }


    // Load app usage data
    fun loadAppUsageData() {
        viewModelScope.launch {
            // Set loading state
            _appUsageState.value = AppUsageState(isLoading = true)

            try {
                // Get tracked app ids
                val trackedApps = appUsageRepository.getAllTrackedApps()

                if (trackedApps.isEmpty()) {
                    // No apps being tracked
                    _appUsageState.value = AppUsageState(isLoading = false)
                    return@launch
                }

                // Get today's date
                val today = Calendar.getInstance().time

                // Collect the latest data for each app
                val appUsageList = mutableListOf<AppUsage>()

                for (app in trackedApps) {
                    try {
                        // Update today's usage for the app first
                        appUsageRepository.updateTodayUsage(
                            app.habitId,
                            app.packageName,
                            "App ${app.packageName.split('.').last()}", // Simple app name for now
                            60 // Default 60 minute limit
                        )

                        // Get the latest usage data for this app
                        val appUsage = appUsageRepository.getAppUsageForDate(
                            app.habitId,
                            app.packageName,
                            today
                        )

                        if (appUsage != null) {
                            appUsageList.add(appUsage)
                        }
                    } catch (e: Exception) {
                        // Log error
                        e.printStackTrace()
                    }
                }

                // Update state with loaded data
                _appUsageState.value = AppUsageState(
                    isLoading = false,
                    appUsageList = appUsageList
                )
            } catch (e: Exception) {
                // Update state with error
                _appUsageState.value = AppUsageState(
                    isLoading = false,
                    error = e.message ?: "Unknown error loading app usage data"
                )
                e.printStackTrace()
            }
        }
    }


    // Add a new app to track
    fun addAppToTrack(packageName: String, habitId: Long, dailyLimitMinutes: Int) {
        viewModelScope.launch {
            try {
                // Get app info
                val appName = try {
                    val packageManager = getApplication<Application>().packageManager
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    packageName.split('.').last() // Fallback to package name if app not found
                }

                // Create a new app usage record
                val appUsage = AppUsage(
                    packageName = packageName,
                    appName = appName,
                    usageDurationMinutes = usageStatsTracker.getTodayUsage(packageName),
                    dailyLimitMinutes = dailyLimitMinutes,
                    habitId = habitId,
                    isTracker = true
                )

                // Insert into database
                appUsageDao.insertAppUsage(appUsage)

                // Reload data
                loadAppUsageData()
            } catch (e: Exception) {
                // Handle error
                _appUsageState.value = AppUsageState(
                    isLoading = false,
                    error = e.message ?: "Error adding app to track"
                )
                e.printStackTrace()
            }
        }
    }


    // Refresh usage data
    fun refreshUsageData() {
        checkPermission()
        if (_hasPermission.value) {
            loadAppUsageData()
        }
    }
}


// State holder for app usage data
data class AppUsageState(
    val isLoading: Boolean = false,
    val appUsageList: List<AppUsage> = emptyList(),
    val error: String? = null
)