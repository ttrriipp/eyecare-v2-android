package com.eyecare.app.di

import com.eyecare.app.presentation.ar.capability.AndroidArCapabilityProvider
import com.eyecare.app.presentation.ar.capability.ArCapabilityProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ArCapabilityModule {

    @Binds
    @Singleton
    abstract fun bindArCapabilityProvider(
        provider: AndroidArCapabilityProvider,
    ): ArCapabilityProvider
}
