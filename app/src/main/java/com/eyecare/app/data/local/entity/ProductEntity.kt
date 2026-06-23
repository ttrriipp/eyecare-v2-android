package com.eyecare.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val slug: String,
    val description: String?,
    val productType: String,
    val brandName: String,
    val categoryName: String,
    val variantsJson: String,
    val imagesJson: String,
)
