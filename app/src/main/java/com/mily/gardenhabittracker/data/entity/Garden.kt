package com.mily.gardenhabittracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "garden")
data class Garden(
    @PrimaryKey
    val id: Long = 1,
    val capacity: Int = 5,
    val name: String = "My Garden",
    val unlockProgress: Int = 0
)