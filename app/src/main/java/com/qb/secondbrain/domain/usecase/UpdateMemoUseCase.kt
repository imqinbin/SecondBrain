package com.qb.secondbrain.domain.usecase

import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.data.repository.MemoRepository
import javax.inject.Inject

class UpdateMemoUseCase @Inject constructor(
    private val repository: MemoRepository
) {
    suspend operator fun invoke(memo: Memo) = repository.updateMemo(memo)
}
