package com.eyecare.app.di

import com.eyecare.app.data.remote.api.EyewearApiService
import com.eyecare.app.data.repository.EyewearRepositoryImpl
import com.eyecare.app.domain.repository.EyewearRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EyewearModule {

    @Binds
    @Singleton
    abstract fun bindEyewearRepository(impl: EyewearRepositoryImpl): EyewearRepository

    companion object {
        @Provides
        @Singleton
        fun provideEyewearApiService(retrofit: Retrofit): EyewearApiService =
            retrofit.create(EyewearApiService::class.java)
    }
}
