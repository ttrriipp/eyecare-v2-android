package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.Brand
import com.eyecare.app.domain.model.Category
import com.eyecare.app.domain.model.Product

interface ProductRepository {
    suspend fun getProducts(
        page: Int = 1,
        search: String? = null,
        brandId: Int? = null,
        categoryId: Int? = null,
        sort: String? = null,
        inStock: Boolean? = null,
    ): Result<List<Product>>

    suspend fun getProduct(id: Int): Result<Product>
    suspend fun hasMorePages(page: Int): Boolean
    suspend fun getBrands(): Result<List<Brand>>
    suspend fun getCategories(): Result<List<Category>>
}
