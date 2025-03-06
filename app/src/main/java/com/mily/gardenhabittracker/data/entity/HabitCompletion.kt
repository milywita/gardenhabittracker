package com.mily.gardenhabittracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitId")]
)

data class HabitCompletion(
    val habitId: Long,
    val date: Date,
    val status: CompletionStatus = CompletionStatus.COMPLETED
)

enum class CompletionStatus {
    COMPLETED,
    SKIPPED,
    MISSED
}