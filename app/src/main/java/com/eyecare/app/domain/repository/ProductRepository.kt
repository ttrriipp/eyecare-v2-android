package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.Product

interface ProductRepository {
    suspend fun getProducts(): Result<List<Product>>
    suspend fun getProduct(id: Int): Result<Product>
}
