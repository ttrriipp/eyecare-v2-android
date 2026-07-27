package com.eyecare.app.di

import com.eyecare.app.data.remote.api.PatientIntakeApiService
import com.eyecare.app.data.repository.PatientIntakeRepositoryImpl
import com.eyecare.app.domain.repository.PatientIntakeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PatientIntakeModule {

    @Binds
    @Singleton
    abstract fun bindPatientIntakeRepository(impl: PatientIntakeRepositoryImpl): PatientIntakeRepository

    companion object {
        @Provides
        @Singleton
        fun providePatientIntakeApiService(retrofit: Retrofit): PatientIntakeApiService =
            retrofit.create(PatientIntakeApiService::class.java)
    }
}
