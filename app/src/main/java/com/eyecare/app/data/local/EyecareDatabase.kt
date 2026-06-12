package com.eyecare.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.eyecare.app.data.local.dao.ProductDao
import com.eyecare.app.data.local.entity.ProductEntity

@Database(entities = [ProductEntity::class], version = 1, exportSchema = true)
abstract class EyecareDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}
