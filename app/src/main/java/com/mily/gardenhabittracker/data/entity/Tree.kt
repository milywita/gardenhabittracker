package com.mily.gardenhabittracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trees",
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

data class Tree(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    val growthStage: Int = 0, // 0-100 representing growth percentage
    val treeType: TreeType = TreeType.OAK,
    val positionX: Int = 0,
    val positionY: Int = 0,
    val isWilting: Boolean = false
)

enum class TreeType {
    OAK,
    PINE,
    MAPLE,
    CHERRY,
    WILLOW,
    BIRCH
}