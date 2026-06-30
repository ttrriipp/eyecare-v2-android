package com.eyecare.app.di

import android.content.Context
import androidx.room.Room
import com.eyecare.app.data.local.EyecareDatabase
import com.eyecare.app.data.local.dao.AppointmentDao
import com.eyecare.app.data.local.dao.BillingDao
import com.eyecare.app.data.local.dao.BrandDao
import com.eyecare.app.data.local.dao.CategoryDao
import com.eyecare.app.data.local.dao.OrderDao
import com.eyecare.app.data.local.dao.PendingOperationDao
import com.eyecare.app.data.local.dao.PrescriptionDao
import com.eyecare.app.data.local.dao.ProductDao
import com.eyecare.app.data.local.dao.UserDao
import com.eyecare.app.data.local.dao.VisitReasonDao
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

    @Provides fun provideProductDao(db: EyecareDatabase): ProductDao = db.productDao()
    @Provides fun provideAppointmentDao(db: EyecareDatabase): AppointmentDao = db.appointmentDao()
    @Provides fun provideOrderDao(db: EyecareDatabase): OrderDao = db.orderDao()
    @Provides fun providePrescriptionDao(db: EyecareDatabase): PrescriptionDao = db.prescriptionDao()
    @Provides fun provideBillingDao(db: EyecareDatabase): BillingDao = db.billingDao()
    @Provides fun provideUserDao(db: EyecareDatabase): UserDao = db.userDao()
    @Provides fun provideVisitReasonDao(db: EyecareDatabase): VisitReasonDao = db.visitReasonDao()
    @Provides fun provideBrandDao(db: EyecareDatabase): BrandDao = db.brandDao()
    @Provides fun provideCategoryDao(db: EyecareDatabase): CategoryDao = db.categoryDao()
    @Provides fun providePendingOperationDao(db: EyecareDatabase): PendingOperationDao = db.pendingOperationDao()
}
