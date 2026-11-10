package com.example.vlu_campusmarket.data.models

import com.google.gson.annotations.SerializedName

data class UserProfile(
    @SerializedName(value = "id", alternate = ["_id"])
    val id: String = "",
    val name: String = "Sinh viên VLU",
    val bio: String? = null,
    val avatar: String? = null,
    val phone: String = ""
)

data class UserModel(
    @SerializedName(value = "id", alternate = ["_id"])
    val id: String = "",
    val mssv: String = "",
    val email: String = "",
    val role: String = "user",
    val profile: UserProfile? = null
)
