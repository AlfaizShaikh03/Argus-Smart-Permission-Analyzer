package com.yourname.smartpermissionanalyzer.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.yourname.smartpermissionanalyzer.data.database.entities.ScanEventDbEntity

@Dao
interface ScanEventDao {

    @Query("SELECT * FROM scan_events ORDER BY timestamp DESC")
    fun getAllScanEvents(): Flow<List<ScanEventDbEntity>>

    @Query("SELECT * FROM scan_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentScanEvents(limit: Int): Flow<List<ScanEventDbEntity>>

    @Query("SELECT * FROM scan_events WHERE timestamp >= :fromTime ORDER BY timestamp DESC")
    fun getScanEventsSince(fromTime: Long): Flow<List<ScanEventDbEntity>>

    @Insert
    suspend fun insertScanEvent(event: ScanEventDbEntity)

    @Query("DELETE FROM scan_events WHERE timestamp < :beforeTime")
    suspend fun deleteOldScanEvents(beforeTime: Long)

    @Query("SELECT * FROM scan_events ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastScanEvent(): ScanEventDbEntity?
}
