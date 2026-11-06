package com.example.vlu_campusmarket.data.models

data class OrderItemModel(
    val product: String,
    val quantity: Int,
    val price: Double,
    val local: String
)

data class ShippingInfoModel(
    val fullName: String,
    val phone: String,
    val address: String,
    val local: String
)

data class OrderModel(
    val _id: String,
    val user: String,
    val orderItems: List<OrderItemModel>,
    val shippingInfo: ShippingInfoModel,
    val paymentMethod: String,
    val totalPrice: Double,
    val orderStatus: String,
    val isPaid: Boolean,
    val paidAt: String?
)