package com.mily.gardenhabittracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.Date

@Entity (tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val color: String = "#4CAF50",
    val createDate: Date = Calendar.getInstance().time,
    val iconEmoji: String =  "🌱"
)
