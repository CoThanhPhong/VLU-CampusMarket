package com.example.vlu_campusmarket.data.remote

import com.example.vlu_campusmarket.data.model.Product
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class LoginRequest (
    val mssv: String,
    val password: String
)

data class LoginResponse (
    val token: String?,
    val message: String
)

data class ProductResponse (
    val success: Boolean?,
    val message: String,
    val data: Product?
)

interface ApiService {
    // Bài viết
    // api lấy danh sách bài viết
    @GET("products/g")
    suspend fun getProducts(): List<Product>
    // api lấy thông tin bài viết
    @GET("products/g")
    suspend fun getProductById(@Path("id") id: String): Product
    // api tạo bài viết
    @POST("products/c")
    suspend fun createProduct(@Body product: Product): ProductResponse
    // api cập nhật bài viết
    @PUT("products/u")
    suspend fun updateProduct(@Path("id") id: String, @Body product: Product): ProductResponse

    // Đăng nhập
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}