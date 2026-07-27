package com.eyecare.app.di

import com.eyecare.app.data.remote.api.JobOrderApiService
import com.eyecare.app.data.repository.JobOrderRepositoryImpl
import com.eyecare.app.domain.repository.JobOrderRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class JobOrderModule {

    @Binds
    @Singleton
    abstract fun bindJobOrderRepository(impl: JobOrderRepositoryImpl): JobOrderRepository

    companion object {
        @Provides
        @Singleton
        fun provideJobOrderApiService(retrofit: Retrofit): JobOrderApiService =
            retrofit.create(JobOrderApiService::class.java)
    }
}
