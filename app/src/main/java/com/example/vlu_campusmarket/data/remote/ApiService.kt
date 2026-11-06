package com.example.vlu_campusmarket.data.remote

import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.data.remote.response.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ==========================================
    // 1. AUTHENTICATION & USER ROUTES (auth.js)
    // ==========================================
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("auth/profile")
    suspend fun getCurrentUser(): Response<UserDetailResponse>

    @GET("auth/users")
    suspend fun getAllUsers(): Response<UserListResponse>

    @POST("auth/user")
    suspend fun getUserById(
        @Body request: UserIdRequest
    ): Response<UserDetailResponse>


    // ==========================================
    // 2. CART ROUTES (cart_2.js)
    // ==========================================
    @GET("cart/g")
    suspend fun getCart(): Response<CartApiResponse>

    @POST("cart/u")
    suspend fun updateCart(
        @Body request: UpdateCartRequest
    ): Response<CartApiResponse>


    // ==========================================
    // 3. CHECKOUT ROUTES (checkout.js)
    // ==========================================
    @POST("checkout/validate")
    suspend fun validateCheckout(
        @Body request: ValidateCheckoutRequest
    ): Response<BaseApiResponse>


    // ==========================================
    // 4. ORDER ROUTES (order_2.js)
    // ==========================================
    @POST("orders/c")
    suspend fun createOrder(
        @Body request: CreateOrderRequest
    ): Response<OrderCreateResponse>

    @GET("orders/g")
    suspend fun getMyOrders(): Response<OrderListResponse>

    @PUT("orders/cancel")
    suspend fun cancelOrder(
        @Body request: CancelOrderRequest
    ): Response<CancelOrderResponse>


    // ==========================================
    // 5. PRODUCT ROUTES (productRoutes.js)
    // ==========================================
    @POST("products/c")
    suspend fun createProducts(
        @Body products: List<ProductModel>
    ): Response<ProductCreateResponse>

    // Sử dụng @HTTP với hasBody = true vì GET request bên Node.js có nhận req.body[cite: 11, 15]
    @HTTP(method = "GET", path = "products/g", hasBody = true)
    suspend fun getProductById(
        @Body request: ProductIdRequest?
    ): Response<ProductListResponse>

    @PUT("products/u")
    suspend fun updateProduct(
        @Body product: ProductModel
    ): Response<ProductSingleResponse>

    @HTTP(method = "DELETE", path = "products/d", hasBody = true)
    suspend fun deleteProduct(
        @Body request: ProductIdRequest
    ): Response<BaseApiResponse>
}