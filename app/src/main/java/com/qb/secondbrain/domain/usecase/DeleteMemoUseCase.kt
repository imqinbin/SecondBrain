package com.qb.secondbrain.domain.usecase

import com.qb.secondbrain.data.repository.MemoRepository
import javax.inject.Inject

class DeleteMemoUseCase @Inject constructor(
    private val repository: MemoRepository
) {
    suspend operator fun invoke(id: Long) = repository.deleteMemo(id)
}
