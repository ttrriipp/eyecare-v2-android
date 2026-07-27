package com.eyecare.app.di

import com.eyecare.app.data.remote.api.FrameApiService
import com.eyecare.app.data.repository.FrameRepositoryImpl
import com.eyecare.app.domain.repository.FrameRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FrameModule {

    @Binds
    @Singleton
    abstract fun bindFrameRepository(impl: FrameRepositoryImpl): FrameRepository

    companion object {
        @Provides
        @Singleton
        fun provideFrameApiService(retrofit: Retrofit): FrameApiService =
            retrofit.create(FrameApiService::class.java)
    }
}
