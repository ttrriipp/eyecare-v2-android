package com.eyecare.app.di

import com.eyecare.app.data.remote.api.BillingApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BillingModule {
    @Provides
    @Singleton
    fun provideBillingApiService(retrofit: Retrofit): BillingApiService =
        retrofit.create(BillingApiService::class.java)
}
