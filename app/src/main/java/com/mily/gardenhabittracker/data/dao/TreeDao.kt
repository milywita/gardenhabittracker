package com.mily.gardenhabittracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mily.gardenhabittracker.data.entity.Tree
import kotlinx.coroutines.flow.Flow

@Dao
interface TreeDao {

    // Insert new tree into database
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTree(tree: Tree): Long

    // Update existing tre in database
    @Update
    suspend fun updateTree(tree: Tree)

    // Get tree by ID
    @Query("SELECT * FROM trees WHERE id = :treeId")
    suspend fun getTreeById(treeId: Long): Tree?

    // Get tree by associated habit ID
    @Query("SELECT * FROM trees WHERE habitId = :habitId")
    suspend fun getTreeByHabitId(habitId: Long): Tree?

    // Get all trees in the garden as Flow
    @Query("SELECT * FROM trees ORDER BY id")
    fun getAllTrees(): Flow<List<Tree>>

    // Get tree count
    @Query("SELECT COUNT(*) FROM trees")
    suspend fun getTreeCount(): Int

    // Update growth stage of tree
    @Query("UPDATE trees SET growthStage = :growthStage WHERE habitId = :habitId")
    suspend fun updateGrowthStage(habitId: Long, growthStage: Int)

    // Update wilting status of tree
    @Query("UPDATE trees SET isWilting = :isWilting WHERE habitId = :habitId")
    suspend fun updateWiltingStatus(habitId: Long, isWilting: Boolean)

    // Delete a tree when habit is deleted
    @Query("DELETE FROM trees WHERE habitId = :habitId")
    suspend fun deleteTreeByHabitId(habitId: Long)
}