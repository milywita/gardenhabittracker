package com.mily.gardenhabittracker.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mily.gardenhabittracker.data.database.AppDatabase
import com.mily.gardenhabittracker.data.entity.AppUsage
import com.mily.gardenhabittracker.data.entity.CompletionStatus
import com.mily.gardenhabittracker.data.entity.Habit
import com.mily.gardenhabittracker.data.entity.HabitCompletion
import com.mily.gardenhabittracker.data.entity.Tree
import com.mily.gardenhabittracker.data.entity.TreeType
import com.mily.gardenhabittracker.data.usage.UsageStatsTracker
import com.mily.gardenhabittracker.ui.theme.GardenhabittrackerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class UsageTestState(
    val youtubeUsage: String,
    val trackedAppsCount: String,
    val lastUpdateTime: String,
    val autoRefreshEnabled: Boolean
)

data class UsageTestEvents(
    val onRequestPermission: () -> Unit,
    val onRefreshUsage: () -> Unit,
    val onClearTestData: () -> Unit,
    val onToggleAutoRefresh: (Boolean) -> Unit
)

class MainActivity : ComponentActivity() {
    companion object {
        private const val TIME_FORMAT_PATTERN = "HH:mm:ss"
        private const val TAG = "TEST"
    }

    // tracking results for display in UI
    private val youtubeUsage = mutableStateOf("Not checked yet")
    private val trackedAppsCount = mutableStateOf("0")
    private val lastUpdateTime = mutableStateOf("Never")
    private val autoRefreshEnabled = mutableStateOf(false)

    // Coroutine job for auto-refresh
    private var autoRefreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Test code
        testComponents()

        enableEdgeToEdge()
        setContent {
            GardenhabittrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Create the state and events objects
                    val state = UsageTestState(
                        youtubeUsage = youtubeUsage.value,
                        trackedAppsCount = trackedAppsCount.value,
                        lastUpdateTime = lastUpdateTime.value,
                        autoRefreshEnabled = autoRefreshEnabled.value
                    )

                    val events = UsageTestEvents(
                        onRequestPermission = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            startActivity(intent)
                        },
                        onRefreshUsage = { refreshUsageData() },
                        onClearTestData = { clearTestData() },
                        onToggleAutoRefresh = { toggleAutoRefresh(it) }
                    )

                    TestScreen(
                        modifier = Modifier.padding(innerPadding),
                        state = state,
                        events = events
                    )

                    // Lifecycle management for auto-refresh
                    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_PAUSE) {
                                stopAutoRefresh()
                            } else if (event == Lifecycle.Event.ON_RESUME && autoRefreshEnabled.value) {
                                startAutoRefresh()
                            }
                        }

                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                            stopAutoRefresh()
                        }
                    }
                }
            }
        }
    }

    private fun toggleAutoRefresh(enabled: Boolean) {
        autoRefreshEnabled.value = enabled
        if (enabled) {
            startAutoRefresh()
        } else {
            stopAutoRefresh()
        }
    }

    private fun startAutoRefresh() {
        stopAutoRefresh() // Stop any existing job first

        autoRefreshJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                refreshUsageData()
                delay(60000) // 60 seconds = 1 minute
            }
        }
        Log.d(TAG, "Auto-refresh started")
    }

    private fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
        Log.d(TAG, "Auto-refresh stopped")
    }

    private fun refreshUsageData() {
        val scope = CoroutineScope(Dispatchers.IO)
        val usageTracker = UsageStatsTracker(this)

        if (!usageTracker.hasUsageStatsPermission()) {
            youtubeUsage.value = "No permission to check usage"
            return
        }

        scope.launch {
            try {
                // Get YouTube usage
                val youtubePackage = "com.google.android.youtube"
                val usage = usageTracker.getTodayUsage(youtubePackage)
                youtubeUsage.value = "$usage minutes"

                // Get tracked apps count
                val database = AppDatabase.getDatabase(this@MainActivity)
                val appUsageDao = database.appUsageDao()
                val trackedApps = appUsageDao.getAllTrackedApps()
                trackedAppsCount.value = trackedApps.size.toString()

                // Update timestamp
                val dateFormat = SimpleDateFormat(TIME_FORMAT_PATTERN, Locale.getDefault())
                lastUpdateTime.value = dateFormat.format(Date())

                // Log to console
                Log.d(TAG, "Refreshed at ${lastUpdateTime.value}: YouTube usage today: $usage minutes")
                Log.d(TAG, "Tracked apps count: ${trackedApps.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing usage data", e)
                youtubeUsage.value = "Error: ${e.message}"
            }
        }
    }

    private fun clearTestData() {
        val scope = CoroutineScope(Dispatchers.IO)
        val database = AppDatabase.getDatabase(this)

        scope.launch {
            try {
                // Use raw queries to delete all data from tables
                database.clearAllTables()

                Log.d(TAG, "Cleared all test data from database")
                youtubeUsage.value = "Data cleared"
                trackedAppsCount.value = "0"

                // Update timestamp
                val dateFormat = SimpleDateFormat(TIME_FORMAT_PATTERN, Locale.getDefault())
                lastUpdateTime.value = dateFormat.format(Date()) + " (cleared)"
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing test data", e)
            }
        }
    }

    private fun testComponents() {
        // Test coroutine scope
        val scope = CoroutineScope(Dispatchers.IO)

        // Get database
        val database = AppDatabase.getDatabase(this)

        Log.d(TAG, "Starting comprehensive tests")

        // Test database operations independently
        scope.launch {
            try {
                // 1. Test basic database operations
                testDatabaseOperations(database)

                // 2. Check if we have usage stats permission
                val usageTracker = UsageStatsTracker(this@MainActivity)
                val hasPermission = usageTracker.hasUsageStatsPermission()
                Log.d(TAG, "Has usage stats permission: $hasPermission")

                // 3. If we have permission, test usage stats
                if (hasPermission) {
                    testUsageStats(database, usageTracker)
                } else {
                    // Just log the need for permission but don't immediately launch settings
                    Log.d(TAG, "Usage stats permission not granted. Only database tests were performed.")
                }

                Log.d(TAG, "Testing completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error during testing", e)
                e.printStackTrace()
            }
        }
    }

    private suspend fun testDatabaseOperations(database: AppDatabase) {
        Log.d(TAG, "Testing database operations")

        // Test HabitDao
        val habitDao = database.habitDao()

        // Count existing habits
        val initialHabitCount = habitDao.getHabitCount()
        Log.d(TAG, "Initial habit count: $initialHabitCount")

        // Insert a test habit
        val habit = Habit(
            name = "Test Habit",
            description = "Database test habit",
            color = "#00FF00"
        )
        val habitId = habitDao.insertHabit(habit)
        Log.d(TAG, "Inserted test habit with ID: $habitId")

        // Verify habit was inserted
        val newHabitCount = habitDao.getHabitCount()
        Log.d(TAG, "Habit count after insert: $newHabitCount")

        // Test retrieving habit by ID
        val retrievedHabit = habitDao.getHabitById(habitId)
        Log.d(TAG, "Retrieved habit by ID: ${retrievedHabit?.name}")

        // Test HabitCompletionDao
        val completionDao = database.habitCompletionDao()

        // Insert a completion
        val today = Calendar.getInstance().time
        val completion = HabitCompletion(
            habitId = habitId,
            date = today,
            status = CompletionStatus.COMPLETED
        )
        completionDao.insertCompletion(completion)
        Log.d(TAG, "Inserted habit completion for today")

        // Verify completion was inserted
        val retrievedCompletion = completionDao.getCompletionForDate(habitId, today)
        Log.d(TAG, "Retrieved completion: ${retrievedCompletion?.status}")

        // Test GardenDao
        val gardenDao = database.gardenDao()
        gardenDao.initializeGardenIfNeeded()
        Log.d(TAG, "Initialized garden if needed")

        // Test TreeDao
        val treeDao = database.treeDao()
        val tree = Tree(
            habitId = habitId,
            treeType = TreeType.MAPLE
        )
        val treeId = treeDao.insertTree(tree)
        Log.d(TAG, "Inserted tree with ID: $treeId")

        // Test AppUsageDao
        val appUsageDao = database.appUsageDao()
        val appUsage = AppUsage(
            packageName = "com.example.test",
            appName = "Test App",
            usageDurationMinutes = 30,
            dailyLimitMinutes = 60,
            habitId = habitId
        )
        val appUsageId = appUsageDao.insertAppUsage(appUsage)
        Log.d(TAG, "Inserted app usage record with ID: $appUsageId")

        Log.d(TAG, "Database operations test completed successfully")
    }

    private suspend fun testUsageStats(database: AppDatabase, usageTracker: UsageStatsTracker) {
        Log.d(TAG, "Testing usage stats")

        // Get YouTube package usage
        val youtubePackage = "com.google.android.youtube"
        val usage = usageTracker.getTodayUsage(youtubePackage)
        Log.d(TAG, "Today's usage for $youtubePackage: $usage minutes")
        youtubeUsage.value = "$usage minutes"

        // Create a habit for tracking YouTube
        val habitDao = database.habitDao()
        val habit = Habit(
            name = "Limit YouTube Usage",
            description = "Spend less time on YouTube",
            color = "#FF0000"
        )
        val habitId = habitDao.insertHabit(habit)
        Log.d(TAG, "Created YouTube tracking habit with ID: $habitId")

        // Create usage record
        val appUsageDao = database.appUsageDao()
        val appUsage = AppUsage(
            packageName = youtubePackage,
            appName = "YouTube",
            usageDurationMinutes = usage,
            dailyLimitMinutes = 60,
            habitId = habitId,
            isTracker = true
        )
        val usageId = appUsageDao.insertAppUsage(appUsage)
        Log.d(TAG, "Created app usage record for YouTube with ID: $usageId")

        // Verify we can retrieve tracked apps
        val trackedApps = appUsageDao.getAllTrackedApps()
        Log.d(TAG, "Tracked apps: ${trackedApps.size} apps")
        trackedAppsCount.value = trackedApps.size.toString()

        trackedApps.forEach {
            Log.d(TAG, "Tracked app: ${it.packageName} for habit ${it.habitId}")
        }

        // Update last update time
        val dateFormat = SimpleDateFormat(TIME_FORMAT_PATTERN, Locale.getDefault())
        lastUpdateTime.value = dateFormat.format(Date())

        Log.d(TAG, "Usage stats test completed successfully")
    }
}

@Composable
fun TestScreen(
    modifier: Modifier = Modifier,
    state: UsageTestState,
    events: UsageTestEvents
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "App Usage Tracking Test",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        // Last update time
        Text(
            text = "Last updated: ${state.lastUpdateTime}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

        // Display current YouTube usage
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "YouTube Usage Today:",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = state.youtubeUsage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Display tracked apps count
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tracked Apps:",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = state.trackedAppsCount,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Auto-refresh toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Auto-refresh (1 minute):",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = state.autoRefreshEnabled,
                onCheckedChange = events.onToggleAutoRefresh
            )
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = events.onRefreshUsage) {
                Text("Refresh Now")
            }

            Button(onClick = events.onClearTestData) {
                Text("Clear Data")
            }
        }

        Button(onClick = events.onRequestPermission) {
            Text("Usage Access Settings")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Check Logcat for detailed results (filter by 'TEST' tag)",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outline
        )
    }
}