package com.eyecare.app.di

import com.eyecare.app.data.remote.api.InvoiceApiService
import com.eyecare.app.data.repository.InvoiceRepositoryImpl
import com.eyecare.app.domain.repository.InvoiceRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InvoiceModule {

    @Binds
    @Singleton
    abstract fun bindInvoiceRepository(impl: InvoiceRepositoryImpl): InvoiceRepository

    companion object {
        @Provides
        @Singleton
        fun provideInvoiceApiService(retrofit: Retrofit): InvoiceApiService =
            retrofit.create(InvoiceApiService::class.java)
    }
}
