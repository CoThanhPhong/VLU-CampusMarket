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

    @GET("auth/me")
    suspend fun getCurrentUserMe(): Response<UserDetailResponse>

    @GET("user/me")
    suspend fun getUserMe(): Response<UserDetailResponse>

    @GET("users/me")
    suspend fun getUsersMe(): Response<UserDetailResponse>

    @GET("auth/user")
    suspend fun getCurrentUserAuthUser(): Response<UserDetailResponse>

    @PUT("auth/profile")
    suspend fun updateProfile(
        @Body request: Map<String, String>
    ): Response<UserDetailResponse>

    @PUT("auth/user")
    suspend fun updateUserProfile(
        @Body request: Map<String, String>
    ): Response<UserDetailResponse>

    @PUT("auth/u")
    suspend fun updateProfileU(
        @Body request: Map<String, String>
    ): Response<UserDetailResponse>

    @POST("auth/u")
    suspend fun updateProfileUPost(
        @Body request: Map<String, String>
    ): Response<UserDetailResponse>

    @PUT("auth/user/u")
    suspend fun updateUserProfileU(
        @Body request: Map<String, String>
    ): Response<UserDetailResponse>

    @POST("auth/user/u")
    suspend fun updateUserProfileUPost(
        @Body request: Map<String, String>
    ): Response<UserDetailResponse>

    @PUT("auth/update")
    suspend fun updateProfileUpdate(
        @Body request: Map<String, String>
    ): Response<UserDetailResponse>

    @POST("auth/update")
    suspend fun updateProfileUpdatePost(
        @Body request: Map<String, String>
    ): Response<UserDetailResponse>

    @PUT("users/u")
    suspend fun updateUsersU(
        @Body request: Map<String, String>
    ): Response<UserDetailResponse>

    @PUT("users/update")
    suspend fun updateUsersUpdate(
        @Body request: Map<String, String>
    ): Response<UserDetailResponse>

    @GET("auth/users")
    suspend fun getAllUsers(): Response<UserListResponse>

    @POST("auth/user")
    suspend fun getUserById(
        @Body request: UserIdRequest
    ): Response<UserDetailResponse>

    @HTTP(method = "DELETE", path = "auth/user/d", hasBody = true)
    suspend fun deleteUser(
        @Body request: UserIdRequest
    ): Response<BaseApiResponse>


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

    @GET("orders/seller/g")
    suspend fun getAllOrders(): Response<OrderListResponse>

    @PUT("orders/cancel")
    suspend fun cancelOrder(
        @Body request: CancelOrderRequest
    ): Response<CancelOrderResponse>

    @PUT("orders/u")
    suspend fun updateOrderStatus(
        @Body request: UpdateOrderStatusRequest
    ): Response<OrderCreateResponse>

    @PUT("orders/status")
    suspend fun updateOrderStatusStatus(
        @Body request: UpdateOrderStatusRequest
    ): Response<OrderCreateResponse>

    @PUT("orders/update")
    suspend fun updateOrderStatusUpdate(
        @Body request: UpdateOrderStatusRequest
    ): Response<OrderCreateResponse>


    // ==========================================
    // 5. PRODUCT ROUTES (productRoutes.js)
    // ==========================================

    @POST("products/c")
    suspend fun createProducts(
        @Body products: List<ProductModel>
    ): Response<ProductCreateResponse>

    @GET("products/g")
    suspend fun getProductById(
        @Query("id") id: String?
    ): Response<ProductSingleResponse>

    @GET("products/g")
    suspend fun getAllProducts(
        @Query("page") page: Int = 1
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
