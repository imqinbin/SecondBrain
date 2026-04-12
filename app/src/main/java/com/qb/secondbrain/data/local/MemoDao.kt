package com.qb.secondbrain.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.qb.secondbrain.data.model.Memo
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memo: Memo): Long

    @Update
    suspend fun update(memo: Memo)

    @Query("UPDATE memo SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM memo WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: Long): Memo?

    @Query("SELECT * FROM memo WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Memo>>

    @Query("""
        SELECT * FROM memo
        WHERE isDeleted = 0
        AND (content LIKE '%' || :keyword || '%' OR rawText LIKE '%' || :keyword || '%')
        ORDER BY createdAt DESC
    """)
    fun searchByKeyword(keyword: String): Flow<List<Memo>>

    @Query("""
        SELECT * FROM memo
        WHERE isDeleted = 0
        AND (
            content LIKE '%' || :kw1 || '%'
            OR rawText LIKE '%' || :kw1 || '%'
            OR content LIKE '%' || :kw2 || '%'
            OR rawText LIKE '%' || :kw2 || '%'
        )
        ORDER BY createdAt DESC
    """)
    fun searchByTwoKeywords(kw1: String, kw2: String): Flow<List<Memo>>

    @Query("""
        SELECT * FROM memo
        WHERE isDeleted = 0
        AND tags LIKE '%' || :tag || '%'
        ORDER BY createdAt DESC
    """)
    fun searchByTag(tag: String): Flow<List<Memo>>

    @Query("SELECT * FROM memo WHERE isDeleted = 0 AND reminderTime IS NOT NULL AND reminderTime <= :now")
    suspend fun getDueReminders(now: Long = System.currentTimeMillis()): List<Memo>
}