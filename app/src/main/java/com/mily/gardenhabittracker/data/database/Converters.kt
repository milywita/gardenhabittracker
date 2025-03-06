package com.mily.gardenhabittracker.data.database

import androidx.room.TypeConverter
import java.util.Date

/**
 * Type converters for Room database to handle custom types
 * Room can only store primitive types, so we need to convert our custom types
 * to and from types that Room can store
 */

class Converters {


    // Converts Date to Long timestamp for storage in database
    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    // Converts Long timestamp back to Date when reading from database
    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }

    // Converts CompletionStatus enum to String for storage
    @TypeConverter
    fun fromCompletionStatus(status: com.mily.gardenhabittracker.data.entity.CompletionStatus): String {
        return status.name
    }

    // Converts String back to CompletionStatus enum
    @TypeConverter
    fun toCompletionStatus(statusString: String): com.mily.gardenhabittracker.data.entity.CompletionStatus {
        return com.mily.gardenhabittracker.data.entity.CompletionStatus.valueOf(statusString)
    }

    // Converts TreeType enum to String for storage
    @TypeConverter
    fun fromTreeType(treeType: com.mily.gardenhabittracker.data.entity.TreeType): String {
        return treeType.name
    }

    // Converts String back to TreeType enum
    @TypeConverter
    fun toTreeType(treeTypeString: String): com.mily.gardenhabittracker.data.entity.TreeType {
        return com.mily.gardenhabittracker.data.entity.TreeType.valueOf(treeTypeString)
    }
}