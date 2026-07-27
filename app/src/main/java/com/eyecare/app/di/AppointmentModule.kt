package com.eyecare.app.di

import com.eyecare.app.data.remote.api.AppointmentV1ApiService
import com.eyecare.app.data.repository.AppointmentV1RepositoryImpl
import com.eyecare.app.domain.repository.AppointmentV1Repository
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
    abstract fun bindAppointmentV1Repository(impl: AppointmentV1RepositoryImpl): AppointmentV1Repository

    companion object {
        @Provides
        @Singleton
        fun provideAppointmentV1ApiService(retrofit: Retrofit): AppointmentV1ApiService =
            retrofit.create(AppointmentV1ApiService::class.java)
    }
}
