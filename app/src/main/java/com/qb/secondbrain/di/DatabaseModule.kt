package com.qb.secondbrain.di

import android.content.Context
import androidx.room.Room
import com.qb.secondbrain.data.local.Converters
import com.qb.secondbrain.data.local.MemoDao
import com.qb.secondbrain.data.local.MemoDatabase
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
    fun provideMemoDatabase(
        @ApplicationContext context: Context,
        converters: Converters
    ): MemoDatabase {
        return Room.databaseBuilder(
            context,
            MemoDatabase::class.java,
            "secondbrain_db"
        )
            .addTypeConverter(converters)
            .build()
    }

    @Provides
    fun provideMemoDao(database: MemoDatabase): MemoDao {
        return database.memoDao()
    }
}