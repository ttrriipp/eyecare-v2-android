package com.eyecare.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
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
import com.eyecare.app.data.local.entity.AppointmentEntity
import com.eyecare.app.data.local.entity.BillingEntity
import com.eyecare.app.data.local.entity.BrandEntity
import com.eyecare.app.data.local.entity.CategoryEntity
import com.eyecare.app.data.local.entity.OrderEntity
import com.eyecare.app.data.local.entity.PendingOperationEntity
import com.eyecare.app.data.local.entity.PrescriptionEntity
import com.eyecare.app.data.local.entity.ProductEntity
import com.eyecare.app.data.local.entity.UserEntity
import com.eyecare.app.data.local.entity.VisitReasonEntity

@Database(
    entities = [
        ProductEntity::class,
        AppointmentEntity::class,
        OrderEntity::class,
        PrescriptionEntity::class,
        BillingEntity::class,
        UserEntity::class,
        VisitReasonEntity::class,
        BrandEntity::class,
        CategoryEntity::class,
        PendingOperationEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class EyecareDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun orderDao(): OrderDao
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun billingDao(): BillingDao
    abstract fun userDao(): UserDao
    abstract fun visitReasonDao(): VisitReasonDao
    abstract fun brandDao(): BrandDao
    abstract fun categoryDao(): CategoryDao
    abstract fun pendingOperationDao(): PendingOperationDao
}
