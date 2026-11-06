package com.example.vlu_campusmarket.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    data class EmbeddedCartItem(val product: String, val quantity: Int)
    data class EmbeddedOrderItem(val product: String, val quantity: Int, val price: Double, val local: String)
    data class EmbeddedShippingInfo(val fullName: String, val phone: String, val address: String, val local: String)

    @TypeConverter
    fun fromCartItems(value: List<EmbeddedCartItem>?): String = gson.toJson(value)
    @TypeConverter
    fun toCartItems(value: String): List<EmbeddedCartItem> {
        val type = object : TypeToken<List<EmbeddedCartItem>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromOrderItems(value: List<EmbeddedOrderItem>?): String = gson.toJson(value)
    @TypeConverter
    fun toOrderItems(value: String): List<EmbeddedOrderItem> {
        val type = object : TypeToken<List<EmbeddedOrderItem>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromShippingInfo(value: EmbeddedShippingInfo?): String = gson.toJson(value)
    @TypeConverter
    fun toShippingInfo(value: String): EmbeddedShippingInfo? {
        return gson.fromJson(value, EmbeddedShippingInfo::class.java)
    }
}