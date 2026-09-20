package com.eyecare.app.di

import com.eyecare.app.data.remote.api.AccessoryOrderRequestApiService
import com.eyecare.app.data.repository.AccessoryOrderRequestRepositoryImpl
import com.eyecare.app.domain.repository.AccessoryOrderRequestRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AccessoryOrderRequestModule {

    @Provides
    @Singleton
    fun provideAccessoryOrderRequestApiService(retrofit: Retrofit): AccessoryOrderRequestApiService =
        retrofit.create(AccessoryOrderRequestApiService::class.java)

    @Provides
    @Singleton
    fun provideAccessoryOrderRequestRepository(api: AccessoryOrderRequestApiService): AccessoryOrderRequestRepository =
        AccessoryOrderRequestRepositoryImpl(api)
}