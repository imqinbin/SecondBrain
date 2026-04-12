package com.qb.secondbrain.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.context.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qb.secondbrain.data.local.MemoDao
import com.qb.secondbrain.data.local.MemoDatabase
import com.qb.secondbrain.data.model.Memo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalMemoRepositoryTest {

    private lateinit var database: MemoDatabase
    private lateinit var memoDao: MemoDao
    private lateinit var repository: LocalMemoRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            MemoDatabase::class.java
        ).allowMainThreadQueries().build()
        memoDao = database.memoDao()
        repository = LocalMemoRepository(memoDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addMemo_inserts_and_returns_id() = runTest {
        val memo = Memo(content = "Test memo", rawText = "test raw")
        val id = repository.addMemo(memo)
        assertTrue(id > 0)
        val retrieved = repository.getMemoById(id)
        assertNotNull(retrieved)
        assertEquals("Test memo", retrieved!!.content)
    }

    @Test
    fun updateMemo_updates_existing() = runTest {
        val id = repository.addMemo(Memo(content = "Original"))
        val original = repository.getMemoById(id)!!
        repository.updateMemo(original.copy(content = "Updated", updatedAt = System.currentTimeMillis()))
        assertEquals("Updated", repository.getMemoById(id)!!.content)
    }

    @Test
    fun deleteMemo_soft_deletes() = runTest {
        val id = repository.addMemo(Memo(content = "To delete"))
        repository.deleteMemo(id)
        assertNull(repository.getMemoById(id))
    }

    @Test
    fun getAllMemos_returns_only_nonDeleted() = runTest {
        repository.addMemo(Memo(content = "Memo 1"))
        repository.addMemo(Memo(content = "Memo 2"))
        val all = repository.getAllMemos().first()
        assertEquals(2, all.size)
        repository.deleteMemo(all[0].id)
        assertEquals(1, repository.getAllMemos().first().size)
    }

    @Test
    fun searchByKeywords_returns_matching() = runTest {
        repository.addMemo(Memo(content = "海天公园停车场"))
        repository.addMemo(Memo(content = "今天天气不错"))
        val results = repository.searchByKeywords(listOf("停车")).first()
        assertEquals(1, results.size)
    }

    @Test
    fun searchByKeywords_empty_returns_empty() = runTest {
        repository.addMemo(Memo(content = "Some memo"))
        assertEquals(0, repository.searchByKeywords(emptyList()).first().size)
    }
}