package com.eyecare.app.di

import com.eyecare.app.data.remote.api.BillingRecordApiService
import com.eyecare.app.data.repository.BillingRecordRepositoryImpl
import com.eyecare.app.domain.repository.BillingRecordRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingRecordModule {

    @Binds
    @Singleton
    abstract fun bindBillingRecordRepository(impl: BillingRecordRepositoryImpl): BillingRecordRepository

    companion object {
        @Provides
        @Singleton
        fun provideBillingRecordApiService(retrofit: Retrofit): BillingRecordApiService =
            retrofit.create(BillingRecordApiService::class.java)
    }
}
