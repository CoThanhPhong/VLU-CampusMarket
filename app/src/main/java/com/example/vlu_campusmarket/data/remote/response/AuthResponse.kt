package com.example.vlu_campusmarket.data.remote.response

import com.example.vlu_campusmarket.data.models.UserModel

data class LoginRequest(
    val mssv: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val user: UserModel?,
    val message: String
)