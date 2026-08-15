package com.eyecare.app.di

import com.eyecare.app.data.remote.api.NotificationApiService
import com.eyecare.app.data.repository.NotificationRepositoryImpl
import com.eyecare.app.domain.repository.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    companion object {
        @Provides @Singleton
        fun provideNotificationApiService(retrofit: Retrofit): NotificationApiService =
            retrofit.create(NotificationApiService::class.java)
    }
}
