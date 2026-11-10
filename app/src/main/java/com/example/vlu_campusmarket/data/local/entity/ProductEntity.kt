package com.example.vlu_campusmarket.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val _id: String,
    val name: String,
    val description: String?,
    val price: Double,
    val amount: Int,
    val local: String,
    val img: String,
    val status: String?,
    val deviceType: String,
    val author: String
)