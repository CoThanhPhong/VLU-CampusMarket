package com.example.vlu_campusmarket.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.vlu_campusmarket.data.local.Converters

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val orderItems: List<Converters.EmbeddedOrderItem>,
    val shippingInfo: Converters.EmbeddedShippingInfo,
    val paymentMethod: String,
    val totalPrice: Double,
    val orderStatus: String,
    val isPaid: Boolean
)