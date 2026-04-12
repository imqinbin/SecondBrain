package com.qb.secondbrain.domain.usecase

import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.data.repository.MemoRepository
import javax.inject.Inject

class AddMemoUseCase @Inject constructor(
    private val repository: MemoRepository
) {
    suspend operator fun invoke(memo: Memo): Long = repository.addMemo(memo)
}
