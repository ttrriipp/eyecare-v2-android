package com.eyecare.app.di

import com.eyecare.app.data.remote.api.QuotationApiService
import com.eyecare.app.data.repository.QuotationRepositoryImpl
import com.eyecare.app.domain.repository.QuotationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class QuotationModule {

    @Binds
    @Singleton
    abstract fun bindQuotationRepository(impl: QuotationRepositoryImpl): QuotationRepository

    companion object {
        @Provides
        @Singleton
        fun provideQuotationApiService(retrofit: Retrofit): QuotationApiService =
            retrofit.create(QuotationApiService::class.java)
    }
}
