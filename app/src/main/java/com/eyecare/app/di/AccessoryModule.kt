package com.eyecare.app.di

import com.eyecare.app.data.remote.api.AccessoryApiService
import com.eyecare.app.data.repository.AccessoryRepositoryImpl
import com.eyecare.app.domain.repository.AccessoryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AccessoryModule {

    @Provides
    @Singleton
    fun provideAccessoryApiService(retrofit: Retrofit): AccessoryApiService =
        retrofit.create(AccessoryApiService::class.java)

    @Provides
    @Singleton
    fun provideAccessoryRepository(api: AccessoryApiService): AccessoryRepository =
        AccessoryRepositoryImpl(api)
}