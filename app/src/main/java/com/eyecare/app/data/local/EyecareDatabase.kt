package com.eyecare.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.eyecare.app.data.local.dao.FrameDao
import com.eyecare.app.data.local.dao.ProductDao
import com.eyecare.app.data.local.entity.FrameEntity
import com.eyecare.app.data.local.entity.ProductEntity

@Database(
    entities = [ProductEntity::class, FrameEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class EyecareDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun frameDao(): FrameDao
}
