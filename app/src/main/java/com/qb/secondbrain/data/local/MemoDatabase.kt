package com.qb.secondbrain.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.qb.secondbrain.data.model.Memo

@Database(
    entities = [Memo::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MemoDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
}