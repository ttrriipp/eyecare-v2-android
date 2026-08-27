package com.eyecare.app.di

import com.eyecare.app.data.remote.api.SavedFrameApiService
import com.eyecare.app.data.repository.SavedFrameRepositoryImpl
import com.eyecare.app.domain.repository.SavedFrameRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SavedFrameModule {

    @Binds @Singleton
    abstract fun bindSavedFrameRepository(impl: SavedFrameRepositoryImpl): SavedFrameRepository

    companion object {
        @Provides @Singleton
        fun provideSavedFrameApiService(retrofit: Retrofit): SavedFrameApiService =
            retrofit.create(SavedFrameApiService::class.java)
    }
}
