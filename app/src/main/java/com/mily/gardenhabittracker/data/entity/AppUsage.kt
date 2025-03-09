package com.mily.gardenhabittracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.Date

// Store & track application usage information
@Entity(tableName = "app_usage",
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

data class AppUsage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName : String,
    val usageDate : Date = Calendar.getInstance().time,
    val usageDurationMinutes: Int,
    val dailyLimitMinutes: Int,
    val habitId: Long, // Reference to the habit this usage is associated with
    val isTracker: Boolean = true

)