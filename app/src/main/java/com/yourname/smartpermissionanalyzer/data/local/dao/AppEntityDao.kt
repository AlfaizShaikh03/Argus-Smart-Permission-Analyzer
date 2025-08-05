package com.yourname.smartpermissionanalyzer.data.local.dao

import androidx.room.*
import com.yourname.smartpermissionanalyzer.data.local.entities.AppEntityDb
import kotlinx.coroutines.flow.Flow

@Dao
interface AppEntityDao {

    @Query("SELECT * FROM app_entities ORDER BY appName ASC")
    suspend fun getAllApps(): List<AppEntityDb>

    @Query("SELECT * FROM app_entities ORDER BY appName ASC")
    fun observeAllApps(): Flow<List<AppEntityDb>>

    @Query("SELECT * FROM app_entities WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppByPackageName(packageName: String): AppEntityDb?

    @Query("SELECT * FROM app_entities WHERE riskScore >= :minRiskScore ORDER BY riskScore DESC")
    suspend fun getHighRiskApps(minRiskScore: Int): List<AppEntityDb>

    @Query("SELECT * FROM app_entities WHERE appCategory = :category ORDER BY appName ASC")
    suspend fun getAppsByCategory(category: String): List<AppEntityDb>

    @Query("SELECT * FROM app_entities WHERE appName LIKE :query OR packageName LIKE :query ORDER BY appName ASC")
    suspend fun searchApps(query: String): List<AppEntityDb>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppEntityDb)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<AppEntityDb>)

    @Update
    suspend fun updateApp(app: AppEntityDb)

    @Delete
    suspend fun deleteApp(app: AppEntityDb)

    // ✅ ENHANCED: Better delete function with return count
    @Query("DELETE FROM app_entities WHERE packageName = :packageName")
    suspend fun deleteAppByPackageName(packageName: String): Int

    @Query("DELETE FROM app_entities")
    suspend fun deleteAllApps()

    @Query("SELECT COUNT(*) FROM app_entities")
    suspend fun getAppCount(): Int

    @Query("SELECT COUNT(*) FROM app_entities WHERE riskScore >= :minRiskScore")
    suspend fun getHighRiskAppCount(minRiskScore: Int): Int

    // ✅ NEW: Additional utility functions
    @Query("SELECT EXISTS(SELECT 1 FROM app_entities WHERE packageName = :packageName)")
    suspend fun appExists(packageName: String): Boolean

    @Query("SELECT packageName FROM app_entities")
    suspend fun getAllPackageNames(): List<String>
}
