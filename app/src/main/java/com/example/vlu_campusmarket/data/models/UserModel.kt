package com.example.vlu_campusmarket.data.models

data class UserProfile(
    val id: String,
    val name: String,
    val bio: String?,
    val avatar: String?,
    val phone: String
)

data class UserModel(
    val id: String,
    val mssv: String,
    val email: String,
    val role: String,
    val profile: UserProfile?
)