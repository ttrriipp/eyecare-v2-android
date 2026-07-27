package com.eyecare.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.eyecare.app.data.local.entity.FrameEntity

@Dao
interface FrameDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(frames: List<FrameEntity>)

    @Query("SELECT * FROM frames")
    suspend fun getAll(): List<FrameEntity>

    @Query("SELECT * FROM frames WHERE id = :id")
    suspend fun getById(id: Int): FrameEntity?

    @Query("DELETE FROM frames")
    suspend fun clearAll()
}
