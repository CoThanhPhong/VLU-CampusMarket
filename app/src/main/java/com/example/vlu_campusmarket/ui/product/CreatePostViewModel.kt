package com.example.vlu_campusmarket.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vlu_campusmarket.data.local.dao.ProductDao
import com.example.vlu_campusmarket.data.local.dao.UserDao
import com.example.vlu_campusmarket.data.local.entity.ProductEntity
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.data.remote.ApiService
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class CreatePostViewModel(
    private val apiService: ApiService,
    private val productDao: ProductDao,
    private val userDao: UserDao
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private fun mapDeviceTypeToEnum(raw: String): String {
        return when (raw.trim().lowercase()) {
            "điện thoại", "mobile", "phone" -> "mobile"
            "tablet" -> "tablet"
            "mac", "macbook", "mac book" -> "mac"
            "laptop" -> "laptop"
            else -> "mobile"
        }
    }

    fun createProduct(
        name: String,
        price: Double,
        deviceType: String,
        local: String,
        amount: Int,
        description: String,
        img: Any,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val formattedDeviceType = mapDeviceTypeToEnum(deviceType)
                val imageUrls = when (img) {
                    is List<*> -> img.filterIsInstance<String>()
                    is String -> listOf(img)
                    else -> emptyList()
                }

                val currentUser = userDao.getLoggedInUser().firstOrNull()
                val validAuthorId = currentUser?.id?.takeIf { it.isNotBlank() }

                val newProduct = ProductModel(
                    _id = null,
                    name = name,
                    price = price,
                    deviceType = formattedDeviceType,
                    local = local,
                    amount = amount,
                    description = description,
                    img = imageUrls,
                    status = "Chờ duyệt",
                    author = validAuthorId ?: ""
                )

                val response = apiService.createProducts(listOf(newProduct))
                if (response.isSuccessful && response.body()?.success == true) {
                    val serverCreated = response.body()?.data?.firstOrNull()
                    val productId = serverCreated?.safeId?.ifEmpty { "prod_${System.currentTimeMillis()}" } ?: "prod_${System.currentTimeMillis()}"

                    val entity = ProductEntity(
                        _id = productId,
                        name = newProduct.name,
                        description = newProduct.description,
                        price = newProduct.price,
                        amount = newProduct.amount,
                        local = newProduct.local,
                        img = if (imageUrls.isNotEmpty()) Gson().toJson(imageUrls) else newProduct.imageUrl,
                        status = newProduct.status,
                        deviceType = newProduct.deviceType,
                        author = serverCreated?.authorId ?: validAuthorId ?: ""
                    )
                    productDao.insertProducts(listOf(entity))

                    onResult(true, "Đăng bài thành công! Đang chờ Admin kiểm duyệt.")
                } else {
                    onResult(false, "Đăng bài thất bại: ${response.errorBody()?.string() ?: "Lỗi server"}")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Lỗi kết nối mạng")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProduct(
        productId: String,
        name: String,
        price: Double,
        deviceType: String,
        local: String,
        amount: Int,
        description: String,
        img: Any,
        existingStatus: String?,
        existingAuthor: Any?,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val formattedDeviceType = mapDeviceTypeToEnum(deviceType)
                val imageUrls = when (img) {
                    is List<*> -> img.filterIsInstance<String>()
                    is String -> listOf(img)
                    else -> emptyList()
                }

                val currentUser = userDao.getLoggedInUser().firstOrNull()
                val validAuthorId = existingAuthor ?: currentUser?.id?.takeIf { it.isNotBlank() } ?: ""

                val updatedProduct = ProductModel(
                    _id = productId,
                    name = name,
                    price = price,
                    deviceType = formattedDeviceType,
                    local = local,
                    amount = amount,
                    description = description,
                    img = imageUrls,
                    status = existingStatus ?: "Chờ duyệt",
                    author = validAuthorId
                )

                val response = apiService.updateProduct(updatedProduct)
                if (response.isSuccessful) {
                    val entity = ProductEntity(
                        _id = productId,
                        name = updatedProduct.name,
                        description = updatedProduct.description,
                        price = updatedProduct.price,
                        amount = updatedProduct.amount,
                        local = updatedProduct.local,
                        img = if (imageUrls.isNotEmpty()) Gson().toJson(imageUrls) else updatedProduct.imageUrl,
                        status = updatedProduct.status,
                        deviceType = updatedProduct.deviceType,
                        author = validAuthorId.toString()
                    )
                    productDao.insertProducts(listOf(entity))

                    onResult(true, "Cập nhật bài viết thành công!")
                } else {
                    onResult(false, "Cập nhật thất bại: ${response.errorBody()?.string() ?: "Lỗi server"}")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Lỗi kết nối mạng")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
