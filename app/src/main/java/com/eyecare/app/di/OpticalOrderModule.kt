package com.eyecare.app.di

import com.eyecare.app.data.remote.api.OpticalOrderApiService
import com.eyecare.app.data.repository.OpticalOrderRepositoryImpl
import com.eyecare.app.domain.repository.OpticalOrderRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OpticalOrderModule {

    @Provides
    @Singleton
    fun provideOpticalOrderApiService(retrofit: Retrofit): OpticalOrderApiService =
        retrofit.create(OpticalOrderApiService::class.java)

    @Provides
    @Singleton
    fun provideOpticalOrderRepository(api: OpticalOrderApiService): OpticalOrderRepository =
        OpticalOrderRepositoryImpl(api)
}
