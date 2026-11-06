package com.example.vlu_campusmarket.data.remote.response

import com.example.vlu_campusmarket.data.models.CartItemModel

data class CartApiResponse(
    val success: Boolean,
    val message: String?,
    val data: List<CartItemModel>
)

data class CartItemPayload(
    val product: String,
    val quantity: Int
)

data class UpdateCartRequest(
    val items: List<CartItemPayload>
)