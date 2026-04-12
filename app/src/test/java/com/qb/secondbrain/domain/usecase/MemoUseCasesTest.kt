package com.qb.secondbrain.domain.usecase

import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.data.repository.MemoRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MemoUseCasesTest {

    private val repository: MemoRepository = mock()
    private val addMemoUseCase = AddMemoUseCase(repository)
    private val queryMemoUseCase = QueryMemoUseCase(repository)
    private val updateMemoUseCase = UpdateMemoUseCase(repository)
    private val deleteMemoUseCase = DeleteMemoUseCase(repository)

    @Test
    fun addMemo_invokesRepository() = runTest {
        val memo = Memo(title = "Test", content = "Content")
        whenever(repository.addMemo(memo)).thenReturn(1L)

        val result = addMemoUseCase(memo)

        assert(result == 1L)
        verify(repository).addMemo(memo)
    }

    @Test
    fun getAll_returnsFlowFromRepository() {
        val memos = listOf(Memo(title = "T", content = "C"))
        whenever(repository.getAllMemos()).thenReturn(flowOf(memos))

        val result = queryMemoUseCase.getAll()

        // Verify delegation (Flow is lazy, just verify the call)
        verify(repository).getAllMemos()
    }

    @Test
    fun byId_invokesRepository() = runTest {
        val memo = Memo(id = 42, title = "T", content = "C")
        whenever(repository.getMemoById(42L)).thenReturn(memo)

        val result = queryMemoUseCase.byId(42L)

        assert(result == memo)
        verify(repository).getMemoById(42L)
    }

    @Test
    fun byKeywords_invokesRepository() {
        whenever(repository.searchByKeywords(listOf("test"))).thenReturn(flowOf(emptyList()))

        queryMemoUseCase.byKeywords(listOf("test"))

        verify(repository).searchByKeywords(listOf("test"))
    }

    @Test
    fun updateMemo_invokesRepository() = runTest {
        val memo = Memo(id = 1, title = "Updated", content = "Content")

        updateMemoUseCase(memo)

        verify(repository).updateMemo(memo)
    }

    @Test
    fun deleteMemo_invokesRepository() = runTest {
        deleteMemoUseCase(1L)

        verify(repository).deleteMemo(1L)
    }
}
