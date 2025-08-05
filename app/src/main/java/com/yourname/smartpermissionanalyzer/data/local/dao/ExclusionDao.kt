package com.yourname.smartpermissionanalyzer.data.local.dao

import androidx.room.*
import com.yourname.smartpermissionanalyzer.data.local.entities.ExclusionEntityDb

@Dao
interface ExclusionDao {

    @Query("SELECT * FROM exclusion_list ORDER BY excludedAt DESC")
    suspend fun getAllExclusions(): List<ExclusionEntityDb>

    @Query("SELECT * FROM exclusion_list WHERE packageName = :packageName")
    suspend fun getExclusion(packageName: String): ExclusionEntityDb?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExclusion(exclusion: ExclusionEntityDb)

    @Query("DELETE FROM exclusion_list WHERE packageName = :packageName")
    suspend fun removeExclusion(packageName: String): Int

    @Query("DELETE FROM exclusion_list")
    suspend fun clearAllExclusions()

    @Query("SELECT packageName FROM exclusion_list")
    suspend fun getExcludedPackageNames(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM exclusion_list WHERE packageName = :packageName)")
    suspend fun isPackageExcluded(packageName: String): Boolean
}
