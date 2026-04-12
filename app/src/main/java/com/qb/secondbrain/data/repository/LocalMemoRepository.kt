package com.qb.secondbrain.data.repository

import com.qb.secondbrain.data.local.MemoDao
import com.qb.secondbrain.data.model.Memo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMemoRepository @Inject constructor(
    private val memoDao: MemoDao
) : MemoRepository {

    override suspend fun addMemo(memo: Memo): Long {
        return memoDao.insert(memo)
    }

    override suspend fun updateMemo(memo: Memo) {
        memoDao.update(memo)
    }

    override suspend fun deleteMemo(id: Long) {
        memoDao.softDelete(id)
    }

    override suspend fun getMemoById(id: Long): Memo? {
        return memoDao.getById(id)
    }

    override fun getAllMemos(): Flow<List<Memo>> {
        return memoDao.getAll().distinctUntilChanged()
    }

    override fun searchByKeywords(keywords: List<String>): Flow<List<Memo>> {
        if (keywords.isEmpty()) {
            return flowOf(emptyList())
        }
        if (keywords.size == 1) {
            return memoDao.searchByKeyword(keywords[0]).distinctUntilChanged()
        }
        if (keywords.size == 2) {
            return memoDao.searchByTwoKeywords(keywords[0], keywords[1]).distinctUntilChanged()
        }
        return memoDao.searchByKeyword(keywords[0]).distinctUntilChanged()
    }

    override fun searchByTag(tag: String): Flow<List<Memo>> {
        return memoDao.searchByTag(tag).distinctUntilChanged()
    }
}