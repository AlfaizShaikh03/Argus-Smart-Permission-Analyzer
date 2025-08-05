package com.yourname.smartpermissionanalyzer.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.yourname.smartpermissionanalyzer.data.database.entities.LearningDataDbEntity

@Dao
interface LearningDataDao {

    @Query("SELECT * FROM learning_data ORDER BY timestamp DESC")
    fun getAllLearningData(): Flow<List<LearningDataDbEntity>>

    @Query("SELECT * FROM learning_data WHERE package_name = :packageName")
    suspend fun getLearningDataForApp(packageName: String): LearningDataDbEntity?

    @Query("SELECT * FROM learning_data WHERE app_category = :category")
    fun getLearningDataByCategory(category: String): Flow<List<LearningDataDbEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearningData(data: LearningDataDbEntity)

    @Update
    suspend fun updateLearningData(data: LearningDataDbEntity)

    @Delete
    suspend fun deleteLearningData(data: LearningDataDbEntity)

    @Query("DELETE FROM learning_data WHERE timestamp < :beforeTime")
    suspend fun deleteOldLearningData(beforeTime: Long)

    @Query("SELECT COUNT(*) FROM learning_data WHERE user_flagged = :flagged")
    suspend fun getCountByFlaggedStatus(flagged: Boolean): Int

    @Query("DELETE FROM learning_data")
    suspend fun deleteAllLearningData()
}
