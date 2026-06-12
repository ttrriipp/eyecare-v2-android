package com.eyecare.app.di

import com.eyecare.app.data.remote.api.PrescriptionApiService
import com.eyecare.app.data.repository.PrescriptionRepositoryImpl
import com.eyecare.app.domain.repository.PrescriptionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PrescriptionModule {

    @Binds @Singleton
    abstract fun bindPrescriptionRepository(impl: PrescriptionRepositoryImpl): PrescriptionRepository

    companion object {
        @Provides @Singleton
        fun providePrescriptionApiService(retrofit: Retrofit): PrescriptionApiService =
            retrofit.create(PrescriptionApiService::class.java)
    }
}
