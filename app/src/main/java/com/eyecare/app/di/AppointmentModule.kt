package com.eyecare.app.di

import com.eyecare.app.data.remote.api.AppointmentApiService
import com.eyecare.app.data.repository.AppointmentRepositoryImpl
import com.eyecare.app.domain.repository.AppointmentRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppointmentModule {

    @Binds
    @Singleton
    abstract fun bindAppointmentRepository(impl: AppointmentRepositoryImpl): AppointmentRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppointmentApiService(retrofit: Retrofit): AppointmentApiService =
            retrofit.create(AppointmentApiService::class.java)
    }
}
