package com.example.vlu_campusmarket.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val mssv: String,
    val email: String,
    val role: String,
    val cookie: String?, // Lưu cookie hoặc token phiên đăng nhập
    val isLoggedIn: Boolean = false // Trạng thái ghi nhớ đăng nhập
)