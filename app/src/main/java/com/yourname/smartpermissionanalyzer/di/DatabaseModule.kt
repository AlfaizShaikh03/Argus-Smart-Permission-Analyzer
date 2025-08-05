package com.yourname.smartpermissionanalyzer.di

import android.content.Context
import androidx.room.Room
import com.yourname.smartpermissionanalyzer.data.local.database.AppDatabase
import com.yourname.smartpermissionanalyzer.data.local.dao.AppEntityDao
import com.yourname.smartpermissionanalyzer.data.local.dao.ExclusionDao
import com.yourname.smartpermissionanalyzer.data.mapper.EntityMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideAppEntityDao(database: AppDatabase): AppEntityDao {
        return database.appEntityDao()
    }

    @Provides
    @Singleton
    fun provideExclusionDao(database: AppDatabase): ExclusionDao {
        return database.exclusionDao()
    }

    @Provides
    @Singleton
    fun provideEntityMapper(): EntityMapper {
        return EntityMapper()
    }
}
