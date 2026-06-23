package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.Product

interface ProductRepository {
    suspend fun getProducts(page: Int = 1): Result<List<Product>>
    suspend fun getProduct(id: Int): Result<Product>
    suspend fun hasMorePages(page: Int): Boolean
}
