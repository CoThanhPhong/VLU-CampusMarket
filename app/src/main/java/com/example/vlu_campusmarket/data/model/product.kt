package com.example.vlu_campusmarket.data.model

data class product (
    val id: String? = null,
    val name: String,
    val description: String?,
    val price: Double,
    val amount: Int,
    val local: String,       // "Cơ sở 1", "Cơ sở 2", "Cơ sở 3"
    val img: String,         // URL hoặc tên file ảnh trong drawable
    val startDate: String,   // Có thể dùng String hoặc Date tùy bạn xử lý
    val status: String = "Còn hàng", // "Còn hàng", "Hết hàng"
    val deviceType: String,  // "mobile", "tablet", "laptop", "mac"
    val endDate: String? = null
)