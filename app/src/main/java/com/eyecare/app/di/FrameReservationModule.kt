package com.eyecare.app.di

import com.eyecare.app.data.remote.api.FrameReservationApiService
import com.eyecare.app.data.repository.FrameReservationRepositoryImpl
import com.eyecare.app.domain.repository.FrameReservationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FrameReservationModule {

    @Binds
    @Singleton
    abstract fun bindFrameReservationRepository(impl: FrameReservationRepositoryImpl): FrameReservationRepository

    companion object {
        @Provides
        @Singleton
        fun provideFrameReservationApiService(retrofit: Retrofit): FrameReservationApiService =
            retrofit.create(FrameReservationApiService::class.java)
    }
}
