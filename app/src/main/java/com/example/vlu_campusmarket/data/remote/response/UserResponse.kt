package com.example.vlu_campusmarket.data.remote.response

import com.example.vlu_campusmarket.data.models.UserModel

data class UserDetailResponse(
    val success: Boolean,
    val data: UserModel
)

data class UserListResponse(
    val success: Boolean,
    val count: Int,
    val data: List<UserModel>
)

data class UserIdRequest(
    val id: String
)