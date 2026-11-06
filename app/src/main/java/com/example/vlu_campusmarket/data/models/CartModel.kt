package com.example.vlu_campusmarket.data.models

data class CartItemModel(
    val product: String,
    val quantity: Int
)

data class CartModel(
    val _id: String,
    val user: String,
    val items: List<CartItemModel>
)