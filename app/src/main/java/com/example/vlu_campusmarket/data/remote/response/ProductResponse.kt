package com.example.vlu_campusmarket.data.remote.response

import com.example.vlu_campusmarket.data.models.ProductModel

data class ProductCreateResponse(
    val success: Boolean = true,
    val message: String = "",
    val data: List<ProductModel> = emptyList()
)

data class ProductListResponse(
    val success: Boolean = true,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalProducts: Int = 0,
    val count: Int = 0,
    val data: List<ProductModel> = emptyList()
)

data class ProductSingleResponse(
    val success: Boolean = true,
    val message: String = "",
    val data: ProductModel? = null
)

data class ProductIdRequest(
    val id: String,
    val productId: String = id,
    val _id: String = id
)
