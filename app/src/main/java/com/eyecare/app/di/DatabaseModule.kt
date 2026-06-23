package com.eyecare.app.di

import android.content.Context
import androidx.room.Room
import com.eyecare.app.data.local.EyecareDatabase
import com.eyecare.app.data.local.dao.ProductDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EyecareDatabase =
        Room.databaseBuilder(context, EyecareDatabase::class.java, "eyecare.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideProductDao(db: EyecareDatabase): ProductDao = db.productDao()
}
