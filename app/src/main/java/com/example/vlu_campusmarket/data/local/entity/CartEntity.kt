package com.example.vlu_campusmarket.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.vlu_campusmarket.data.local.Converters

@Entity(tableName = "carts")
data class CartEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val items: List<Converters.EmbeddedCartItem>
)