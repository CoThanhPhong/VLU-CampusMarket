package com.example.vlu_campusmarket.data.models

data class ProductModel(
    val _id: String,
    val name: String,
    val description: String?,
    val price: Double,
    val amount: Int,
    val local: String,
    val img: String,
    val startDate: String,
    val status: String,
    val deviceType: String,
    val endDate: String?,
    val author: String
)