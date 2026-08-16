package com.eyecare.app.di

import android.content.Context
import com.eyecare.app.data.ar.RemoteArAssetRepository
import com.eyecare.app.domain.repository.ArAssetRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

private const val AR_ASSET_CACHE_DIRECTORY = "ar-assets"

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ArAssetCacheDirectory

@Module
@InstallIn(SingletonComponent::class)
abstract class ArModule {

    @Binds
    @Singleton
    abstract fun bindArAssetRepository(impl: RemoteArAssetRepository): ArAssetRepository

    companion object {
        @Provides
        @Singleton
        @ArAssetCacheDirectory
        fun provideArAssetCacheDirectory(
            @ApplicationContext context: Context,
        ): File = File(context.cacheDir, AR_ASSET_CACHE_DIRECTORY)
    }
}
