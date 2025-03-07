package com.mily.gardenhabittracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mily.gardenhabittracker.data.entity.Habit
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface HabitDao {
    // Add new habit into the database
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    // Update existing habit in the database
    @Update
    suspend fun updateHabit(habit: Habit)

    // Delete habit from the database
    suspend fun deleteHabit(habit: Habit)

    // Get habit by its ID
    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getHabitById(habitId: Long): Habit?

    // Get all habits as a Flow
    @Query("SELECT * FROM habits ORDER BY createDate DESC")
    fun getAllHabits(): Flow<List<Habit>>

    // Get count of all habits
    @Query("SELECT COUNT(*) FROM habits")
    suspend fun getHabitCount(): Int

    // Get habits created on ar after a specific date
    @Query("SELECT * FROM habits WHERE createDate >= :date")
    fun getHabitsCreatedSince(date: Date): Flow<List<Habit>>


}