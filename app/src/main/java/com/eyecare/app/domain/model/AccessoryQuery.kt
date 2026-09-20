package com.eyecare.app.domain.model

data class AccessoryQuery(
    val search: String? = null,
    val sort: String? = null,
    val minimumRating: Int? = null,
    val rated: String? = null,
    val page: Int = 1,
    val perPage: Int = 15,
)