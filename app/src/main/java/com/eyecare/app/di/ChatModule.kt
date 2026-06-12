package com.eyecare.app.di

import com.eyecare.app.data.remote.api.ConversationApiService
import com.eyecare.app.data.repository.ChatRepositoryImpl
import com.eyecare.app.domain.repository.ChatRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatModule {

    @Binds @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    companion object {
        @Provides @Singleton
        fun provideConversationApiService(retrofit: Retrofit): ConversationApiService =
            retrofit.create(ConversationApiService::class.java)
    }
}
