package com.eyecare.app.di

import com.eyecare.app.data.remote.api.BillingApiService
import com.eyecare.app.data.repository.BillingRepositoryImpl
import com.eyecare.app.domain.repository.BillingRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {
    @Binds @Singleton
    abstract fun bindBillingRepository(impl: BillingRepositoryImpl): BillingRepository

    companion object {
        @Provides @Singleton
        fun provideBillingApiService(retrofit: Retrofit): BillingApiService =
            retrofit.create(BillingApiService::class.java)
    }
}
