package com.example.vlu_campusmarket.data.models

// Tất cả tham số đều có giá trị mặc định => Kotlin tạo constructor rỗng, Gson sẽ dùng nó
// nên nếu server thiếu field thì không bị null/NPE gây crash.
data class ProductModel(
    val _id: String = "",
    val name: String = "",
    val description: String? = null,
    val price: Double = 0.0,
    val amount: Int = 0,
    val local: String = "",
    val img: String = "",
    val startDate: String? = null,
    val status: String? = null,
    val deviceType: String = "",
    val endDate: String? = null,
    val author: String = ""
)
