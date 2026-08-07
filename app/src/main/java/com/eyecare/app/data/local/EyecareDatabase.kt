package com.eyecare.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.eyecare.app.data.local.dao.FrameDao
import com.eyecare.app.data.local.dao.ProductDao
import com.eyecare.app.data.local.entity.FrameEntity
import com.eyecare.app.data.local.entity.ProductEntity

@Database(
    entities = [ProductEntity::class, FrameEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class EyecareDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun frameDao(): FrameDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE frames ADD COLUMN averageRating REAL")
                db.execSQL("ALTER TABLE frames ADD COLUMN ratingCount INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
