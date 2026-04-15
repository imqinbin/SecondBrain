package com.qb.secondbrain.di

import com.qb.secondbrain.asr.AsrEngine
import com.qb.secondbrain.asr.XfyunAsrEngine
import com.qb.secondbrain.data.repository.LocalMemoRepository
import com.qb.secondbrain.data.repository.MemoRepository
import com.qb.secondbrain.llm.LlmClient
import com.qb.secondbrain.llm.OpenAiLlmClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindMemoRepository(impl: LocalMemoRepository): MemoRepository

    @Binds @Singleton
    abstract fun bindAsrEngine(impl: XfyunAsrEngine): AsrEngine

    @Binds @Singleton
    abstract fun bindLlmClient(impl: OpenAiLlmClient): LlmClient
}
