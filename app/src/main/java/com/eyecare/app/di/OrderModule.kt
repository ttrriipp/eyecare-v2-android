package com.eyecare.app.di

import com.eyecare.app.data.remote.api.OrderApiService
import com.eyecare.app.data.repository.OrderRepositoryImpl
import com.eyecare.app.domain.repository.OrderRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OrderModule {

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    companion object {
        @Provides
        @Singleton
        fun provideOrderApiService(retrofit: Retrofit): OrderApiService =
            retrofit.create(OrderApiService::class.java)
    }
}
