package com.eyecare.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.eyecare.app.data.local.entity.AppointmentEntity
import com.eyecare.app.data.local.entity.BillingEntity
import com.eyecare.app.data.local.entity.BrandEntity
import com.eyecare.app.data.local.entity.CategoryEntity
import com.eyecare.app.data.local.entity.OrderEntity
import com.eyecare.app.data.local.entity.PrescriptionEntity
import com.eyecare.app.data.local.entity.UserEntity
import com.eyecare.app.data.local.entity.VisitReasonEntity

@Dao
interface AppointmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AppointmentEntity>)

    @Query("SELECT * FROM appointments ORDER BY scheduledAt DESC")
    suspend fun getAll(): List<AppointmentEntity>

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getById(id: Int): AppointmentEntity?

    @Query("DELETE FROM appointments")
    suspend fun clearAll()
}

@Dao
interface OrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<OrderEntity>)

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    suspend fun getAll(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getById(id: Int): OrderEntity?

    @Query("DELETE FROM orders")
    suspend fun clearAll()
}

@Dao
interface PrescriptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PrescriptionEntity>)

    @Query("SELECT * FROM prescriptions ORDER BY prescribedAt DESC")
    suspend fun getAll(): List<PrescriptionEntity>

    @Query("SELECT * FROM prescriptions WHERE id = :id")
    suspend fun getById(id: Int): PrescriptionEntity?

    @Query("DELETE FROM prescriptions")
    suspend fun clearAll()
}

@Dao
interface BillingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: BillingEntity)

    @Query("SELECT * FROM billings WHERE id = :id")
    suspend fun getById(id: Int): BillingEntity?

    @Query("DELETE FROM billings")
    suspend fun clearAll()
}

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: UserEntity)

    @Query("SELECT * FROM cached_user LIMIT 1")
    suspend fun get(): UserEntity?

    @Query("DELETE FROM cached_user")
    suspend fun clear()
}

@Dao
interface VisitReasonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VisitReasonEntity>)

    @Query("SELECT * FROM visit_reasons")
    suspend fun getAll(): List<VisitReasonEntity>

    @Query("DELETE FROM visit_reasons")
    suspend fun clearAll()
}

@Dao
interface BrandDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BrandEntity>)

    @Query("SELECT * FROM brands ORDER BY name")
    suspend fun getAll(): List<BrandEntity>

    @Query("DELETE FROM brands")
    suspend fun clearAll()
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CategoryEntity>)

    @Query("SELECT * FROM categories ORDER BY name")
    suspend fun getAll(): List<CategoryEntity>

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}
