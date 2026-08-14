package com.eyecare.app.di

import com.eyecare.app.data.remote.api.ClinicApiService
import com.eyecare.app.data.repository.ClinicRepositoryImpl
import com.eyecare.app.domain.repository.ClinicRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClinicModule {

    @Binds @Singleton
    abstract fun bindClinicRepository(impl: ClinicRepositoryImpl): ClinicRepository

    companion object {
        @Provides @Singleton
        fun provideClinicApiService(retrofit: Retrofit): ClinicApiService =
            retrofit.create(ClinicApiService::class.java)
    }
}
