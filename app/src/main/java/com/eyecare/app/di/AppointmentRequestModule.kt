package com.eyecare.app.di

import com.eyecare.app.data.remote.api.AppointmentRequestApiService
import com.eyecare.app.data.repository.AppointmentRequestRepositoryImpl
import com.eyecare.app.domain.repository.AppointmentRequestRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppointmentRequestModule {

    @Binds
    @Singleton
    abstract fun bindAppointmentRequestRepository(impl: AppointmentRequestRepositoryImpl): AppointmentRequestRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppointmentRequestApiService(retrofit: Retrofit): AppointmentRequestApiService =
            retrofit.create(AppointmentRequestApiService::class.java)
    }
}
