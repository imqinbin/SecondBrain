package com.qb.secondbrain.data.repository

import com.qb.secondbrain.data.model.Memo
import kotlinx.coroutines.flow.Flow

interface MemoRepository {
    suspend fun addMemo(memo: Memo): Long
    suspend fun updateMemo(memo: Memo)
    suspend fun deleteMemo(id: Long)
    suspend fun getMemoById(id: Long): Memo?
    fun getAllMemos(): Flow<List<Memo>>
    fun searchByKeywords(keywords: List<String>): Flow<List<Memo>>
    fun searchByTag(tag: String): Flow<List<Memo>>
}