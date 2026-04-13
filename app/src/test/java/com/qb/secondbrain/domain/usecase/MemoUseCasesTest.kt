package com.qb.secondbrain.domain.usecase

import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.data.repository.MemoRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MemoUseCasesTest {

    private val repository: MemoRepository = mockk(relaxed = true)
    private val addMemoUseCase = AddMemoUseCase(repository)
    private val queryMemoUseCase = QueryMemoUseCase(repository)
    private val updateMemoUseCase = UpdateMemoUseCase(repository)
    private val deleteMemoUseCase = DeleteMemoUseCase(repository)

    @Test
    fun addMemo_invokesRepository() = runTest {
        val memo = Memo(content = "Content")
        coEvery { repository.addMemo(memo) } returns 1L

        val result = addMemoUseCase(memo)

        assert(result == 1L)
        coVerify { repository.addMemo(memo) }
    }

    @Test
    fun getAll_returnsFlowFromRepository() {
        val memos = listOf(Memo(content = "C"))
        every { repository.getAllMemos() } returns flowOf(memos)

        val result = queryMemoUseCase.getAll()

        verify { repository.getAllMemos() }
    }

    @Test
    fun byId_invokesRepository() = runTest {
        val memo = Memo(id = 42, content = "C")
        coEvery { repository.getMemoById(42L) } returns memo

        val result = queryMemoUseCase.byId(42L)

        assert(result == memo)
        coVerify { repository.getMemoById(42L) }
    }

    @Test
    fun byKeywords_invokesRepository() {
        every { repository.searchByKeywords(listOf("test")) } returns flowOf(emptyList())

        queryMemoUseCase.byKeywords(listOf("test"))

        verify { repository.searchByKeywords(listOf("test")) }
    }

    @Test
    fun updateMemo_invokesRepository() = runTest {
        val memo = Memo(id = 1, content = "Updated")

        updateMemoUseCase(memo)

        coVerify { repository.updateMemo(memo) }
    }

    @Test
    fun deleteMemo_invokesRepository() = runTest {
        deleteMemoUseCase(1L)

        coVerify { repository.deleteMemo(1L) }
    }
}
