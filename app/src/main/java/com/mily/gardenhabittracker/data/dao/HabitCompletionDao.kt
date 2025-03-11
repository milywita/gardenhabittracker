package com.mily.gardenhabittracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mily.gardenhabittracker.data.entity.HabitCompletion
import kotlinx.coroutines.flow.Flow
import java.util.Date

// Provides methods to interact with the habit_Completion table in the database
@Dao
interface HabitCompletionDao {
    // Insert new habit completion into the database
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletion)

    // Get all completion for a specific habit
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY date DESC")
    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>>

    // Get completion for a specific habit in a date range
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date BETWEEN :startDate AND :endDate ORDER BY date")
    fun getCompletionsInRange(habitId: Long, startDate: Date, endDate: Date): Flow<List<HabitCompletion>>

    // Get all completion for all habits on a specific date
    @Query("SELECT * FROM habit_completions WHERE date = :date")
    fun getCompletionsOnDate(date: Date): Flow<List<HabitCompletion>>

    // Check if a habit has been completed on a specific date
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun getCompletionForDate(habitId: Long, date: Date): HabitCompletion?

    // Get all completions for the past N days for a specific habit
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date >= :sinceDate ORDER BY date DESC")
    fun getRecentCompletions(habitId: Long, sinceDate: Date): Flow<List<HabitCompletion>>

    // Count completed days for a habit
    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId AND status = 'COMPLETED'")
    suspend fun countCompletedDays(habitId: Long): Int

    // Delete a completion
    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun deleteCompletion(habitId: Long, date: Date)

    // Get streak information (consecutive completed/skipped days)
    // This query gets the most recent completions to calculate the current streak
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND (status = 'COMPLETED' OR status = 'SKIPPED') ORDER BY date DESC")
    suspend fun getStreakCompletions(habitId: Long): List<HabitCompletion>
}