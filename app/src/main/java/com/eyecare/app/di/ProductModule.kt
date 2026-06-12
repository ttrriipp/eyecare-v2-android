package com.eyecare.app.di

import com.eyecare.app.data.remote.api.ProductApiService
import com.eyecare.app.data.repository.ProductRepositoryImpl
import com.eyecare.app.domain.repository.ProductRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProductModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    companion object {
        @Provides
        @Singleton
        fun provideProductApiService(retrofit: Retrofit): ProductApiService =
            retrofit.create(ProductApiService::class.java)
    }
}
