package com.eyecare.app.di

import com.eyecare.app.data.remote.api.FeedbackApiService
import com.eyecare.app.data.repository.FeedbackRepositoryImpl
import com.eyecare.app.domain.repository.FeedbackRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FeedbackModule {
    @Binds @Singleton
    abstract fun bindFeedbackRepository(impl: FeedbackRepositoryImpl): FeedbackRepository

    companion object {
        @Provides @Singleton
        fun provideFeedbackApiService(retrofit: Retrofit): FeedbackApiService =
            retrofit.create(FeedbackApiService::class.java)
    }
}
