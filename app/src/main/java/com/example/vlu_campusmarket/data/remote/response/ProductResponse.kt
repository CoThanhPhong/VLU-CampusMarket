package com.example.vlu_campusmarket.data.remote.response

import com.example.vlu_campusmarket.data.models.ProductModel

data class ProductCreateResponse(
    val success: Boolean,
    val message: String,
    val data: List<ProductModel>
)

data class ProductListResponse(
    val count: Int,
    val success: Boolean,
    val data: List<ProductModel>
)

data class ProductSingleResponse(
    val success: Boolean,
    val message: String,
    val data: ProductModel
)

data class ProductIdRequest(
    val _id: String
)