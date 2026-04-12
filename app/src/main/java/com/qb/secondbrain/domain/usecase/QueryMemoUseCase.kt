package com.qb.secondbrain.domain.usecase

import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.data.repository.MemoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class QueryMemoUseCase @Inject constructor(
    private val repository: MemoRepository
) {
    fun getAll(): Flow<List<Memo>> = repository.getAllMemos()
    suspend fun byId(id: Long): Memo? = repository.getMemoById(id)
    fun byKeywords(keywords: List<String>): Flow<List<Memo>> = repository.searchByKeywords(keywords)
}
