package com.yourname.smartpermissionanalyzer.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "spa_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val realTimeScan      = booleanPreferencesKey("real_time_scan")
        val scanFrequencyMin  = intPreferencesKey("scan_frequency_min")
        val aiSensitivity     = floatPreferencesKey("ai_sensitivity")   // 0.0 – 1.0
        val notifThreshold    = intPreferencesKey("notif_threshold")    // 40 .. 90
    }

    // region READ
    val realTimeScan: Flow<Boolean> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[Keys.realTimeScan] ?: true }

    val scanFrequency: Flow<Int> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[Keys.scanFrequencyMin] ?: 30 }

    val aiSensitivity: Flow<Float> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[Keys.aiSensitivity] ?: 0.7f }

    val notifThreshold: Flow<Int> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[Keys.notifThreshold] ?: 60 }
    // endregion

    // region WRITE
    suspend fun setRealTimeScan(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.realTimeScan] = enabled
        }
    }

    suspend fun setScanFrequency(min: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.scanFrequencyMin] = min.coerceIn(5, 720)
        }
    }

    suspend fun setAiSensitivity(value: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.aiSensitivity] = value.coerceIn(0f, 1f)
        }
    }

    suspend fun setNotifThreshold(score: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.notifThreshold] = score.coerceIn(0, 100)
        }
    }
    // endregion
}
