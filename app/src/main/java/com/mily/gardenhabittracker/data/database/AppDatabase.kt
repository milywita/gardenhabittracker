package com.mily.gardenhabittracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mily.gardenhabittracker.data.dao.AppUsageDao
import com.mily.gardenhabittracker.data.dao.GardenDao
import com.mily.gardenhabittracker.data.dao.HabitCompletionDao
import com.mily.gardenhabittracker.data.dao.HabitDao
import com.mily.gardenhabittracker.data.dao.TreeDao
import com.mily.gardenhabittracker.data.entity.AppUsage
import com.mily.gardenhabittracker.data.entity.Garden
import com.mily.gardenhabittracker.data.entity.Habit
import com.mily.gardenhabittracker.data.entity.HabitCompletion
import com.mily.gardenhabittracker.data.entity.Tree

// Room database

@Database(
    entities = [
        Habit::class,
        HabitCompletion::class,
        Garden::class,
        Tree::class,
        AppUsage::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // Get the DAO for Habits
    abstract fun habitDao(): HabitDao

    // Get the DAO for HabitCompletions
    abstract fun habitCompletionDao(): HabitCompletionDao

    // Get the DAO for Garden
    abstract fun gardenDao(): GardenDao

    // Get the DAO for Trees
    abstract fun treeDao(): TreeDao

    // Get the DAO for AppUsage
    abstract fun appUsageDao(): AppUsageDao

    companion object {
        // Singleton instance of the database
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Get the database instance, creating it if it doesn't exist
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "garden_habit_tracker_database"
                )
                    .fallbackToDestructiveMigration() // Wipe and rebuild the database if no migration path exists
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}